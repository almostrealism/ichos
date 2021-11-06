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

import io.almostrealism.code.ProducerComputation;
import io.almostrealism.code.Setup;
import io.almostrealism.uml.Plural;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.data.PolymorphicAudioData;
import org.almostrealism.audio.filter.AudioCellAdapter;
import org.almostrealism.graph.Cell;
import org.almostrealism.graph.Receptor;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.heredity.Factor;
import org.almostrealism.heredity.Gene;
import org.almostrealism.time.Temporal;
import org.almostrealism.time.TemporalList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

public class CellList extends ArrayList<Cell<Scalar>> implements Cells {
	private CellList parent;
	private List<Receptor<Scalar>> roots;
	private TemporalList requirements;
	private List<Runnable> finals;

	public CellList() { this(null); }

	public CellList(CellList parent) {
		this.parent = parent;
		this.roots = new ArrayList<>();
		this.requirements = new TemporalList();
		this.finals = new ArrayList<>();
	}

	private void setParent(CellList parent) {
		this.parent = parent;
	}

	public void addRoot(Cell<Scalar> c) {
		roots.add(c);
		add(c);
	}

	public void addRequirement(Temporal t) {
		requirements.add(t);
	}

	public CellList map(IntFunction<Cell<Scalar>> dest) {
		return map(this, dest);
	}

	public CellList poly(IntFunction<ProducerComputation<Scalar>> decision) {
		CellList l = poly(1, () -> null, decision,
				stream().map(c -> (Function<PolymorphicAudioData, AudioCellAdapter>) data -> (AudioCellAdapter) c).toArray(Function[]::new));
		// TODO  By dropping the parent, we may be losing necessary dependencies
		// TODO  However, if it is included, operations will be invoked multiple times
		// TODO  Since the new polymorphic cells delegate to the operations of the
		// TODO  original cells in this current CellList
		// l.setParent(this);
		return l;
	}

	public CellList gr(double duration, int segments, IntUnaryOperator choices) {
		return gr(this, duration, segments, choices);
	}

	public CellList f(IntFunction<Factor<Scalar>> filter) {
		return f(this, filter);
	}

	public CellList d(IntFunction<Scalar> delay) { return d(this, delay); }

	public CellList m(IntFunction<Cell<Scalar>> adapter, Plural<Gene<Scalar>> transmission) {
		return m(this, adapter, transmission);
	}

	public CellList m(IntFunction<Cell<Scalar>> adapter, IntFunction<Gene<Scalar>> transmission) {
		return m(this, adapter, transmission);
	}

	public CellList m(List<Cell<Scalar>> adapter, List<Cell<Scalar>> destinations, IntFunction<Gene<Scalar>> transmission) {
		return m(this, adapter, destinations, transmission);
	}

	public CellList mself(List<Cell<Scalar>> adapter, IntFunction<Gene<Scalar>> transmission) {
		return mself(this, adapter, transmission);
	}

	public CellList m(IntFunction<Cell<Scalar>> adapter, List<Cell<Scalar>> destinations, IntFunction<Gene<Scalar>> transmission) {
		return m(this, adapter, destinations, transmission);
	}

	public CellList mself(IntFunction<Cell<Scalar>> adapter, IntFunction<Gene<Scalar>> transmission) {
		return mself(this, adapter, transmission);
	}

	public CellList m(IntFunction<Cell<Scalar>> adapter, IntFunction<Cell<Scalar>> destinations, IntFunction<Gene<Scalar>> transmission) {
		return m(this, adapter, destinations, transmission);
	}

	public CellList csv(IntFunction<File> f) {
		return csv(this, f);
	}

	public CellList o(IntFunction<File> f) {
		return o(this, f);
	}

	public CellList om(IntFunction<File> f) {
		return om(this, f);
	}

	public Supplier<Runnable> min(double minutes) { return min(this, minutes); }

	public Supplier<Runnable> sec(double seconds) { return sec(this, seconds); }

	public CellList getParent() { return parent; }

	public TemporalList getRequirements() { return requirements; }

	public List<Runnable> getFinals() { return finals; }

	public Collection<Cell<Scalar>> getAll() {
		List<Cell<Scalar>> all = new ArrayList<>();
		if (parent != null) {
			all.addAll(parent.getAll());
		}

		forEach(c -> append(all, c));

		return all;
	}

	public TemporalList getAllTemporals() {
		TemporalList all = new TemporalList();
		if (parent != null) {
			all.addAll(parent.getAllTemporals());
		}

		stream().map(c -> c instanceof Temporal ? (Temporal) c : null)
				.filter(Objects::nonNull).forEach(t -> append(all, t));

		requirements.forEach(c -> append(all, c));

		return all;
	}

	public Collection<Receptor<Scalar>> getAllRoots() {
		List<Receptor<Scalar>> all = new ArrayList<>();
		if (parent != null) {
			all.addAll(parent.getAllRoots());
		}

		roots.forEach(c -> append(all, c));

		return all;
	}

	@Override
	public Supplier<Runnable> setup() {
		OperationList setup = new OperationList();
		getAll().stream().map(c -> c instanceof Setup ? (Setup) c : null)
				.filter(Objects::nonNull)
				.map(Setup::setup)
				.forEach(setup::add);
		return setup;
	}

	@Override
	public Supplier<Runnable> tick() {
		OperationList tick = new OperationList();
		getAllRoots().stream().map(r -> r.push(v(0.0))).forEach(tick::add);
		tick.add(getAllTemporals().tick());
		return tick;
	}

	@Override
	public void reset() {
		if (parent != null) parent.reset();
		forEach(Cell::reset);
		requirements.reset();
		finals.forEach(Runnable::run);
	}
}
