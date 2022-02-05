/*
 * Copyright 2022 Michael Murray
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

package org.almostrealism.audio.test;

import org.almostrealism.algebra.ScalarBankHeap;
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.CellList;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.WavFile;
import org.almostrealism.audio.Waves;
import org.almostrealism.util.TestFeatures;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class WavesTest implements CellFeatures, TestFeatures {
	@Test
	public void splits() {
		WavFile.setHeap(() -> new ScalarBankHeap(600 * OutputLine.sampleRate), ScalarBankHeap::destroy);

		Waves waves = new Waves();
		waves.addSplits(
				List.of(new File("/Users/michael/AlmostRealism/ringsdesktop/Stems/001 Kicks 1.7_1.wav"),
						new File("/Users/michael/AlmostRealism/ringsdesktop/Stems/002 Percussion 1.6_1.wav")),
				bpm(116.0), 1.0);
		CellList cells = cells(1, i -> waves.getChoiceCell(v(0.5), v(0.0), v(bpm(116.0).l(1))));
		cells = cells.o(i -> new File("results/waves-splits-test.wav"));
		cells.sec(10).get().run();
	}
}
