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
import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.computations.Choice;
import org.almostrealism.audio.data.ValueSequenceData;
import org.almostrealism.util.CodeFeatures;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ValueSequencePush extends ValueSequenceComputation implements CodeFeatures {
	private Choice choice;

	public ValueSequencePush(ValueSequenceData data, Producer<Scalar> durationFrames, Scalar output, Producer<Scalar>... choices) {
		this(data, durationFrames, output, true, choices);
	}

	public ValueSequencePush(ValueSequenceData data, Producer<Scalar> durationFrames, Scalar output, boolean repeat, Producer<Scalar>... choices) {
		super(data, durationFrames, output, repeat, choices);
		choice = new Choice(scalarsDivide(wavePosition(), durationFrames()),
						choices(in -> a(1, output(), in)));
	}

	@Override
	public void prepareScope(ScopeInputManager manager) {
		super.prepareScope(manager);
		choice.prepareScope(manager);

		scope = new HybridScope(this);
		scope.add(choice.getScope());
	}
}
