/*
 * Copyright 2016 Michael Murray
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

package com.almostrealism.audio.health;

import java.io.File;
import java.util.function.Supplier;

import com.almostrealism.audio.SineWaveCell;
import io.almostrealism.relation.Evaluable;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.AudioMeter;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.computations.DefaultEnvelopeComputation;
import org.almostrealism.graph.Receptor;
import org.almostrealism.hardware.DynamicProducerForMemWrapper;
import org.almostrealism.optimize.HealthComputation;

public abstract class HealthComputationAdapter implements HealthComputation<Scalar> {
	public static int standardDuration = (int) (10 * OutputLine.sampleRate);

//	public static double frequency = 391.95; // G
	public static double frequency = 196.00; // G
	
	private SineWaveCell generator;
	
	private String debugFile;
	
	protected void init() {
		generator = new SineWaveCell();
		generator.setFreq(frequency);
		generator.setNoteLength(400);
		generator.setAmplitude(0.1);
		generator.setEnvelope(DefaultEnvelopeComputation::new);
	}
	
	protected AudioMeter getMeter() {
		AudioMeter meter = new AudioMeter();
		
		if (debugFile != null) {
			WaveOutput out = new WaveOutput(new File(debugFile), standardDuration, 24);
			meter.setForwarding(out);
		}
		
		return meter;
	}
	
	protected Supplier<Runnable> push() { return generator.push(null); }
	
	protected void setReceptor(Receptor<Scalar> r) { generator.setReceptor(r); }
	
	public void setDebugOutputFile(String file) { this.debugFile = file; }
}
