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
import org.almostrealism.audio.data.PolymorphicAudioData;
import org.almostrealism.audio.feature.Resampler;
import org.almostrealism.audio.filter.AudioCellAdapter;
import org.almostrealism.hardware.HardwareFeatures;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.util.CodeFeatures;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

public class WavCell extends AudioCellAdapter implements CodeFeatures, HardwareFeatures {
	private final WavCellData data;
	private final ScalarBank wave;

	private final Producer<Scalar> offset, duration;
	private final boolean repeat;

	private double amplitude;
	private double waveLength;

	public WavCell(ScalarBank wav, int sampleRate, double amplitude, Producer<Scalar> offset, Producer<Scalar> repeat) {
		this(new PolymorphicAudioData(), wav, sampleRate, amplitude, offset, repeat);
	}

	public WavCell(WavCellData data, ScalarBank wav, int sampleRate, double amplitude, Producer<Scalar> offset, Producer<Scalar> repeat) {
		this.data = data;
		this.amplitude = amplitude;

		if (sampleRate != OutputLine.sampleRate) {
			wave = Resampler.resampleWaveform(new Scalar(sampleRate), wav, new Scalar(OutputLine.sampleRate));
		} else {
			wave = wav;
		}

		setFreq(OutputLine.sampleRate);

		if (offset != null) {
			this.offset = scalarsMultiply(offset, v(OutputLine.sampleRate));
		} else {
			this.offset = null;
		}

		if (repeat != null) {
			this.repeat = true;
			this.duration = scalarsMultiply(repeat, v(OutputLine.sampleRate));
		} else {
			this.repeat = false;
			this.duration = null;
		}
	}

	public void setFreq(double hertz) { waveLength = hertz / (double) OutputLine.sampleRate; }

	public void setAmplitude(double amp) { amplitude = amp; }

	@Override
	public Supplier<Runnable> setup() {
		OperationList setup = new OperationList();
		if (offset == null) {
			setup.add(a(2, data::getWavePosition, v(0.0)));
		} else {
			setup.add(a(2, data::getWavePosition, scalarsMultiply(v(-1.0), offset)));
		}

		setup.add(() -> () -> {
			data.setWaveLength(waveLength);
			data.setWaveCount(wave.getCount());
			data.setAmplitude(amplitude);
		});

		return setup;
	}

	@Override
	public Supplier<Runnable> push(Producer<Scalar> protein) {
		Scalar value = new Scalar();
		OperationList push = new OperationList();
		if (duration != null) push.add(a(2, data::getDuration, v(bpm(128).l(1) * OutputLine.sampleRate)));
		push.add(new WavCellPush(data, wave, value, repeat));
		push.add(super.push(p(value)));
		return push;
	}

	@Override
	public Supplier<Runnable> tick() {
		OperationList tick = new OperationList();
		tick.add(new WavCellTick(data, repeat));
		tick.add(super.tick());
		return tick;
	}

	public static Function<WavCellData, WavCell> load(File f, double amplitude, Producer<Scalar> offset, Producer<Scalar> repeat) throws IOException {
		WavFile w = WavFile.openWavFile(f);

		double[][] wave = new double[w.getNumChannels()][(int) w.getFramesRemaining()];
		w.readFrames(wave, 0, (int) w.getFramesRemaining());

		int channelCount = w.getNumChannels();

		assert channelCount > 0;
		int channel = 0;

		ScalarBank waveform = WavFile.channel(wave, channel);
		return data -> new WavCell(data, waveform, (int) w.getSampleRate(), amplitude, offset, repeat);
	}
}

