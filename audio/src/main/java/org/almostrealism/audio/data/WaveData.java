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
import org.almostrealism.audio.WavFile;

import java.io.File;
import java.io.IOException;

public class WaveData {
	private ScalarBank wave;
	private int sampleRate;

	public WaveData(ScalarBank wave, int sampleRate) {
		this.wave = wave;
		this.sampleRate = sampleRate;
	}

	@JsonIgnore
	public ScalarBank getWave() {
		return wave;
	}

	public void setWave(ScalarBank wave) {
		this.wave = wave;
	}

	public int getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(int sampleRate) {
		this.sampleRate = sampleRate;
	}

	public void save(File file) {
		int frames = getWave().getCount();

		WavFile wav;

		try {
			wav = WavFile.newWavFile(file, 2, frames, 24, sampleRate);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		for (int i = 0; i < frames; i++) {
			double value = getWave().get(i).getValue();

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
}
