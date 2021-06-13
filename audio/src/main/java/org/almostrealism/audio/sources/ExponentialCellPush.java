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

import io.almostrealism.code.ScopeInputManager;
import io.almostrealism.code.expressions.Expression;
import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;

public class ExponentialCellPush extends ExponentialComputation {
	public ExponentialCellPush(ExponentialCellData data, Producer<Scalar> envelope, Scalar output) {
		super(data, envelope, output);
	}

	@Override
	public void prepareScope(ScopeInputManager manager) {
		super.prepareScope(manager);

		purgeVariables();

		StringBuilder exp = new StringBuilder();
		exp.append(getEnvelope().get(0).getExpression());
		exp.append(" * ");
		exp.append(getOutputScale().get(0).getExpression());
		exp.append(" * (");
		exp.append("exp(");
		exp.append(getNotePosition().get(0).getExpression());
		exp.append(" * ");
		exp.append(getInputScale().get(0).getExpression());
		exp.append(") - 1) * ");
		exp.append(getDepth().get(0).getExpression());

		addVariable(getOutput().get(0).assign(
				new Expression<>(Double.class, exp.toString(), getOutput(), getNotePosition(),
						getInputScale(), getOutputScale(), getDepth(), getEnvelope())));
	}
}
