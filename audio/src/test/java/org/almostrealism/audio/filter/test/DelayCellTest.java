package org.almostrealism.audio.filter.test;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.CellList;
import org.almostrealism.audio.sources.SineWaveCell;
import org.almostrealism.hardware.AcceleratedComputationOperation;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.heredity.CellularTemporalFactor;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public class DelayCellTest implements CellFeatures {
	@Test
	public void delay() {
		Supplier<Runnable> r =
				w("Library/Snare Perc DD.wav")
						.d(i -> v(2.0))
						.o(i -> new File("results/delay-cell-test.wav"))
						.sec(6);
		r.get().run();
	}

	@Test
	public void filter() throws IOException {
		Supplier<Runnable> r =
				w("src/test/resources/Snare Perc DD.wav")
						.f(i -> hp(2000, 0.1))
						.d(i -> v(2.0))
						.o(i -> new File("filter-delay-cell-test.wav"))
						.sec(6);
		r.get().run();
	}

	@Test
	public void adjust() {
		SineWaveCell generator = new SineWaveCell();
		generator.setPhase(0.5);
		generator.setNoteLength(0);
		generator.setFreq(3.424);
		generator.setAmplitude(1.0);

		Scalar v = new Scalar(0.0);

		CellularTemporalFactor<Scalar> adjustment = generator.toFactor(() -> v, this::a);

		CellList cells = w("Library/Snare Perc DD.wav");
		cells.addRequirement(adjustment);

		cells = cells
				.d(i -> v(2.6), i -> v(2.0).add(adjustment.getResultant(v(1.0))))
				.o(i -> new File("results/adjust-delay-cell-test.wav"));

		Supplier<Runnable> r = cells.sec(7.5);
		r.get().run();

		System.out.println(v);
	}

	@Test
	public void abortDelay() {
		OperationList.setAbortFlag(new Scalar(0.0));

		Supplier<Runnable> r =
				w("Library/Snare Perc DD.wav")
						.d(i -> v(2.0))
						.o(i -> new File("results/delay-cell-abort-test.wav"))
						.sec(120);
		Runnable op = r.get();

		Runnable abort = a(1, p((Scalar) OperationList.getAbortFlag()), v(1.0)).get();

		new Thread(() -> {
			try {
				Thread.sleep(40);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			abort.run(); }).start();

		op.run();
	}
}
