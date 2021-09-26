package org.almostrealism.audio.sources.test;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.CellList;
import org.almostrealism.util.TestFeatures;
import org.junit.Test;


public class SequenceTest implements CellFeatures, TestFeatures {
	@Test
	public void valueSequence() {
		Scalar value = new Scalar();
		CellList cells = seq(i -> v(i + 1), v(0.1), 2);
		cells.get(0).setReceptor(a(p(value)));
		// cells.sec(0.05).get().run();
		// assertEquals(1.0, value);
		cells.sec(0.07).get().run();
		assertEquals(2.0, value);
	}

	/*
	@Test
	public void mix() {
		silence().and(w("a.wav", "b.wav")) // silence and two samples
				.gr(bpm(128).l(4), 8,			  // 4 beats, divided into 8 segments
						(IntUnaryOperator) (i -> i % 2 == 0 ?
													i % 4 == 0 ? 1 : 2
													: 0))
				.o("out.wav").get().run();
	}
	 */
}
