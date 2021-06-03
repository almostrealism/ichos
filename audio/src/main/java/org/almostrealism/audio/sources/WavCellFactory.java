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

package org.almostrealism.audio.sources;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.computations.DefaultEnvelopeComputation;
import org.almostrealism.graph.Cell;
import org.almostrealism.graph.CellFactory;
import org.almostrealism.heredity.Gene;
import org.almostrealism.time.Frequency;
import org.almostrealism.util.CodeFeatures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WavCellFactory implements CellFactory<Scalar, Scalar, Object>, CodeFeatures {
	private final List<File> samples;

	private final int factorIndex;

	public WavCellFactory(Collection<File> samples, int factorIndex) {
		this.samples = new ArrayList<>();
		this.samples.addAll(samples);

		this.factorIndex = factorIndex;
	}

	@Override
	public Cell<Scalar> generateCell(Gene<Scalar> gene, Object v) {
		try {
			return WavCell.load(samples.get((int) (Math.random() * samples.size())), 1.0, 0);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
