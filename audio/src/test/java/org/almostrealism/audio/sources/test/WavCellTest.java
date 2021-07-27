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
import org.almostrealism.audio.data.PolymorphicAudioData;
import org.almostrealism.audio.sources.WavCell;
import org.almostrealism.hardware.AcceleratedComputationOperation;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.util.TestFeatures;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;

public class WavCellTest implements TestFeatures {
	protected WavCell cell() throws IOException {
		return WavCell.load(new File("src/main/resources/test.wav"), 1000, 10).apply(new PolymorphicAudioData());
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
}
