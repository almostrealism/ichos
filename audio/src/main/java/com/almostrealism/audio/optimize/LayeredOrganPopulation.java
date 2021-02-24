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

package com.almostrealism.audio.optimize;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.almostrealism.heredity.Genome;
import org.almostrealism.organs.AdjustmentLayerOrganSystem;
import org.almostrealism.organs.OrganFactory;
import org.almostrealism.population.Population;

public class LayeredOrganPopulation<G, O, A, R> implements Population<O, AdjustmentLayerOrganSystem<G, O, A, R>> {
	public static boolean enableLazyPopulation = true;

	private List<Genome> pop;
	private List<AdjustmentLayerOrganSystem<G, O, A, R>> organs;

	private OrganFactory<O, AdjustmentLayerOrganSystem<G, O, A, R>> organFactory;

	public LayeredOrganPopulation() { this(new ArrayList<>()); }
	
	public LayeredOrganPopulation(List<Genome> population) {
		this.pop = population;
		this.organs = new ArrayList<>();
	}

	@Override
	public void init(OrganFactory<O, AdjustmentLayerOrganSystem<G, O, A, R>> organFactory) {
		this.organFactory = organFactory;

		if (!enableLazyPopulation) {
			organs = pop.stream().map(organFactory::generateOrgan).collect(Collectors.toList());
		}
	}

	@Override
	public void merge(Population<O, AdjustmentLayerOrganSystem<G, O, A, R>> pop) {
		this.pop.addAll(pop.getGenomes());

		for (int i = 0; i < pop.size(); i++) {
			this.organs.add(pop.getOrgan(i));
		}
	}

	@Override
	public int indexOf(AdjustmentLayerOrganSystem<G, O, A, R> organ) { return organs.indexOf(organ); }

	@Override
	public List<Genome> getGenomes() { return pop; }

	@Override
	public Genome getGenome(AdjustmentLayerOrganSystem<G, O, A, R> organ) {
		return pop.get(organs.indexOf(organ));
	}

	@Override
	public AdjustmentLayerOrganSystem<G, O, A, R> getOrgan(int index) {
		if (enableLazyPopulation) {
			AdjustmentLayerOrganSystem<G, O, A, R> organ = organFactory.generateOrgan(getGenomes().get(index));
			organ.setName("Organ-" + index);
			return organ;
		} else {
			return organs.get(index);
		}
	}

	@Override
	public int size() { return getGenomes().size(); }
}
