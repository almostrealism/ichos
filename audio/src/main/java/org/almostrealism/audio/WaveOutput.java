/*
 * Copyright 2020 Michael Murray
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

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.graph.Receptor;
import io.almostrealism.relation.Producer;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.time.AcceleratedTimeSeries;
import org.almostrealism.time.CursorPair;
import org.almostrealism.util.CodeFeatures;

public class WaveOutput implements Receptor<Scalar>, CodeFeatures {
	private File file;
	private int bits;
	private long sampleRate;

	private WavFile wav;
	private CursorPair cursor;
	private AcceleratedTimeSeries data;

	public WaveOutput(File f) {
		this(f, 24);
	}

	public WaveOutput(File f, int bits) {
		this(f, 120 * OutputLine.sampleRate, bits);
	}

	public WaveOutput(File f, int maxFrames, int bits) {
		this(f, maxFrames, bits, OutputLine.sampleRate);
	}

	public WaveOutput(File f, int maxFrames, int bits, long sampleRate) {
		this.file = f;
		this.bits = bits;
		this.sampleRate = sampleRate;
		this.cursor = new CursorPair();
		this.data = new AcceleratedTimeSeries(maxFrames);
	}

	@Override
	public Supplier<Runnable> push(Producer<Scalar> protein) {
		OperationList push = new OperationList();
		push.add(data.add(temporal(l(p(cursor)), protein)));
		push.add(cursor.increment(v(1)));
		return push;
	}

	public Supplier<Runnable> write() {
		// TODO  Write frames in larger batches than 1
		return () -> () -> {
			int frames = (int) cursor.left();

			try {
				this.wav = WavFile.newWavFile(file, 2, frames, bits, sampleRate);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			for (int i = 0; i < frames; i++) {
				double value = data.get(i).getValue();

				try {
					wav.writeFrames(new double[][]{{value}, {value}}, 1);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
	}
}
