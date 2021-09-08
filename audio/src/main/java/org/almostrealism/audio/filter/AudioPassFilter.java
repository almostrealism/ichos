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

package org.almostrealism.audio.filter;

import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.data.AudioFilterData;
import org.almostrealism.audio.data.PolymorphicAudioData;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.heredity.TemporalFactor;
import org.almostrealism.util.CodeFeatures;

import java.util.function.Supplier;

public class AudioPassFilter implements TemporalFactor<Scalar>, CodeFeatures {
	private AudioFilterData data;
	private Producer<Scalar> input;

	private boolean high;

	public AudioPassFilter(boolean high) {
		this(new PolymorphicAudioData(), high);
	}
	public AudioPassFilter(AudioFilterData data, boolean high) {
		this.data = data;
		this.high = high;
	}

	public double getFrequency() {
		return data.frequency().getValue();
	}

	public void setFrequency(double frequency) {
		data.setFrequency(frequency);
	}

	public double getResonance() {
		return data.resonance().getValue();
	}

	public void setResonance(double resonance) {
		data.setResonance(resonance);
	}

	public int getSampleRate() {
		return (int) data.sampleRate().getValue();
	}

	public void setSampleRate(int sampleRate) {
		data.setSampleRate(sampleRate);
	}

	public boolean isHigh() {
		return high;
	}

	@Override
	public Producer<Scalar> getResultant(Producer<Scalar> value) {
		if (input != null) {
			throw new IllegalArgumentException("AudioPassFilter cannot be reused");
		}

		input = value;
		return data::getOutput;
	}

	@Override
	public Supplier<Runnable> tick() {
		return new AudioPassFilterComputation(data, input, high);
	}
}