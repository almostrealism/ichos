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
import io.almostrealism.relation.Evaluable;
import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.computations.Choice;
import org.almostrealism.audio.data.PolymorphicAudioData;
import org.almostrealism.audio.filter.AudioCellAdapter;
import org.almostrealism.graph.Cell;
import org.almostrealism.hardware.OperationList;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AudioCellChoiceAdapter extends AudioCellAdapter implements CellFeatures {
	private final ProducerComputation<Scalar> decision;
	private final List<AudioCellAdapter> cells;
	private final boolean parallel;

	private final ScalarBank storage;

	public AudioCellChoiceAdapter(ProducerComputation<Scalar> decision,
								  IntFunction<PolymorphicAudioData> data,
								  List<Function<PolymorphicAudioData, ? extends AudioCellAdapter>> choices,
								  boolean parallel) {
		this.decision = decision;
		this.cells = IntStream.range(0, choices.size())
				.mapToObj(i -> choices.get(i).apply(data.apply(i)))
				.collect(Collectors.toList());
		this.parallel = parallel;

		if (parallel) {
			storage = new ScalarBank(choices.size());
			IntStream.range(0, cells.size())
					.forEach(i -> cells.get(i).setReceptor(a(p(storage.get(i)))));
		} else {
			storage = new ScalarBank(1);
			cells.forEach(cell -> cell.setReceptor(a(p(storage.get(0)))));
		}
	}

	@Override
	public Supplier<Runnable> setup() {
		Evaluable<Scalar> d = decision.get();

		return () -> () -> {
			double v = d.evaluate().getValue();
			double in = 1.0 / cells.size();

			for (int i = 0; i < cells.size(); i++) {
				if (parallel) {
					cells.get(i).setup().get().run();
				} else if (v <= (i + 1) * in) {
					cells.get(i).setup().get().run();
					return;
				}
			}
		};
	}

	@Override
	public Supplier<Runnable> push(Producer<Scalar> protein) {
		OperationList push = new OperationList();

		if (parallel) {
			cells.stream().map(cell -> cell.push(protein)).forEach(push::add);
			push.add(new Choice(decision, storage.stream()
					.map(v -> (Computation) getReceptor().push(p(v)))
					.collect(Collectors.toList())));
		} else {
			push.add(new Choice(decision,
					cells.stream().map(cell -> (Computation) cell.push(protein))
							.collect(Collectors.toList())));
			push.add(getReceptor().push(p(storage.get(0))));
		}

		return push;
	}

	@Override
	public Supplier<Runnable> tick() {
		if (parallel) {
			OperationList tick = new OperationList();
			cells.stream().map(AudioCellAdapter::tick).forEach(tick::add);
			return tick;
		} else {
			return new Choice(decision,
					cells.stream().map(AudioCellAdapter::tick).map(v -> (Computation) v)
							.collect(Collectors.toList()));
		}
	}

	@Override
	public void reset() {
		super.reset();
		cells.forEach(Cell::reset);
	}
}
