/*
 * Copyright 2020 Michael Murray
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

import org.almostrealism.algebra.Scalar;
import org.almostrealism.graph.Cell;
import org.almostrealism.graph.CellFactory;
import org.almostrealism.heredity.Gene;
import org.almostrealism.util.CodeFeatures;

public class DelayCellFactory implements CellFactory<Scalar, Scalar>, CodeFeatures {
	private int min;
	private Scalar delta;
	private int factorIndex;
	
	public DelayCellFactory(int minDelay, int maxDelay, int factorIndex) {
		this.min = minDelay;
		this.delta = new Scalar(maxDelay - minDelay);
		this.factorIndex = factorIndex;
	}

	@Override
	public Cell<Scalar> generateCell(Gene<Scalar> gene) {
		return new AdjustableDelayCell((int) (min + gene.getFactor(factorIndex).getResultant(p(delta)).get().evaluate().getValue()));
		// return new BasicDelayCell((int) (min + gene.getFactor(factorIndex).getResultant(new StaticProducer<>(Double.valueOf(delta))).evaluate()));
	}
}
