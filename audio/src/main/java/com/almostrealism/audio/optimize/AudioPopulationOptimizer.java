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

import com.almostrealism.audio.health.StableDurationHealthComputation;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.heredity.Genome;
import org.almostrealism.heredity.GenomeBreeder;
import org.almostrealism.optimize.AverageHealthComputationSet;
import org.almostrealism.optimize.PopulationOptimizer;
import org.almostrealism.organs.Organ;
import org.almostrealism.organs.OrganFactory;
import org.almostrealism.population.Population;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class AudioPopulationOptimizer<O extends Organ<Scalar>> extends PopulationOptimizer<Scalar, O> implements Runnable {
	public static boolean enableWavOutput = true;

	private String file;

	private int tot = 100;

	public AudioPopulationOptimizer(Population<Scalar, O> p, OrganFactory<Scalar, O> f,
									Function<List<Genome>, Population> children,
									GenomeBreeder breeder, Supplier<Genome> generator, String file) {
		super(p, f, healthComputation(), children, breeder, generator);
		this.file = file;
	}

	public void init() throws FileNotFoundException {
		if (enableWavOutput) {
			((AverageHealthComputationSet) getHealthComputation()).addListener((hc, o) -> {
				if (hc instanceof StableDurationHealthComputation) {
					((StableDurationHealthComputation) hc)
							.setDebugOutputFile("health/" + ((Organ) o).getName() + ".wav");
				}
			});
		}

		if (new File(file).exists()) {
			getPopulation().read(new FileInputStream(file));
			PopulationOptimizer.console.println("Read chromosome data from " + file);
		} else {
			List<Genome> genomes = new ArrayList<>();

			for (int i = 0; i < PopulationOptimizer.popSize; i++) {
				genomes.add(getGenerator().get());
			}

			setPopulation(getChildrenFunction().apply(genomes));
			PopulationOptimizer.console.println("Generated initial population");
		}

		getPopulation().init(getOrganFactory());
		PopulationOptimizer.console.println(getPopulation().size() + " organs in population");
	}

	@Override
	public void run() {
		for (int i = 0; i < tot; i++) {
			iterate();
			storePopulation();

			System.gc();
		}
	}

	public void storePopulation() {
		try {
			getPopulation().store(new FileOutputStream(file));
			PopulationOptimizer.console.println("Wrote " + file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static AverageHealthComputationSet<Scalar> healthComputation() {
		AverageHealthComputationSet<Scalar> health = new AverageHealthComputationSet<>();
		health.add(new StableDurationHealthComputation());
		return health;
	}
}
