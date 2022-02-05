/*
 * Copyright 2022 Michael Murray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.almostrealism.audio;

import io.almostrealism.relation.Producer;
import org.almostrealism.CodeFeatures;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.computations.ScalarChoice;
import org.almostrealism.graph.temporal.WaveCell;
import org.almostrealism.time.Frequency;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Waves extends ArrayList<Waves> implements CodeFeatures {
	private ScalarBank leaf;
	private int pos, len;

	public Waves() { }

	public Waves(ScalarBank leaf) { this(leaf, 0, leaf.getCount()); }

	public Waves(ScalarBank leaf, int pos, int len) {
		this.leaf = (ScalarBank) leaf.getRootDelegate();
		this.pos = leaf.getOffset() + pos;
		this.len = len;
	}

	public WaveCell getChoiceCell(Producer<Scalar> decision, Producer<Scalar> offset, Producer<Scalar> duration) {
		Map<ScalarBank, List<Segment>> segmentsByBank = getSegments().stream().collect(Collectors.groupingBy(Segment::getSource));
		if (segmentsByBank.size() > 1) {
			throw new UnsupportedOperationException("More than one root ScalarBank for Waves instance");
		}

		ScalarBank source = segmentsByBank.keySet().iterator().next();
		List<Segment> segments = segmentsByBank.values().iterator().next();

		int count = segments.size();
		ScalarBank positions = new ScalarBank(count);
		ScalarBank lengths = new ScalarBank(count);

		IntStream.range(0, count).forEach(i -> positions.set(i, segments.get(i).getPosition()));
		IntStream.range(0, count).forEach(i -> lengths.set(i, segments.get(i).getLength()));

		ScalarChoice positionChoice = new ScalarChoice(count, decision, v(positions));
		ScalarChoice lengthChoice = new ScalarChoice(count, decision, v(lengths));

		return new WaveCell(source, OutputLine.sampleRate, 1.0, offset, duration, positionChoice, lengthChoice);
	}

	public Segment getSegment() {
		if (!isLeaf()) throw new UnsupportedOperationException();
		return new Segment(leaf, pos, len);
	}

	public List<Segment> getSegments() {
		if (isLeaf()) return Collections.singletonList(getSegment());
		return stream().map(Waves::getSegments).flatMap(List::stream).collect(Collectors.toList());
	}

	public boolean isLeaf() { return leaf != null; }

	public void addSplits(Collection<File> files, double bpm, Double... splits) {
		addSplits(files, bpm(bpm), splits);
	}

	public void addSplits(Collection<File> files, Frequency bpm, Double... splits) {
		if (isLeaf()) throw new UnsupportedOperationException();

		List<Double> sizes = List.of(splits);

		files.stream().map(file -> {
					try {
						return Waves.load(file, w -> w.getSampleRate() == OutputLine.sampleRate);
					} catch (UnsupportedOperationException | IOException e) {
						return null;
					}
				}).filter(Objects::nonNull).flatMap(wav ->
						sizes.stream()
								.map(beats -> bpm.l(beats) * OutputLine.sampleRate)
								.mapToDouble(duration -> duration)
								.mapToInt(frames -> (int) frames)
								.mapToObj(wav::split))
				.forEach(this::add);
	}

	public Waves split(int frames) {
		if (!isLeaf()) throw new UnsupportedOperationException();

		return IntStream.range(0, len / frames)
				.mapToObj(i -> new Waves(leaf, pos + i * frames, frames))
				.collect(Collectors.toCollection(Waves::new));
	}

	public static Waves load(File f) throws IOException {
		return load(f, v -> true);
	}

	public static Waves load(File f, Predicate<WavFile> validator) throws IOException {
		WavFile w = WavFile.openWavFile(f);
		if (w.getNumFrames() >= Integer.MAX_VALUE) throw new UnsupportedOperationException();
		if (!validator.test(w)) throw new IOException();

		double data[][] = new double[w.getNumChannels()][(int) w.getNumFrames()];
		w.readFrames(data, (int) w.getFramesRemaining());

		return new Waves(WavFile.channel(data, 0));
	}

	public static class Segment {
		private ScalarBank source;
		private int pos, len;

		public Segment(ScalarBank source, int pos, int len) {
			this.source = source;
			this.pos = pos;
			this.len = len;
		}

		public ScalarBank getSource() { return source; }
		public int getPosition() { return pos; }
		public int getLength() { return len; }
	}
}
