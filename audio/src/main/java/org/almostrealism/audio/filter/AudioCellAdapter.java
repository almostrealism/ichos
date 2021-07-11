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

package org.almostrealism.audio.filter;

import io.almostrealism.relation.Evaluable;
import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.graph.Adjustable;
import org.almostrealism.graph.CellAdapter;
import org.almostrealism.graph.ScalarCachedStateCell;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.util.CodeFeatures;

import java.util.function.Supplier;

public abstract class AudioCellAdapter extends ScalarCachedStateCell implements Adjustable<Scalar>, CodeFeatures {
	public static final double PI = Math.PI;

	public static double depth = 1.0;

	private final OperationList setup;

	public AudioCellAdapter() {
		setup = new OperationList();
	}

	public void addSetup(Supplier<Runnable> setup) {
		this.setup.add(setup);
	}

	protected int toFrames(int msec) { return (int) (OutputLine.sampleRate * msec / 1000d); }

	protected Supplier<Evaluable<? extends Scalar>> toFrames(Supplier<Evaluable<? extends Scalar>> msec) {
		return scalarsMultiply(v(OutputLine.sampleRate / 1000d), msec);
	}

	@Override
	public Supplier<Runnable> updateAdjustment(Producer<Scalar> value) {
		return new OperationList();
	}

	@Override
	public Supplier<Runnable> setup() {
		OperationList setup = new OperationList();
		setup.add(super.setup());
		setup.add(this.setup);
		return setup;
	}
}
