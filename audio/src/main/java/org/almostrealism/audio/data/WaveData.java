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

package org.almostrealism.audio.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.ScalarBankHeap;
import org.almostrealism.audio.WavFile;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.PackedCollectionHeap;
import org.almostrealism.hardware.KernelizedEvaluable;
import org.almostrealism.hardware.ctx.ContextSpecific;
import org.almostrealism.hardware.ctx.DefaultContextSpecific;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WaveData {
	private static ContextSpecific<ScalarBankHeap> heap;
	private static ContextSpecific<PackedCollectionHeap> collectionHeap;

	// TODO
	private static ContextSpecific<KernelizedEvaluable<PackedCollection>> collect;

	static {
	}

	private ScalarBank wave;

	private PackedCollection collection;
	private int sampleRate;

	public WaveData(PackedCollection wave, int sampleRate) {
		this.collection = wave;
		this.sampleRate = sampleRate;
		this.wave = allocate(wave.getMemLength());
	}

	@Deprecated
	public WaveData(ScalarBank wave, int sampleRate) {
		this.wave = wave;
		this.sampleRate = sampleRate;
	}

	@JsonIgnore
	@Deprecated
	public ScalarBank getWave() {
		if (collection != null) {
			long start = System.currentTimeMillis();

			double data[] = collection.toArray(0, collection.getMemLength());
			for (int i = 0; i < this.wave.getCount(); i++) {
				this.wave.set(i, data[i], 1.0);
			}

			System.out.println("WaveData: Imported collection in " + (System.currentTimeMillis() - start) + "ms");
		}

		return wave;
	}

	@Deprecated
	public void setWave(ScalarBank wave) {
		this.wave = wave;
	}

	public PackedCollection getCollection() {
		if (collection == null) {
			long start = System.currentTimeMillis();

			collection = allocateCollection(wave.getCount());
			double data[] = wave.toArray(0, wave.getMemLength());
			for (int i = 0; i < collection.getMemLength(); i++) {
				collection.setMem(i, data[2 * i]);
			}

			System.out.println("WaveData: Exported collection in " + (System.currentTimeMillis() - start) + "ms");
		}

		return collection;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public void save(File file) {
		ScalarBank w = getWave();

		int frames = w.getCount();

		WavFile wav;

		try {
			wav = WavFile.newWavFile(file, 2, frames, 24, sampleRate);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		for (int i = 0; i < frames; i++) {
			double value = w.get(i).getValue();

			try {
				wav.writeFrames(new double[][]{{value}, {value}}, 1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			wav.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static WaveData load(File f) throws IOException {
		WavFile w = WavFile.openWavFile(f);

		double[][] wave = new double[w.getNumChannels()][(int) w.getFramesRemaining()];
		w.readFrames(wave, 0, (int) w.getFramesRemaining());

		int channelCount = w.getNumChannels();

		assert channelCount > 0;
		int channel = 0;

		return new WaveData(WavFile.channel(wave, channel), (int) w.getSampleRate());
	}

	@Deprecated
	public static ScalarBankHeap getHeap() { return heap == null ? null : heap.getValue(); }

	@Deprecated
	public static void setHeap(Supplier<ScalarBankHeap> create, Consumer<ScalarBankHeap> destroy) {
		heap = new DefaultContextSpecific<>(create, destroy);
		heap.init();
	}

	public static PackedCollectionHeap getCollectionHeap() { return collectionHeap == null ? null : collectionHeap.getValue(); }

	public static void setCollectionHeap(Supplier<PackedCollectionHeap> create, Consumer<PackedCollectionHeap> destroy) {
		collectionHeap = new DefaultContextSpecific<>(create, destroy);
		collectionHeap.init();
	}

	public static void dropHeap() {
		heap = null;
		collectionHeap = null;
	}

	@Deprecated
	public static ScalarBank allocate(int count) {
		return Optional.ofNullable(getHeap()).map(h -> h.allocate(count)).orElse(new ScalarBank(count));
	}

	public static PackedCollection allocateCollection(int count) {
		return Optional.ofNullable(getCollectionHeap()).map(h -> h.allocate(count)).orElse(new PackedCollection(count));
	}
}
