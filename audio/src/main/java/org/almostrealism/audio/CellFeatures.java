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

package org.almostrealism.audio;

import io.almostrealism.relation.Evaluable;
import io.almostrealism.relation.Producer;
import io.almostrealism.uml.Lifecycle;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.data.PolymorphicAudioData;
import org.almostrealism.audio.filter.AudioPassFilter;
import org.almostrealism.audio.sources.WavCell;
import org.almostrealism.graph.Cell;
import org.almostrealism.graph.FilteredCell;
import org.almostrealism.graph.Receptor;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.heredity.Factor;
import org.almostrealism.time.Temporal;
import org.almostrealism.time.TemporalFeatures;
import org.almostrealism.util.CodeFeatures;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface CellFeatures extends TemporalFeatures, CodeFeatures {
	default Receptor<Scalar> a(Supplier<Evaluable<? extends Scalar>> destination) {
		return protein -> a(2, destination, protein);
	}

	default CellList w(String path) throws IOException {
		return w(new File(path));
	}

	default CellList w(File f) throws IOException {
		CellList cells = new CellList();
		cells.addRoot(WavCell.load(f, 1.0, 0).apply(new PolymorphicAudioData()));
		return cells;
	}

	default CellList o(CellList cells, IntFunction<File> f) {
		CellList result = new CellList(cells);

		for (int i = 0; i < cells.size(); i++) {
			WaveOutput out = new WaveOutput(f.apply(i));
			cells.get(i).setReceptor(out);
			result.getFinals().add(out.write().get());
		}

		return result;
	}

	default CellList f(CellList cells, IntFunction<Factor<Scalar>> filter) {
		CellList layer = new CellList(cells);
		Iterator<Cell<Scalar>> itr = cells.iterator();

		for (int i = 0; itr.hasNext(); i++) {
			FilteredCell<Scalar> f = new FilteredCell<>(filter.apply(i));
			Cell<Scalar> c = itr.next();

			c.setReceptor(f);
			layer.add(f);
		}

		return layer;
	}

	default Supplier<Runnable> min(Temporal t, double minutes) {
		return sec(t, minutes * 60);
	}

	default Supplier<Runnable> sec(Temporal t, double seconds) {
		return iter(t, (int) (seconds * OutputLine.sampleRate));
	}

	default AudioPassFilter hp(double frequency, double resonance) {
		return hp(OutputLine.sampleRate, frequency, resonance);
	}

	default AudioPassFilter hp(int sampleRate, double frequency, double resonance) {
		return hp(sampleRate, v(frequency), v(resonance));
	}

	default AudioPassFilter hp(Producer<Scalar> frequency, Producer<Scalar> resonance) {
		return hp(OutputLine.sampleRate, frequency, resonance);
	}

	default AudioPassFilter hp(int sampleRate, Producer<Scalar> frequency, Producer<Scalar> resonance) {
		return new AudioPassFilter(sampleRate, frequency, resonance, true);
	}

	default AudioPassFilter lp(double frequency, double resonance) {
		return hp(OutputLine.sampleRate, frequency, resonance);
	}

	default AudioPassFilter lp(int sampleRate, double frequency, double resonance) {
		return hp(sampleRate, v(frequency), v(resonance));
	}

	default AudioPassFilter lp(Producer<Scalar> frequency, Producer<Scalar> resonance) {
		return lp(OutputLine.sampleRate, frequency, resonance);
	}

	default AudioPassFilter lp(int sampleRate, Producer<Scalar> frequency, Producer<Scalar> resonance) {
		return new AudioPassFilter(sampleRate, frequency, resonance, false);
	}
}
