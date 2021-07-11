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

import io.almostrealism.relation.Evaluable;
import org.almostrealism.audio.sources.SineWaveCell;
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

public class PeriodicAdjustment extends CellAdjustment<SineWaveCell> implements Adjustable<Scalar> {
	private final Supplier<Evaluable<? extends Scalar>> freq;

	public PeriodicAdjustment(Supplier<Evaluable<? extends Scalar>> freq, Supplier<Evaluable<? extends Pair>> bounds) {
		super(new SineWaveCell(), bounds);
		this.freq = freq;
		getGenerator().setPhase(0.5);
		getGenerator().setNoteLength(0);
		getGenerator().setAmplitude(1);
		getGenerator().setReceptor(this);
	}

	@Override
	public Supplier<Runnable> setup() {
		OperationList setup = new OperationList();
		setup.add(getGenerator().setFreq(freq));
		setup.add(super.setup());
		return setup;
	}

	@Override
	public Supplier<Runnable> updateAdjustment(Producer<Scalar> value) {
		return getGenerator().setAmplitude(value);
	}
}
