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


package org.almostrealism.audio.sources.test;

import io.almostrealism.relation.Evaluable;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.CellList;
import org.almostrealism.audio.data.PolymorphicAudioData;
import org.almostrealism.audio.sources.WavCell;
import org.almostrealism.hardware.AcceleratedComputationOperation;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.heredity.ScaleFactor;
import org.almostrealism.util.TestFeatures;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class WavCellTest implements CellFeatures, TestFeatures {
	protected WavCell cell() throws IOException {
		return WavCell.load(new File("src/main/resources/test.wav"), 1000, null, v(10)).apply(new PolymorphicAudioData());
	}

	@Test
	public void push() throws IOException {
		WavCell cell = cell();
		cell.setReceptor(protein -> {
			Evaluable<? extends Scalar> ev = protein.get();
			return () -> () -> System.out.println(ev.evaluate());
		});

		OperationList l = (OperationList) cell.push(v(0.0));
		System.out.println(((AcceleratedComputationOperation) l.get(0).get()).getFunctionDefinition());

		Runnable r = l.get();
		IntStream.range(0, 100).forEach(i -> r.run());
	}

	@Test
	public void endless() {
		AtomicInteger total = new AtomicInteger();

		while (true) {
			dc(() -> {
				CellList cells = w("Library/Snare Perc DD.wav")
						.o(i -> new File("results/snare-clean-test.wav"));
				Runnable r = cells.sec(70).get();

				for (int i = 0; i < 80; i++) {
					r.run();
					int tot = total.incrementAndGet();

					if (tot % 10 == 0) {
						System.out.println(Instant.now() + " - Completed " + tot);
					}
				}
			});
		}
	}

	@Test
	public void clean() {
		int count = 8;

		CellList cells = w("Library/Snare Perc DD.wav")
				.o(i -> new File("results/snare-clean-test.wav"));

		// OperationList l = (OperationList) cells.sec(bpm(128).l(count));
		// AcceleratedComputationOperation op = (AcceleratedComputationOperation) l.get(1).get();
		// System.out.println(op.getFunctionDefinition());

		cells.sec(bpm(128).l(count)).get().run();
	}

	@Test
	public void repeatHat() {
		int count = 32;

		CellList cells = w(v(bpm(128).l(0.5)), v(bpm(128).l(4)),
						"Library/GT_HAT_31.wav")
				.o(i -> new File("results/hat-repeat-test.wav"));

		cells.sec(bpm(128).l(count)).get().run();
	}

	@Test
	public void repeatSnare() {
		int count = 32;

		CellList cells = w(v(bpm(128).l(1)), v(bpm(128).l(2)),
				"Library/Snare Perc DD.wav")
				.o(i -> new File("results/snare-repeat-test.wav"));

		cells.sec(bpm(128).l(count)).get().run();
	}

	@Test
	public void sequence() {
		int count = 32;

		CellList cells = silence().and(w(v(bpm(128).l(0.5)), v(bpm(128).l(1)),
							"Library/GT_HAT_31.wav"))
							.gr(bpm(128).l(count), count, i -> 1)
							.f(i -> i == 0 ? new ScaleFactor(0.5) : new ScaleFactor(0.1))
							.o(i -> new File("results/wav-cell-seq-test-" + i + ".wav"));

		cells.sec(bpm(128).l(count)).get().run();
	}
}
