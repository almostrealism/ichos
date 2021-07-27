package org.almostrealism.audio.computations.test;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.audio.computations.DitherAndRemoveDcOffset;
import org.almostrealism.util.TestFeatures;
import org.junit.Test;

import java.util.stream.IntStream;


public class DitherAndRemoveDcOffsetTest implements TestFeatures {
	@Test
	public void ditherAndRemoveDcOffset() {
		ScalarBank bank = new ScalarBank(200);
		IntStream.range(0, 200).forEach(i -> bank.set(i, 100 * Math.random()));

		DitherAndRemoveDcOffset dither = new DitherAndRemoveDcOffset(200, v(400, 0), v(Scalar.class, 1));
		ScalarBank result = dither.get().evaluate(bank, new Scalar(1.0));
		assertNotEquals(0.0, result.get(20));
	}
}
