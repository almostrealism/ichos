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

import io.almostrealism.relation.Evaluable;
import org.almostrealism.audio.Envelope;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.data.PolymorphicAudioData;
import org.almostrealism.audio.filter.AudioCellAdapter;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.hardware.HardwareFeatures;
import io.almostrealism.relation.Producer;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.util.CodeFeatures;

import java.util.function.Supplier;

public class SineWaveCell extends AudioCellAdapter implements CodeFeatures, HardwareFeatures {
	private Envelope env;
	private final SineWaveCellData data;

	private double noteLength;
	private double waveLength;
	private double phase;
	private double amplitude;

	public SineWaveCell() {
		this(new PolymorphicAudioData());
	}

	public SineWaveCell(SineWaveCellData data) {
		this.data = data;
	}

	public void setEnvelope(Envelope e) { this.env = e; }

	public void strike() { data.setNotePosition(0); }
	
	public void setFreq(double hertz) { this.waveLength = hertz / (double) OutputLine.sampleRate; }

	public Supplier<Runnable> setFreq(Supplier<Evaluable<? extends Scalar>> hertz) {
		return a(2, data::getWaveLength, scalarsDivide(hertz, v(OutputLine.sampleRate)));
	}

	public void setNoteLength(int msec) { this.noteLength = toFramesMilli(msec); }

	public Supplier<Runnable> setNoteLength(Supplier<Evaluable<? extends Scalar>> noteLength) {
		return a(2, data::getNoteLength, toFramesMilli(noteLength));
	}
	
	public void setPhase(double phase) { this.phase = phase; }
	
	public void setAmplitude(double amp) { amplitude = amp; }

	public Supplier<Runnable> setAmplitude(Producer<Scalar> amp) {
		return a(2, data::getAmplitude, amp);
	}

	@Override
	public Supplier<Runnable> setup() {
		Supplier<Runnable> defaults = () -> () -> {
			data.setDepth(AudioCellAdapter.depth);
			data.setNotePosition(0);
			data.setWavePosition(0);
			data.setNoteLength(noteLength);
			data.setWaveLength(waveLength);
			data.setPhase(phase);
			data.setAmplitude(amplitude);
		};
		Supplier<Runnable> customization = super.setup();

		OperationList setup = new OperationList();
		setup.add(defaults);
		setup.add(customization);
		return setup;
	}

	@Override
	public Supplier<Runnable> push(Producer<Scalar> protein) {
		Scalar value = new Scalar();
		OperationList push = new OperationList();
		push.add(new SineWavePush(data, env == null ? v(1.0) :
					env.getScale(data::getNotePosition), value));
		push.add(super.push(p(value)));
		return push;
	}

	@Override
	public Supplier<Runnable> tick() {
		OperationList tick = new OperationList();
		tick.add(new SineWaveTick(data, env == null ? v(1.0) :
				env.getScale(data::getNotePosition)));
		tick.add(super.tick());
		return tick;
	}
}
