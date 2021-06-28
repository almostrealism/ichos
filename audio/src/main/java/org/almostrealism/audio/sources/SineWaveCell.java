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

	public SineWaveCell() {
		data = new PolymorphicAudioData();
		data.setDepth(AudioCellAdapter.depth);
	}

	public void setEnvelope(Envelope e) { this.env = e; }

	public void strike() { data.setNotePosition(0); }
	
	public void setFreq(double hertz) { data.setWaveLength(hertz / (double) OutputLine.sampleRate); }

	public void setNoteLength(int msec) { data.setNoteLength(toFrames(msec)); }
	
	public void setPhase(double phase) { data.setPhase(phase); }
	
	public void setAmplitude(double amp) { data.setAmplitude(amp); }

	public Supplier<Runnable> setAmplitude(Producer<Scalar> amp) {
		return a(2, data::getAmplitude, amp);
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
