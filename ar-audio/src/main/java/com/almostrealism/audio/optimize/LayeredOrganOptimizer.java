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

import java.io.FileNotFoundException;
import java.util.function.Supplier;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.breeding.Breeders;
import org.almostrealism.heredity.DefaultGenomeBreeder;
import org.almostrealism.heredity.FloatingPointRandomChromosomeFactory;
import org.almostrealism.heredity.Genome;
import org.almostrealism.heredity.GenomeBreeder;
import org.almostrealism.heredity.ScaleFactor;
import org.almostrealism.organs.AdjustmentLayerOrganSystem;
import org.almostrealism.organs.AdjustmentLayerOrganSystemFactory;
import org.almostrealism.organs.TieredCellAdjustmentFactory;

public class LayeredOrganOptimizer extends AudioPopulationOptimizer<AdjustmentLayerOrganSystem<Double, Scalar, Double, Scalar>> {
	private static double defaultMinFeedback = 0.01;
	private static double defaultMaxFeedback = 0.5;

	public LayeredOrganOptimizer(LayeredOrganPopulation<Double, Scalar, Double, Scalar> p,
								 AdjustmentLayerOrganSystemFactory<Double, Scalar, Double, Scalar> f,
								 GenomeBreeder breeder, Supplier<Genome> generator) {
		super(p, f,
				LayeredOrganPopulation::new,
				breeder, generator, "LayeredPopulation.xml");
	}

	public static void main(String args[]) throws FileNotFoundException {
		int dim = 4;
		double min = defaultMinFeedback;
		double max = defaultMaxFeedback;

		// Random genetic material generators
		FloatingPointRandomChromosomeFactory xfactory = new FloatingPointRandomChromosomeFactory();
//		DefaultRandomChromosomeFactory yfactory = new DefaultRandomChromosomeFactory(min, max);
		FloatingPointRandomChromosomeFactory yfactory = new FloatingPointRandomChromosomeFactory();
		FloatingPointRandomChromosomeFactory afactory = new FloatingPointRandomChromosomeFactory();
		xfactory.setChromosomeSize(dim, 2);
		yfactory.setChromosomeSize(dim, dim);
		afactory.setChromosomeSize(dim, 3);

		GenomeFromChromosomes generator = new GenomeFromChromosomes(xfactory, yfactory, afactory);

		TieredCellAdjustmentFactory<Scalar, Scalar> tca = new TieredCellAdjustmentFactory<>(new DefaultCellAdjustmentFactory());
		AdjustmentLayerOrganSystemFactory<Double, Scalar, Double, Scalar> factory = new AdjustmentLayerOrganSystemFactory(tca, SimpleOrganFactory.defaultFactory);

		DefaultGenomeBreeder breeder = new DefaultGenomeBreeder(
				Breeders.perturbationBreeder(0.0005, ScaleFactor::new),
				Breeders.perturbationBreeder(0.0005, ScaleFactor::new),
				Breeders.perturbationBreeder(0.05, ScaleFactor::new));

		// Create and run the optimizer
		LayeredOrganOptimizer opt = new LayeredOrganOptimizer(new LayeredOrganPopulation<>(),
															factory, breeder, generator);
		opt.init();
		opt.run();
	}
}
