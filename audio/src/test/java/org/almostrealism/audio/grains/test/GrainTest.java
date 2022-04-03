package org.almostrealism.audio.grains.test;

import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.ScalarProducer;
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.WaveOutput;
import org.almostrealism.audio.data.WaveData;
import org.almostrealism.audio.grains.Grain;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.TraversalPolicy;
import org.almostrealism.graph.ReceptorCell;
import org.almostrealism.hardware.MemoryBank;
import org.almostrealism.time.CursorPair;
import org.junit.Test;

import java.io.File;

public class GrainTest implements CellFeatures {
	@Test
	public void grains() {
		WaveOutput source = new WaveOutput();
		w(new File("Library/organ.wav")).map(i -> new ReceptorCell<>(source)).sec(1.0, false).get().run();

		Grain grain = new Grain();
		grain.setStart(0.2);
		grain.setDuration(0.015);
		grain.setRate(2.0);

		TraversalPolicy grainShape = new TraversalPolicy(3);
		Producer<PackedCollection> g = v(PackedCollection.class, 1, -1);

		ScalarProducer pos = scalar(grainShape, g, 0).add(
				mod(scalar(grainShape, g, 2).multiply(
						v(Scalar.class, 0)), scalar(grainShape, g, 1)))
				.multiply(OutputLine.sampleRate);
		Producer cursor = pair(pos, v(0.0));

		ScalarBank result = new ScalarBank(10 * OutputLine.sampleRate);
		System.out.println("GrainTest: Evaluating timeline kernel...");
		source.getData().valueAt(cursor).get().kernelEvaluate(result, new MemoryBank[] { WaveOutput.timeline.getValue(), grain });
		System.out.println("GrainTest: Timeline kernel evaluated");

		System.out.println("GrainTest: Rendering grains...");
		w(new WaveData(result, OutputLine.sampleRate)).o(i -> new File("results/grain-test.wav")).sec(5).get().run();
		System.out.println("GrainTest: Done");
	}
}
