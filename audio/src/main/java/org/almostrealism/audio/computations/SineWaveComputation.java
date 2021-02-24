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

package org.almostrealism.audio.computations;

import org.almostrealism.audio.SineWaveCellData;
import io.almostrealism.code.ArrayVariable;
import io.almostrealism.code.expressions.Expression;
import io.almostrealism.code.expressions.Sum;
import io.almostrealism.relation.Producer;
import io.almostrealism.relation.Provider;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.hardware.DynamicOperationComputationAdapter;
import io.almostrealism.code.ScopeInputManager;

import java.util.function.Supplier;

public class SineWaveComputation extends DynamicOperationComputationAdapter {
	private static double TWO_PI = 2 * Math.PI;

	public SineWaveComputation(SineWaveCellData data, Producer<Scalar> envelope, Scalar output) {
		super(() -> new Provider<>(output),
				() -> data.getWavePosition(),
				() -> data.getWaveLength(),
				() -> data.getNotePosition(),
				() -> data.getNoteLength(),
				() -> data.getPhase(),
				() -> data.getAmplitude(),
				() -> data.getDepth(),
				(Supplier) envelope);
	}

	public ArrayVariable getOutput() { return getArgument(0); }
	public ArrayVariable getWavePosition() { return getArgument(1); }
	public ArrayVariable getWaveLength() { return getArgument(2); }
	public ArrayVariable getNotePosition() { return getArgument(3); }
	public ArrayVariable getNoteLength() { return getArgument(4); }
	public ArrayVariable getPhase() { return getArgument(5); }
	public ArrayVariable getAmplitude() { return getArgument(6); }
	public ArrayVariable getDepth() { return getArgument(7); }
	public ArrayVariable getEnvelope() { return getArgument(8); }

	@Override
	public void prepareScope(ScopeInputManager manager) {
		super.prepareScope(manager);

		purgeVariables();

		StringBuffer exp = new StringBuffer();
		exp.append(getEnvelope().get(0).getExpression());
		exp.append(" * ");
		exp.append(getAmplitude().get(0).getExpression());
		exp.append(" * ");
		exp.append("sin((");
		exp.append(getWavePosition().get(0).getExpression());
		exp.append(" + ");
		exp.append(getPhase().get(0).getExpression());
		exp.append(") * ");
		exp.append(stringForDouble(TWO_PI));
		exp.append(") * ");
		exp.append(getDepth().get(0).getExpression());

		addVariable(getOutput().get(0).assign(
				new Expression<>(Double.class, exp.toString(), getOutput(), getAmplitude(),
						getWavePosition(), getPhase(), getDepth(), getEnvelope())));

		addVariable(getWavePosition().get(0).assign(
				new Sum(getWavePosition().get(0), getWaveLength().get(0))));
		addVariable(getNotePosition().get(0).assign(
				new Expression<>(Double.class, getNotePosition().get(0).getExpression() +
							" + " + stringForDouble(1.0) + " / " + getNoteLength().get(0).getExpression(),
						getNotePosition(), getNoteLength())));
	}
}
