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

package com.almostrealism.audio.filter;

import com.almostrealism.audio.SineWaveCell;
import org.almostrealism.algebra.Pair;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.graph.Adjustable;
import org.almostrealism.graph.Adjustment;
import org.almostrealism.graph.Receptor;

import io.almostrealism.relation.Producer;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.util.CodeFeatures;

import java.util.function.Supplier;

public class PeriodicAdjustment implements Adjustment<Scalar>, Receptor<Scalar>, CodeFeatures {
	private SineWaveCell generator;
	private Pair bounds;
	private Scalar factor = new Scalar(1.0);

	private long timer = 2 * OutputLine.sampleRate;
	
	public PeriodicAdjustment(double freq, Pair bounds) {
		this.bounds = bounds;
		setFrequency(freq);
	}
	
	public void setFrequency(double freq) {
		generator = new SineWaveCell();
		generator.setFreq(freq);
		generator.setPhase(0.5);
		generator.setNoteLength(0);
		generator.setAmplitude(1);
		generator.setReceptor(this);
	}

	@Override
	public Supplier<Runnable> adjust(Adjustable<Scalar> toAdjust) {
		OperationList adjust = new OperationList();
		adjust.add(generator.push(null));
		adjust.add(toAdjust.updateAdjustment(v(bounds).r().subtract(v(bounds).l())
						.multiply(p(factor)).add(v(bounds).l())));
		return adjust;
	}

	@Override
	public Supplier<Runnable> push(Producer<Scalar> protein) {
		return a(2, p(factor), v(1).add(protein).divide(2.0));
	}
}
