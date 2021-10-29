package org.almostrealism.audio.filter.test;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.CellFeatures;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public class DelayCellTest implements CellFeatures {
	@Test
	public void delay() throws IOException {
		Supplier<Runnable> r =
				w("src/test/resources/Snare Perc DD.wav")
						.d(i -> new Scalar(2.0))
						.om(i -> new File("delay-cell-test.wav"))
						.sec(6);
		r.get().run();
	}

	@Test
	public void filter() throws IOException {
		Supplier<Runnable> r =
				w("src/test/resources/Snare Perc DD.wav")
						.f(i -> hp(2000, 0.1))
						.d(i -> new Scalar(2.0))
						.om(i -> new File("filter-delay-cell-test.wav"))
						.sec(6);
		r.get().run();
	}
}
