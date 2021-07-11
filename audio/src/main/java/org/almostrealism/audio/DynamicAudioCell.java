/*
 * Copyright 2021 Michael Murray
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

package org.almostrealism.audio;

import io.almostrealism.code.Computation;
import io.almostrealism.code.ProducerComputation;
import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.computations.Choice;
import org.almostrealism.audio.data.PolymorphicAudioData;
import org.almostrealism.audio.filter.AudioCellAdapter;
import org.almostrealism.graph.Cell;
import org.almostrealism.graph.Receptor;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DynamicAudioCell extends AudioCellAdapter {
	private final PolymorphicAudioData data;
	private final ProducerComputation<Scalar> decision;
	private final List<AudioCellAdapter> cells;


	public DynamicAudioCell(ProducerComputation<Scalar> decision,
							List<Function<PolymorphicAudioData, ? extends AudioCellAdapter>> choices) {
		this(new PolymorphicAudioData(), decision, choices);
	}

	public DynamicAudioCell(PolymorphicAudioData data, ProducerComputation<Scalar> decision,
							List<Function<PolymorphicAudioData, ? extends AudioCellAdapter>> choices) {
		this.data = data;
		this.decision = decision;
		this.cells = choices.stream()
				.map(choice -> choice.apply(data))
				.collect(Collectors.toList());
	}

	@Override
	public void setReceptor(Receptor<Scalar> r) {
		super.setReceptor(r);
		cells.forEach(c -> c.setReceptor(r));
	}

	@Override
	public Supplier<Runnable> setup() {
		return () -> () -> {
			double v = decision.get().evaluate().getValue();
			double in = 1.0 / cells.size();

			for (int i = 0; i < cells.size(); i++) {
				if (v <= (i + 1) * in) {
					cells.get(i).setup().get().run();
					return;
				}
			}
		};
	}

	@Override
	public Supplier<Runnable> push(Producer<Scalar> protein) {
		return new Choice(decision,
				cells.stream().map(cell -> (Computation) cell.push(protein))
						.collect(Collectors.toList()));
	}

	@Override
	public Supplier<Runnable> tick() {
		return new Choice(decision,
				cells.stream().map(AudioCellAdapter::tick).map(v -> (Computation) v)
						.collect(Collectors.toList()));
	}

	@Override
	public void reset() {
		super.reset();
		cells.forEach(Cell::reset);
	}
}
