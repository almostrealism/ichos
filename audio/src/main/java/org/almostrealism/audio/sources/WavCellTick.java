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

import io.almostrealism.code.HybridScope;
import io.almostrealism.code.ScopeInputManager;
import io.almostrealism.code.expressions.Sum;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;

import java.util.function.Consumer;

public class WavCellTick extends WavCellComputation {
	public WavCellTick(WavCellData data, boolean repeat) {
		super(data, new ScalarBank(1), new Scalar(), repeat);
	}

	@Override
	public void prepareScope(ScopeInputManager manager) {
		super.prepareScope(manager);

		scope = new HybridScope(this);

		Consumer<String> exp = scope.code();

		exp.accept(getWavePosition().valueAt(0).getExpression());
		exp.accept(" = ");
		exp.accept(new Sum(getWavePosition().valueAt(0), getWaveLength().valueAt(0)).getExpression());
		exp.accept(";\n");

		if (repeat) {
			exp.accept("if (" + getDuration().valueAt(0).getExpression() + " > " + stringForDouble(0.0) + ") {\n");
			exp.accept("\t");
			exp.accept(getWavePosition().valueAt(0).getExpression());
			exp.accept(" = fmod(");
			exp.accept(getWavePosition().valueAt(0).getExpression());
			exp.accept(", ");
			exp.accept(getDuration().valueAt(0).getExpression());
			exp.accept(");\n");
			exp.accept("}");
		}
	}
}
