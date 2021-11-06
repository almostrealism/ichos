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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class AudioCellChoiceAdapter extends AudioCellAdapter implements CellFeatures {
	private ProducerComputation<Scalar> decision;
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
			initParallel();
		} else {
			storage = new ScalarBank(1);
			cells.forEach(cell -> cell.setReceptor(a(p(storage.get(0)))));
		}
	}

	public void setDecision(ProducerComputation<Scalar> decision) {
		this.decision = decision;
	}

	private void initParallel() {
		getCellSet().forEach(c ->
				c.setReceptor(a(indexes(c).mapToObj(storage::get).map(this::p).toArray(Supplier[]::new))));
	}

	private IntStream indexes(AudioCellAdapter c) {
		return IntStream.range(0, cells.size()).filter(i -> cells.get(i) == c);
	}

	protected Set<AudioCellAdapter> getCellSet() {
		HashSet<AudioCellAdapter> set = new HashSet<>();

		n: for (AudioCellAdapter n : cells) {
			for (AudioCellAdapter c : set) {
				if (c == n) continue n;
			}

			set.add(n);
		}

		return set;
	}

	@Override
	public Supplier<Runnable> setup() {
		Evaluable<Scalar> d = decision.get();

		return () -> () -> {
			if (parallel) {
				getCellSet().forEach(c -> c.setup().get().run());
			} else {
				double v = d.evaluate().getValue();
				double in = 1.0 / cells.size();

				for (int i = 0; i < cells.size(); i++) {
					if (v <= (i + 1) * in) {
						cells.get(i).setup().get().run();
						return;
					}
				}
			}
		};
	}

	@Override
	public Supplier<Runnable> push(Producer<Scalar> protein) {
		OperationList push = new OperationList();

		if (parallel) {
			getCellSet().stream().map(cell -> cell.push(protein)).forEach(push::add);
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
			return getCellSet().stream().map(AudioCellAdapter::tick).collect(OperationList.collector());
		} else {
			return new Choice(decision,
					cells.stream().map(AudioCellAdapter::tick).map(v -> (Computation) v)
							.collect(Collectors.toList()));
		}
	}

	@Override
	public void reset() {
		super.reset();
		getCellSet().forEach(Cell::reset);
	}
}
