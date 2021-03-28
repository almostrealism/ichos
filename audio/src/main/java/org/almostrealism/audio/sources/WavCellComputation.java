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
import io.almostrealism.code.ScopeInputManager;
import io.almostrealism.code.expressions.Expression;
import io.almostrealism.code.expressions.Sum;
import io.almostrealism.relation.Provider;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.hardware.DynamicOperationComputationAdapter;

public class WavCellComputation extends DynamicOperationComputationAdapter {
	public WavCellComputation(WavCellData data, ScalarBank wave, Scalar output) {
		super(() -> new Provider<>(output),
				() -> new Provider<>(wave),
				data::getWavePosition,
				data::getWaveLength,
				data::getAmplitude);
	}

	public ArrayVariable getOutput() { return getArgument(0); }
	public ArrayVariable getWave() { return getArgument(1); }
	public ArrayVariable getWavePosition() { return getArgument(2); }
	public ArrayVariable getWaveLength() { return getArgument(3); }
	public ArrayVariable getAmplitude() { return getArgument(4); }

	@Override
	public void prepareScope(ScopeInputManager manager) {
		super.prepareScope(manager);

		purgeVariables();

		StringBuffer exp = new StringBuffer();
		exp.append(getAmplitude().get(0).getExpression());
		exp.append(" * ");
		exp.append(getWave().get(getWavePosition().get(0).getExpression()).getExpression());

		addVariable(getOutput().get(0).assign(
				new Expression<>(Double.class, exp.toString(), getOutput(), getWave(),
						getAmplitude(), getWavePosition())));

		addVariable(getWavePosition().get(0).assign(
				new Sum(getWavePosition().get(0), getWaveLength().get(0))));
	}
}