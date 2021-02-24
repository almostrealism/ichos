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

package com.almostrealism.audio.filter.test;

import com.almostrealism.audio.health.StableDurationHealthComputation;
import com.almostrealism.audio.optimize.DefaultCellAdjustmentFactory;
import com.almostrealism.audio.optimize.SimpleOrganFactory;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.hardware.mem.MemoryBankAdapter;
import org.almostrealism.heredity.ArrayListChromosome;
import org.almostrealism.heredity.ArrayListGene;
import org.almostrealism.heredity.Genome;
import org.almostrealism.heredity.ScaleFactor;
import org.almostrealism.organs.AdjustmentLayerOrganSystem;
import org.almostrealism.organs.AdjustmentLayerOrganSystemFactory;
import org.almostrealism.organs.TieredCellAdjustmentFactory;
import org.almostrealism.time.AcceleratedTimeSeries;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class PeriodicCellAdjustmentTest {
	@BeforeClass
	public static void init() {
		AcceleratedTimeSeries.defaultCacheLevel = MemoryBankAdapter.CacheLevel.ALL;
		StableDurationHealthComputation.enableVerbose = true;
	}

	@AfterClass
	public static void shutdown() {
		AcceleratedTimeSeries.defaultCacheLevel = MemoryBankAdapter.CacheLevel.NONE;
		StableDurationHealthComputation.enableVerbose = false;
	}

	protected AdjustmentLayerOrganSystemFactory<Scalar, Long, Scalar, Long> factory() {
		TieredCellAdjustmentFactory<Scalar, Scalar> tca =
				new TieredCellAdjustmentFactory<>(new DefaultCellAdjustmentFactory());
		return new AdjustmentLayerOrganSystemFactory<>(tca, SimpleOrganFactory.defaultFactory);
	}

	protected AdjustmentLayerOrganSystem organ() {
		ArrayListChromosome<Scalar> x = new ArrayListChromosome();
		x.add(new ArrayListGene<>(1.0, 0.2));
		x.add(new ArrayListGene<>(1.0, 0.2));

		ArrayListChromosome<Scalar> y = new ArrayListChromosome();
		y.add(new ArrayListGene<>(new ScaleFactor(0.0), new ScaleFactor(1.0)));
		y.add(new ArrayListGene<>(new ScaleFactor(1.0), new ScaleFactor(0.0)));

		ArrayListChromosome<Scalar> a = new ArrayListChromosome();
		a.add(new ArrayListGene<>(0.1, 0.0, 1.0));
		a.add(new ArrayListGene<>(0.1, 0.0, 1.0));

		Genome genome = new Genome();
		genome.add(x);
		genome.add(y);
		genome.add(a);

		return factory().generateOrgan(genome);
	}

	@Test
	public void pushTest() {
		AdjustmentLayerOrganSystem organ = organ();
		// TODO
		// organ().push(v(1.0)).
	}

	@Test
	public void healthTest() {
		StableDurationHealthComputation health = new StableDurationHealthComputation();
		health.setMaxDuration(8);
		health.setDebugOutputFile("health/periodic-test.wav");

		AdjustmentLayerOrganSystem organ = organ();
		health.computeHealth(organ);
	}

	@Test
	public void healthTestBatch() {
		StableDurationHealthComputation health = new StableDurationHealthComputation();
		health.setMaxDuration(8);
		health.setBatchSize(OutputLine.sampleRate);
		health.setDebugOutputFile("health/periodic-test-batch.wav");

		AdjustmentLayerOrganSystem organ = organ();
		health.computeHealth(organ);
	}
}
