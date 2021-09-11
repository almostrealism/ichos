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

import io.almostrealism.uml.Lifecycle;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.graph.Cell;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.heredity.Factor;
import org.almostrealism.time.Temporal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class CellList extends ArrayList<Cell<Scalar>> implements Temporal, Lifecycle, CellFeatures {
	private CellList parent;
	private List<Runnable> finals;

	public CellList() { this(null); }

	public CellList(CellList parent) {
		this.parent = parent;
		this.finals = new ArrayList<>();
	}

	public CellList f(IntFunction<Factor<Scalar>> filter) {
		return f(this, filter);
	}

	public CellList o(IntFunction<File> f) {
		return o(this, f);
	}

	public Supplier<Runnable> min(double minutes) { return min(this, minutes); }

	public Supplier<Runnable> sec(double seconds) { return sec(this, seconds); }

	public CellList getParent() { return parent; }

	public List<Runnable> getFinals() { return finals; }

	public Collection<Cell<Scalar>> getAll() {
		List<Cell<Scalar>> all = new ArrayList<>();
		if (parent != null) {
			all.addAll(parent.getAll());
		}

		forEach(c -> {
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i) == c) {
					return;
				}
			}

			all.add(c);
		});

		return all;
	}

	@Override
	public Supplier<Runnable> tick() {
		OperationList tick = new OperationList();
		getAll().stream().map(c -> c instanceof Temporal ? (Temporal) c : null)
				.filter(Objects::nonNull)
				.map(Temporal::tick)
				.forEach(tick::add);
		return tick;
	}

	@Override
	public void reset() {
		if (parent != null) parent.reset();
		finals.forEach(Runnable::run);
	}
}
