/*
 * Copyright 2016 Michael Murray
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

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.almostrealism.heredity.Genome;
import org.almostrealism.organs.OrganFactory;
import org.almostrealism.organs.SimpleOrgan;
import org.almostrealism.population.Population;
import io.almostrealism.uml.ModelEntity;

@ModelEntity
public class SimpleOrganPopulation<T> implements Population<T, SimpleOrgan<T>> {
	private List<Genome> genomes;
	private List<SimpleOrgan<T>> organs;
	
	public SimpleOrganPopulation() {
		this(new ArrayList<>());
	}
	
	public SimpleOrganPopulation(List<Genome> g) {
		genomes = g;
		organs = new ArrayList<>();
	}

	public SimpleOrganPopulation(int size, Supplier<Genome> generator) {
		genomes = new ArrayList<>();
		organs = new ArrayList<>();

		for (int i = 0; i < size; i++) {
			genomes.add(generator.get());
		}
	}

	@Override
	public void init(OrganFactory<T, SimpleOrgan<T>> factory) {
		for (int i = 0; i < genomes.size(); i++) {
			this.organs.add(factory.generateOrgan(genomes.get(i)));
		}
	}

	@Override
	public void merge(Population<T, SimpleOrgan<T>> pop) {
		this.genomes.addAll(pop.getGenomes());

		for (int i = 0; i < pop.size(); i++) {
			this.organs.add(pop.getOrgan(i));
		}
	}

	@Override
	public List<Genome> getGenomes() { return genomes; }

	@Override
	public Genome getGenome(SimpleOrgan<T> organ) {
		return genomes.get(organs.indexOf(organ));
	}

	@Override
	public SimpleOrgan<T> getOrgan(int index) { return this.organs.get(index); }

	@Override
	public int indexOf(SimpleOrgan<T> organ) { return this.organs.indexOf(organ); }

	@Override
	public int size() { return this.organs.size(); }
}
