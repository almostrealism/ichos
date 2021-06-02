/*
 * Copyright 2021 Michael Murray
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

package org.almostrealism.audio.sources;

import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.WavFile;
import org.almostrealism.audio.feature.Resampler;
import org.almostrealism.audio.filter.AudioCellAdapter;
import org.almostrealism.hardware.HardwareFeatures;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.util.CodeFeatures;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public class WavCell extends AudioCellAdapter implements CodeFeatures, HardwareFeatures {
	private final WavCellData data;
	private final ScalarBank wave;
	private final boolean repeat;

	public WavCell(ScalarBank wav, int sampleRate, double amplitude, double repeat) {
		data = new WavCellData(wav.getCount(), amplitude);

		if (sampleRate != OutputLine.sampleRate) {
			double ratio = OutputLine.sampleRate;
			ratio = ratio / sampleRate;
			wave = new ScalarBank((int) (wav.getCount() * ratio));

			Resampler.resampleWaveform(new Scalar(sampleRate), wav, new Scalar(OutputLine.sampleRate), wave);
		} else {
			wave = wav;
		}

		// TODO  This should not be required, there is some problem that is causing the output to be half speed
		setFreq(2 * OutputLine.sampleRate);

		if (repeat > 0) {
			this.repeat = true;
			data.setDuration(repeat * OutputLine.sampleRate);
		} else {
			this.repeat = false;
		}
	}

	public void setFreq(double hertz) { data.setWaveLength(hertz / (double) OutputLine.sampleRate); }

	public void setAmplitude(double amp) { data.setAmplitude(amp); }

	@Override
	public Supplier<Runnable> push(Producer<Scalar> protein) {
		Scalar value = new Scalar();
		OperationList push = new OperationList();
		push.add(new WavCellComputation(data, wave, value, repeat));
		push.add(super.push(p(value)));
		return push;
	}

	public static WavCell load(File f, double amplitude, double repeat) throws IOException {
		WavFile w = WavFile.openWavFile(f);

		double[][] wave = new double[w.getNumChannels()][(int) w.getFramesRemaining()];
		w.readFrames(wave, 0, (int) w.getFramesRemaining());

		int channelCount = w.getNumChannels();

		assert channelCount > 0;
		int channel = 0;

		ScalarBank waveform = WavFile.channel(wave, channel);
		return new WavCell(waveform, (int) w.getSampleRate(), amplitude, repeat);
	}
}

