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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.almostrealism.cycle.Setup;
import io.almostrealism.relation.Producer;
import org.almostrealism.CodeFeatures;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.computations.ScalarChoice;
import org.almostrealism.audio.data.FileWaveDataProvider;
import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.audio.data.ParameterizedWaveDataProviderFactory;
import org.almostrealism.audio.data.StaticWaveDataProviderFactory;
import org.almostrealism.audio.data.WaveDataProvider;
import org.almostrealism.audio.filter.EnvelopeProvider;
import org.almostrealism.graph.temporal.WaveCell;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.time.Frequency;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Waves implements CodeFeatures {
	private List<Waves> children;
	private ParameterizedWaveDataProviderFactory source;
	private EnvelopeProvider envelope;
	private int pos = -1, len = -1;
	private String sourceName;

	public Waves() { this(null); }

	public Waves(String sourceName) { this(sourceName, null, -1, -1); }

	public Waves(String sourceName, ParameterizedWaveDataProviderFactory source) {
		this(sourceName, source, 0, source.getCount());
	}

	public Waves(String sourceName, ParameterizedWaveDataProviderFactory source, int pos, int len) {
		this.sourceName = sourceName;
		this.pos = pos;
		this.len = len;
		this.source = source;
		this.children = new ArrayList<>();
	}

	public List<Waves> getChildren() { return children; }
	public void setChildren(List<Waves> children) { this.children = children; }

	public int getPos() { return pos; }
	public void setPos(int pos) { this.pos = pos; }

	public int getLen() { return len; }
	public void setLen(int len) { this.len = len; }

	public String getSourceName() { return sourceName; }
	public void setSourceName(String sourceName) { this.sourceName = sourceName; }

	public ParameterizedWaveDataProviderFactory getSource() { return source; }
	public void setSource(ParameterizedWaveDataProviderFactory source) { this.source = source; }

	public EnvelopeProvider getEnvelope() { return envelope; }
	public void setEnvelope(EnvelopeProvider envelope) { this.envelope = envelope; }

	public WaveCell getChoiceCell(Producer<Scalar> decision, Producer<Scalar> x, Producer<Scalar> y, Producer<Scalar> z,
								  Producer<Scalar> offset, Producer<Scalar> duration) {
		Map<ScalarBank, List<Segment>> segmentsByBank = getSegments(x, y, z).stream().collect(Collectors.groupingBy(Segment::getSource));
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

		WaveCell cell = new WaveCell(source, OutputLine.sampleRate, 1.0, offset, duration, positionChoice, lengthChoice);
		cell.addSetup(segments.stream().map(Segment::setup).collect(OperationList.collector()));
		return cell;
	}

	@JsonIgnore
	public Segment getSegment(Producer<Scalar> x, Producer<Scalar> y, Producer<Scalar> z) {
		if (!isLeaf()) throw new UnsupportedOperationException();
		WaveDataProvider provider = source.create(x, y, z);
		return new Segment(sourceName, provider.get().getWave(), pos, len, provider.setup());
	}

	@JsonIgnore
	public List<Segment> getSegments(Producer<Scalar> x, Producer<Scalar> y, Producer<Scalar> z) {
		if (isLeaf()) return Collections.singletonList(getSegment(x, y, z));
		return getChildren().stream().map(c -> c.getSegments(x, y, z)).flatMap(List::stream).collect(Collectors.toList());
	}

	public Segment getSegmentChoice(double decision, double x, double y, double z) {
		List<Segment> segments = getSegments(v(x), v(y), v(z));
		if (segments.isEmpty()) return null;
		return segments.get((int) (decision * segments.size()));
	}

	@JsonIgnore
	public boolean isLeaf() { return source != null || (pos > -1 && len > -1); }

	public void addFiles(File... files) { addFiles(List.of(files)); }

	public void addFiles(Collection<File> files) {
		if (isLeaf()) throw new UnsupportedOperationException();

		files.stream().map(file -> {
					try {
						return Waves.loadAudio(file, w -> w.getSampleRate() == OutputLine.sampleRate);
					} catch (UnsupportedOperationException | IOException e) {
						return null;
					}
				}).filter(Objects::nonNull).map(wav -> {
					Waves waves = new Waves(wav.getSourceName());
					waves.getChildren().add(wav);
					return waves;
				}).forEach(this.getChildren()::add);
	}

	public void addSplits(Collection<File> files, double bpm, double silenceThreshold, Double... splits) {
		addSplits(files, bpm(bpm), silenceThreshold, splits);
	}

	public void addSplits(Collection<File> files, Frequency bpm, double silenceThreshold, Double... splits) {
		if (isLeaf()) throw new UnsupportedOperationException();

		List<Double> sizes = List.of(splits);

		files.stream().map(file -> {
					try {
						return Waves.loadAudio(file, w -> w.getSampleRate() == OutputLine.sampleRate);
					} catch (UnsupportedOperationException | IOException e) {
						return null;
					}
				}).filter(Objects::nonNull).flatMap(wav ->
						sizes.stream()
								.map(beats -> bpm.l(beats) * OutputLine.sampleRate)
								.mapToDouble(duration -> duration)
								.mapToInt(frames -> (int) frames)
								.mapToObj(f -> wav.split(f, silenceThreshold)))
				.forEach(this.getChildren()::add);
	}

	public Waves split(int frames, double silenceThreshold) {
		if (!isLeaf()) throw new UnsupportedOperationException();

		Waves waves = new Waves(sourceName);
		IntStream.range(0, len / frames)
				.mapToObj(i -> new Waves(sourceName, source, pos + i * frames, frames))
				.filter(w -> w.getSegment(null, null, null).range().length().getValue() > (silenceThreshold * frames))
				.forEach(w -> waves.getChildren().add(w));
		return waves;
	}

	public String asJson() throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(this);
	}

	public void store(File f) throws IOException {
		try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
			out.write(asJson());
			out.flush();
		}
	}

	public static Waves load(File f) throws IOException {
		return new ObjectMapper().readValue(f.toURI().toURL(), Waves.class);
	}

	public static Waves loadAudio(File f) throws IOException {
		return loadAudio(f, v -> true);
	}

	public static Waves loadAudio(File f, Predicate<WavFile> validator) throws IOException {
		WavFile w = WavFile.openWavFile(f);
		if (w.getNumFrames() >= Integer.MAX_VALUE) throw new UnsupportedOperationException();
		if (!validator.test(w)) throw new IOException();

		double data[][] = new double[w.getNumChannels()][(int) w.getNumFrames()];
		w.readFrames(data, (int) w.getFramesRemaining());

		return new Waves(f.getCanonicalPath(), new StaticWaveDataProviderFactory(new FileWaveDataProvider(f.getCanonicalPath())));
	}

	public static class Segment implements Setup {
		private String sourceText;
		private ScalarBank source;
		private int pos, len;

		private Supplier<Runnable> setup;

		public Segment(String sourceText, ScalarBank source, int pos, int len, Supplier<Runnable> setup) {
			// TODO  This will only work if the atomic length of the root delegate is 2 (ScalarBank, ScalarTable, etc)
			// TODO  Other kinds of data structures will have problems
			this.sourceText = sourceText;
			this.source = (ScalarBank) source.getRootDelegate();
			this.pos = (source.getOffset() / 2) + pos;
			this.len = len;
			this.setup = setup;
		}

		public String getSourceText() { return sourceText; }
		public ScalarBank getSource() { return source; }
		public int getPosition() { return pos; }
		public int getLength() { return len; }
		public ScalarBank range() { return source.range(pos, len); }

		public Supplier<Runnable> setup() { return setup; }
	}
}
