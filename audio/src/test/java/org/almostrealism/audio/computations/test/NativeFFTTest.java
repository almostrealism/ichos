/*
 * Copyright 2021 Michael Murray
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.almostrealism.audio.computations.test;

import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.audio.computations.NativeFFT;
import org.almostrealism.hardware.RAM;
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
		fft.transform(((RAM) input.getMem()).getNativePointer(),
				((RAM) input.getMem()).getNativePointer(),
				((RAM) input.getMem()).getNativePointer(),
				input.getOffset(), input.getOffset(), input.getOffset(),
				2,2,2);
	}
}
