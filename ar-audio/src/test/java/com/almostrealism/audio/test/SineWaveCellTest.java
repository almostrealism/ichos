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

package com.almostrealism.audio.test;

import com.almostrealism.audio.SineWaveCell;
import com.almostrealism.audio.filter.BasicDelayCell;
import com.almostrealism.audio.health.WaveOutput;
import io.almostrealism.relation.Evaluable;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.computations.DefaultEnvelopeComputation;
import org.almostrealism.graph.Cell;
import org.almostrealism.graph.Receptor;
import org.almostrealism.graph.ReceptorCell;
import org.almostrealism.hardware.DynamicProducerForMemWrapper;
import org.almostrealism.heredity.Factor;
import org.almostrealism.heredity.Gene;
import org.almostrealism.heredity.IdentityFactor;
import org.almostrealism.organs.CellPair;
import org.almostrealism.organs.MultiCell;
import org.almostrealism.util.CodeFeatures;
import org.almostrealism.util.TestFeatures;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class SineWaveCellTest implements TestFeatures {
	private static final int DURATION_FRAMES = 10 * OutputLine.sampleRate;

	protected Receptor<Scalar> loggingReceptor() {
		return protein -> () -> () -> System.out.println(protein.get().evaluate());
	}

	protected Cell<Scalar> loggingCell() { return new ReceptorCell<>(loggingReceptor()); }

	protected SineWaveCell cell() {
		SineWaveCell cell = new SineWaveCell();
		cell.setFreq(196.00);
		cell.setNoteLength(400);
		cell.setAmplitude(0.1);
		cell.setEnvelope(DefaultEnvelopeComputation::new);

		return cell;
	}

	@Test
	public void sineWave() {
		SineWaveCell cell = cell();
		cell.setReceptor(loggingReceptor());
		Runnable push = cell.push(v(0.0)).get();
		IntStream.range(0, 100).forEach(i -> push.run());
		// TODO  Add assertions
	}

	@Test
	public void withOutput() {
		WaveOutput output = new WaveOutput(new File("health/sine-wave-cell-test.wav"));

		SineWaveCell cell = cell();
		cell.setReceptor(output);

		Runnable push = cell.push(v(0.0)).get();
		IntStream.range(0, DURATION_FRAMES).forEach(i -> {
			push.run();
			if ((i + 1) % 1000 == 0) System.out.println("SineWaveCellTest: " + (i + 1) + " iterations");
		});

		System.out.println("SineWaveCellTest: Writing WAV...");
		output.write().get().run();
		System.out.println("SineWaveCellTest: Done");
	}

	protected Gene<Scalar> identityGene() {
		return new Gene<>() {
			@Override public Factor<Scalar> getFactor(int index) { return new IdentityFactor<>(); }
			@Override public int length() { return 1; }
		};
	}

	protected void loggingCellPair(Cell<Scalar> input) {
		List<Cell<Scalar>> cells = new ArrayList<>();
		cells.add(loggingCell());

		MultiCell<Scalar> m = new MultiCell<>(cells, identityGene());
		m.setName("LoggingMultiCell");
		new CellPair<>(input, m, null, new IdentityFactor<>());
	}

	@Test
	public void withCellPair() {
		SineWaveCell cell = cell();
		loggingCellPair(cell);

		Runnable push = cell.push(null).get();
		IntStream.range(0, 100).forEach(i -> push.run());
	}

	@Test
	public void withBasicDelayCell() {
		BasicDelayCell delay = new BasicDelayCell(1);
		delay.setReceptor(loggingReceptor());

		SineWaveCell cell = cell();
		cell.setReceptor(delay);

		Runnable push = cell.push(null).get();
		Runnable tick = delay.tick().get();

		IntStream.range(0, 200).forEach(i -> {
			push.run();
			tick.run();
		});
	}
}