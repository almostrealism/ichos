/*
 * Copyright 2020 Michael Murray
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.almostrealism.audio.computations;

import io.almostrealism.code.HybridScope;
import io.almostrealism.code.Scope;
import org.almostrealism.algebra.Pair;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.hardware.DynamicOperationComputationAdapter;
import io.almostrealism.relation.Producer;

import java.util.function.Consumer;

public class ClipCounter extends DynamicOperationComputationAdapter {
	public ClipCounter(Producer<Scalar> clipCount, Producer<Pair> clipSettings, Producer<Scalar> value) {
		super(new Producer[] { clipCount, clipSettings, value });
	}

	@Override
	public Scope<Void> getScope() {
		HybridScope<Void> scope = new HybridScope<>(this);

		String value = getArgument(2).valueAt(0).getExpression();
		String min = getArgument(1).valueAt(0).getExpression();
		String max = getArgument(1).valueAt(1).getExpression();
		String count = getArgument(0).valueAt(0).getExpression();

		Consumer<String> code = scope.code();
		code.accept("if (" + value + " >= " + max + " || " + value
				+ " <= " + min + ") " + count + " = " + count + " + 1;\n");
		return scope;
	}
}
