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

package org.almostrealism.audio.sources;

import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.data.PolymorphicAudioData;
import org.almostrealism.audio.data.WaveData;
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

	// TODO  This should probably take MemoryBank<Scalar> to be more general
	public WavCell(ScalarBank wav, int sampleRate) {
		this(wav, sampleRate, 1.0);
	}

	public WavCell(ScalarBank wav, int sampleRate, double amplitude) {
		this(wav, sampleRate, amplitude, null, null);
	}

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
		OperationList setup = new OperationList("WavCell Setup");
		if (offset == null) {
			setup.add(a(1, data::getWavePosition, v(0.0)));
		} else {
			setup.add(a(1, data::getWavePosition, scalarsMultiply(v(-1.0), offset)));
		}

		setup.add(a(1, data::getWaveLength, v(waveLength)));
		setup.add(a(1, data::getWaveCount, v(wave.getCount())));
		setup.add(a(1, data::getAmplitude, v(amplitude)));
		setup.add(super.setup());
		return setup;
	}

	@Override
	public Supplier<Runnable> push(Producer<Scalar> protein) {
		Scalar value = new Scalar();
		OperationList push = new OperationList("WavCell Push");
		if (duration != null) push.add(a(1, data::getDuration, duration));
		push.add(new WavCellPush(data, wave, value, repeat));
		push.add(super.push(p(value)));
		return push;
	}

	@Override
	public Supplier<Runnable> tick() {
		OperationList tick = new OperationList("WavCell Tick");
		tick.add(new WavCellTick(data, repeat));
		tick.add(super.tick());
		return tick;
	}

	public static Function<WavCellData, WavCell> load(File f, double amplitude, Producer<Scalar> offset, Producer<Scalar> repeat) throws IOException {
		WaveData waveform = WaveData.load(f);
		return data -> new WavCell(data, waveform.getWave(), waveform.getSampleRate(), amplitude, offset, repeat);
	}

	public static Function<WavCellData, WavCell> load(WaveData w, double amplitude, Producer<Scalar> offset, Producer<Scalar> repeat) throws IOException {
		return data -> new WavCell(data, w.getWave(), w.getSampleRate(), amplitude, offset, repeat);
	}
}

