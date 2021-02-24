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

package com.almostrealism.audio.health.test;

import com.almostrealism.audio.SineWaveCell;
import com.almostrealism.audio.filter.test.AdjustableDelayCellTest;
import com.almostrealism.audio.health.StableDurationHealthComputation;
import com.almostrealism.audio.health.WaveOutput;
import com.almostrealism.audio.optimize.SimpleOrganFactory;
import io.almostrealism.code.OperationAdapter;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.heredity.ArrayListChromosome;
import org.almostrealism.heredity.ArrayListGene;
import org.almostrealism.heredity.Genome;
import org.almostrealism.heredity.ScaleFactor;
import org.almostrealism.organs.SimpleOrgan;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.stream.IntStream;

public class StableDurationHealthComputationTest extends AdjustableDelayCellTest {
	@BeforeClass
	public static void init() {
		StableDurationHealthComputation.enableVerbose = true;
	}

	@AfterClass
	public static void shutdown() {
		StableDurationHealthComputation.enableVerbose = false;
	}

	protected SimpleOrgan<Scalar> organ() {
		ArrayListChromosome<Double> x = new ArrayListChromosome();
		x.add(new ArrayListGene<>(1.0, 0.2));
		x.add(new ArrayListGene<>(1.0, 0.2));

		ArrayListChromosome<Scalar> y = new ArrayListChromosome();
		y.add(new ArrayListGene<>(new ScaleFactor(0.0), new ScaleFactor(1.0)));
		y.add(new ArrayListGene<>(new ScaleFactor(1.0), new ScaleFactor(0.0)));

		Genome genome = new Genome();
		genome.add(x);
		genome.add(y);

		return SimpleOrganFactory.defaultFactory.generateOrgan(genome);
	}

	@Test
	public void firstCell() {
		WaveOutput output = new WaveOutput(new File("health/first-cell-test.wav"));

		SimpleOrgan<Scalar> organ = organ();
		organ.firstCell().setReceptor(output);

		SineWaveCell cell = cell();
		cell.setReceptor(organ);

		Runnable push = cell.push(null).get();
		((OperationAdapter) push).compile();

		Runnable tick = organ.tick().get();
		((OperationAdapter) tick).compile();

		IntStream.range(0, 5 * OutputLine.sampleRate).forEach(i -> {
			push.run();
			tick.run();
			if ((i + 1) % 1000 == 0) System.out.println("StableDurationHealthComputationTest: " + (i + 1) + " iterations");
		});

		System.out.println("StableDurationHealthComputationTest: Writing WAV...");
		output.write().get().run();
		System.out.println("Done");
	}

	@Test
	public void simpleOrganHealth() {
		StableDurationHealthComputation health = new StableDurationHealthComputation();
		health.setMaxDuration(4);
		health.setBatchSize(1000);
		health.setDebugOutputFile("health/simple-organ-test.wav");

		SimpleOrgan<Scalar> organ = organ();
		health.computeHealth(organ);
	}
}
