package org.almostrealism.audio.optimize.test;

import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.optimize.LinearInterpolationChromosome;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.graph.TimeCell;
import org.almostrealism.graph.temporal.TemporalFactorFromCell;
import org.almostrealism.graph.temporal.WaveCell;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.heredity.ConfigurableGenome;
import org.almostrealism.heredity.SimpleChromosome;
import org.almostrealism.heredity.SimpleGene;
import org.almostrealism.time.TemporalList;
import org.almostrealism.util.TestFeatures;
import org.junit.Assert;
import org.junit.Test;

public class LinearInterpolationChromosomeTest implements CellFeatures, TestFeatures {
	public static int channels = 1;
	public static int channel = 0;

	public static double totalDuration = 16.0;
	public static double duration = 8.0;

	public static boolean enableDebugReceptor = true;

	@Test
	public void interpolate() {
		TimeCell clock = new TimeCell();

		SimpleChromosome chromosome = new ConfigurableGenome().addSimpleChromosome(LinearInterpolationChromosome.SIZE);

		for (int i = 0; i < channels; i++) {
			SimpleGene g = chromosome.addGene();
			g.set(0, 0.0);
			g.set(1, 1.0);
		}

		LinearInterpolationChromosome interpolate =
				new LinearInterpolationChromosome(chromosome, 2000.0, 20000.0, OutputLine.sampleRate);
		interpolate.setGlobalTime(clock.frame());

		OperationList setup = new OperationList();
		setup.add(() -> () -> interpolate.setDuration(totalDuration));
		setup.add(interpolate.expand());

		TemporalList temporal = new TemporalList();
		temporal.add(clock);
		temporal.add(interpolate.getTemporals());

		PackedCollection<?> out = new PackedCollection<>(1);

		Producer<PackedCollection<?>> result = interpolate.valueAt(channel, 0).getResultant(c(1.0));

		OperationList process = new OperationList();
		process.add(sec(temporal, duration, false));
		process.add(a(1, p(out), result));

		setup.get().run();
		process.get().run();

		System.out.println("Value after " + duration + " seconds: " + out.toDouble(0));

		Scalar count = clock.frame().get().evaluate();
		System.out.println("Clock: " + count.toDouble(0) + " frames after " + duration + " seconds");

		assertEquals(duration * OutputLine.sampleRate, count);
	}

	@Test
	public void waveCell() {
		TimeCell clock = new TimeCell();

		SimpleChromosome chromosome = new ConfigurableGenome().addSimpleChromosome(LinearInterpolationChromosome.SIZE);

		for (int i = 0; i < channels; i++) {
			SimpleGene g = chromosome.addGene();
			g.set(0, 0.0);
			g.set(1, 1.0);
		}

		LinearInterpolationChromosome interpolate =
				new LinearInterpolationChromosome(chromosome, 2000.0, 20000.0, OutputLine.sampleRate);
		interpolate.setGlobalTime(clock.frame());

		OperationList setup = new OperationList();
		setup.add(() -> () -> interpolate.setDuration(totalDuration));
		setup.add(interpolate.expand());

		TemporalFactorFromCell factor = (TemporalFactorFromCell) interpolate.valueAt(channel, 0);
		WaveCell cell = (WaveCell) factor.getCell();

		TemporalList temporal = new TemporalList();
		temporal.add(clock);
		temporal.add(cell);

		Producer<PackedCollection<?>> result = factor.getResultant(c(1.0));
		PackedCollection<?> out = new PackedCollection<>(1);

		if (enableDebugReceptor) {
			cell.setReceptor(protein -> a(1, p(out), protein));
		}

		OperationList process = new OperationList();
		process.add(sec(temporal, duration, false));
		if (!enableDebugReceptor) process.add(a(1, p(out), result));

		setup.get().run();
		process.get().run();

		System.out.println("Value after " + duration + " seconds: " + out.toDouble(0));

		Scalar count = clock.frame().get().evaluate();
		System.out.println("Clock: " + count.toDouble(0) + " frames after " + duration + " seconds");

		assertEquals(duration * OutputLine.sampleRate, count);
	}
}
