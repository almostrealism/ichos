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

package org.almostrealism.audio.filter.test;

import org.almostrealism.audio.CellFeatures;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public class AudioCellTest implements CellFeatures {
	@Test
	public void filter() throws IOException {
		Supplier<Runnable> r =
				w("Library/Snare Perc DD.wav")
						.f(i -> hp(2000, 0.1))
						.om(i -> new File("results/filter-cell-test.wav"))
						.sec(10);
		r.get().run();
	}

	@Test
	public void repeat() throws IOException {
		Supplier<Runnable> r =
				w(c(1.0), "Library/Snare Perc DD.wav")
						.om(i -> new File("results/repeat-cell-test.wav"))
						.sec(10);
		r.get().run();
	}
}
