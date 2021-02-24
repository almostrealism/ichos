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

package com.almostrealism.audio.optimize;

import com.almostrealism.audio.filter.PeriodicAdjustment;
import org.almostrealism.algebra.Pair;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.graph.Adjustment;
import org.almostrealism.heredity.Gene;
import org.almostrealism.organs.CellAdjustmentFactory;
import org.almostrealism.util.CodeFeatures;

public class DefaultCellAdjustmentFactory implements CellAdjustmentFactory<Scalar, Scalar>, CodeFeatures {
	@Override
	public Adjustment<Scalar> generateAdjustment(Gene<Scalar> gene) {
		double min = Math.min(1.0, gene.getFactor(1).getResultant(v(1.0)).get().evaluate().getValue());
		double max = min + gene.getFactor(2).getResultant(v(1.0 - min)).get().evaluate().getValue();
		return new PeriodicAdjustment(gene.getFactor(0).getResultant(v(1.0)).get().evaluate().getValue(), new Pair(min, max));
	}
}
