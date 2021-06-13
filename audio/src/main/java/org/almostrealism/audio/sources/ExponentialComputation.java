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

import io.almostrealism.code.ArrayVariable;
import io.almostrealism.code.expressions.Expression;
import io.almostrealism.relation.Producer;
import io.almostrealism.relation.Provider;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.hardware.DynamicOperationComputationAdapter;
import io.almostrealism.code.ScopeInputManager;

import java.util.function.Supplier;

public abstract class ExponentialComputation extends DynamicOperationComputationAdapter {
	public ExponentialComputation(ExponentialCellData data, Producer<Scalar> envelope, Scalar output) {
		super(() -> new Provider<>(output),
				data::getNotePosition,
				data::getNoteLength,
				data::getInputScale,
				data::getOutputScale,
				data::getDepth,
				(Supplier) envelope);
	}

	public ArrayVariable getOutput() { return getArgument(0); }
	public ArrayVariable getNotePosition() { return getArgument(1); }
	public ArrayVariable getNoteLength() { return getArgument(2); }
	public ArrayVariable getInputScale() { return getArgument(3); }
	public ArrayVariable getOutputScale() { return getArgument(4); }
	public ArrayVariable getDepth() { return getArgument(5); }
	public ArrayVariable getEnvelope() { return getArgument(6); }
}
