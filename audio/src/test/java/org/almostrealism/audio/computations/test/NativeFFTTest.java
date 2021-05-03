package org.almostrealism.audio.computations.test;

import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.audio.computations.NativeFFT;
import org.junit.Test;

public class NativeFFTTest {
	static {
		System.loadLibrary("native");
	}

	@Test
	public void nativeFft() {
		NativeFFT fft = new NativeFFT();

		ScalarBank input = new ScalarBank(1);
		input.set(0, 5.0, 2.0);
		fft.transform(input.getMem(), input.getMem(), input.getMem(),
				input.getOffset(), input.getOffset(), input.getOffset(),
				2,2,2);
	}
}
