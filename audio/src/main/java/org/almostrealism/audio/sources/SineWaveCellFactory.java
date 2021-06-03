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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SineWaveCellFactory implements CellFactory<Scalar, Scalar, Object>, CodeFeatures {
	private final int min;
	private final Scalar delta;
	private final List<Frequency> frequencies;

	private final int factorIndex;

	public SineWaveCellFactory(int minNoteLength, int maxNoteLength, Collection<Frequency> frequencies, int factorIndex) {
		this.min = minNoteLength;
		this.delta = new Scalar(maxNoteLength - minNoteLength);

		this.frequencies = new ArrayList<>();
		this.frequencies.addAll(frequencies);

		this.factorIndex = factorIndex;
	}

	@Override
	public Cell<Scalar> generateCell(Gene<Scalar> gene, Object v) {
		SineWaveCell cell = new SineWaveCell();
		cell.setFreq(frequencies.get((int) (Math.random() * frequencies.size())).asHertz());
		cell.setNoteLength((int) (min + gene.getFactor(factorIndex).getResultant(p(delta)).get().evaluate().getValue()));
		cell.setAmplitude(0.1);
		cell.setEnvelope(DefaultEnvelopeComputation::new);
		return cell;
	}
}
