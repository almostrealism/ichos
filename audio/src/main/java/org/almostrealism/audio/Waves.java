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

import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.ScalarBankHeap;
import org.almostrealism.hardware.mem.MemoryBankAdapter;
import org.almostrealism.time.TemporalFeatures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Waves extends ArrayList<Waves> implements TemporalFeatures {
	private ScalarBank leaf;

	public Waves() { }

	public Waves(ScalarBank leaf) {
		this.leaf = leaf;
	}

	public ScalarBank getScalarBank() { return leaf; }

	public Collection<ScalarBank> getAllBanks() {
		if (isLeaf()) {
			return Collections.singletonList(leaf);
		} else {
			ArrayList<ScalarBank> all = new ArrayList<>();
			stream().map(Waves::getAllBanks).forEach(all::addAll);
			return all;
		}
	}

	public boolean isLeaf() { return leaf != null; }

	public void addSplits(Collection<File> files, double bpm, Double... splits) {
		if (isLeaf()) throw new UnsupportedOperationException();

		List<Double> sizes = List.of(splits);

		files.stream().map(file -> {
					try {
						return Waves.load(file, w -> w.getSampleRate() == OutputLine.sampleRate);
					} catch (UnsupportedOperationException | IOException e) {
						return null;
					}
				}).flatMap(wav ->
						sizes.stream()
								.map(beats -> bpm(bpm).l(beats) * OutputLine.sampleRate)
								.mapToDouble(duration -> duration)
								.mapToInt(frames -> (int) frames)
								.mapToObj(wav::split))
				.forEach(this::add);
	}

	public Waves split(int frames) {
		if (!isLeaf()) throw new UnsupportedOperationException();

		return IntStream.range(0, leaf.getCount() / frames)
				.mapToObj(i -> new ScalarBank(frames, leaf, i * frames, MemoryBankAdapter.CacheLevel.NONE))
				.map(Waves::new)
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
}
