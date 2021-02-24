/*
 * Copyright 2020 Michael Murray
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

package com.almostrealism.audio.optimize;

import java.util.ArrayList;
import java.util.List;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.graph.Cell;
import org.almostrealism.graph.CellFactory;
import org.almostrealism.graph.ProbabilityDensityCellFactory;
import org.almostrealism.heredity.Chromosome;
import org.almostrealism.heredity.Genome;
import org.almostrealism.organs.OrganFactory;
import org.almostrealism.organs.SimpleOrgan;

import com.almostrealism.audio.filter.DelayCellFactory;

public class SimpleOrganFactory<T> implements OrganFactory<T, SimpleOrgan<T>> {
	public static int minDelay = 40;
	public static int maxDelay = 8000;
	
	public static SimpleOrganFactory<Scalar> defaultFactory;
	
	static {
		CellFactory choices[] = { new DelayCellFactory(minDelay, maxDelay, 1) };
		defaultFactory = new SimpleOrganFactory<>(new ProbabilityDensityCellFactory<>(choices, 0));
	}
	
	private CellFactory<Double, T> factory;
	
	public SimpleOrganFactory(CellFactory<Double, T> f) { this.factory = f; }

	@Override
	public SimpleOrgan<T> generateOrgan(Genome genome) {
		return generateOrgan((Chromosome<Double>) genome.get(0), (Chromosome<T>) genome.get(1));
	}

	public SimpleOrgan<T> generateOrgan(Chromosome<Double> x, Chromosome<T> y) {
		List<Cell<T>> cells = new ArrayList<Cell<T>>();

		for (int i = 0; i < x.length(); i++) {
			cells.add(factory.generateCell(x.getGene(i)));
		}
		
		// Return a new organ with the specified cells
		// plus the Y chromosome which controls the
		// scale of expression for each cell
		return new SimpleOrgan<>(cells, y);
	}
}
