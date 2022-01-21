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
import org.almostrealism.algebra.Pair;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.graph.Adjustable;
import org.almostrealism.graph.Adjustment;
import org.almostrealism.graph.Cell;
import org.almostrealism.graph.Receptor;

import io.almostrealism.relation.Producer;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.CodeFeatures;

import java.util.function.Supplier;

public class CellAdjustment<T extends Cell<Scalar>> implements Adjustment<Scalar>, Receptor<Scalar>, CodeFeatures {
	private final T generator;
	private final Supplier<Evaluable<? extends Pair>> bounds;
	private final Scalar factor = new Scalar(1.0);

	public CellAdjustment(T generator, Supplier<Evaluable<? extends Pair>> bounds) {
		this.generator = generator;
		this.bounds = bounds;
		// System.out.println("CellAdjustment.bounds = " + bounds.get().evaluate());
	}

	public T getGenerator() { return generator; }

	public Supplier<Evaluable<? extends Pair>> getBounds() { return bounds; }

	/**
	 * Delegates to the {@link Cell#setup()} method of {@link #getGenerator()}.
	 */
	@Override
	public Supplier<Runnable> setup() {
		return generator.setup();
	}

	@Override
	public Supplier<Runnable> adjust(Adjustable<Scalar> toAdjust) {
		OperationList adjust = new OperationList("CellAdjustment Perform Adjustment");
		adjust.add(generator.push(null));
		adjust.add(toAdjust.updateAdjustment(r(bounds).subtract(l(bounds)).multiply(p(factor)).add(l(bounds))));
		return adjust;
	}

	// TODO  This is never called
	@Override
	public Supplier<Runnable> push(Producer<Scalar> protein) {
		return a(1, p(factor), v(1).add(protein).divide(2.0));
	}
}
