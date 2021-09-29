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
import org.almostrealism.audio.filter.AdjustableDelayCell;
import org.almostrealism.audio.filter.AudioPassFilter;
import org.almostrealism.audio.sources.ValueSequenceCell;
import org.almostrealism.audio.sources.WavCell;
import org.almostrealism.graph.Cell;
import org.almostrealism.graph.CellPair;
import org.almostrealism.graph.FilteredCell;
import org.almostrealism.graph.MultiCell;
import org.almostrealism.graph.Receptor;
import org.almostrealism.graph.ReceptorCell;
import org.almostrealism.heredity.Factor;
import org.almostrealism.heredity.Gene;
import org.almostrealism.heredity.HeredityFeatures;
import org.almostrealism.time.Temporal;
import org.almostrealism.time.TemporalFeatures;
import org.almostrealism.util.CodeFeatures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface CellFeatures extends HeredityFeatures, TemporalFeatures, CodeFeatures {
	default Receptor<Scalar> a(Supplier<Evaluable<? extends Scalar>> destination) {
		return protein -> a(2, destination, protein);
	}

	default CellList silence() {
		throw new RuntimeException("Not implemented");
	}

	default CellList w(String... path) throws IOException {
		return w(Stream.of(path).map(File::new).toArray(File[]::new));
	}

	default CellList w(File... files) throws IOException {
		CellList cells = new CellList();
		Stream.of(files).map(f -> {
			try {
				return WavCell.load(f, 1.0, 0).apply(new PolymorphicAudioData());
			} catch (IOException e) {
				e.printStackTrace();
				return silence().get(0);
			}
		}).forEach(cells::addRoot);
		return cells;
	}

	default CellList o(int count, IntFunction<File> f) {
		CellList result = new CellList();

		for (int i = 0; i < count; i++) {
			WaveOutput out = new WaveOutput(f.apply(i));
			result.add(new ReceptorCell<>(out));
			result.getFinals().add(out.write().get());
		}

		return result;
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

	default CellList f(int count, IntFunction<Factor<Scalar>> filter) {
		CellList layer = new CellList();

		for (int i = 0; i < count; i++) {
			layer.add(new FilteredCell<>(filter.apply(i)));
		}

		return layer;
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

	default CellList d(int count, IntFunction<Scalar> d) {
		CellList result = new CellList();

		for (int i = 0; i < count; i++) {
			result.add(new AdjustableDelayCell(d.apply(i)));
		}

		return result;
	}

	default CellList d(CellList cells, IntFunction<Scalar> delay) {
		CellList layer = new CellList(cells);
		Iterator<Cell<Scalar>> itr = cells.iterator();

		for (int i = 0; itr.hasNext(); i++) {
			AdjustableDelayCell d = new AdjustableDelayCell(delay.apply(i));
			Cell<Scalar> c = itr.next();

			c.setReceptor(d);
			layer.add(d);
		}

		return layer;
	}

	default CellList m(CellList cells, List<Cell<Scalar>> adapter, List<Cell<Scalar>> destinations, IntFunction<Gene<Scalar>> transmission) {
		CellList result = m(cells, adapter::get, destinations, transmission);
		if (adapter instanceof CellList) {
			result.getFinals().addAll(((CellList) adapter).getFinals());
			((CellList) adapter).getRequirements().forEach(result::addRequirement);
		}
		return result;
	}

	default CellList m(CellList cells, IntFunction<Cell<Scalar>> adapter, List<Cell<Scalar>> destinations, IntFunction<Gene<Scalar>> transmission) {
		CellList result = m(cells, adapter, destinations::get, transmission);
		if (destinations instanceof CellList) {
			result.getFinals().addAll(((CellList) destinations).getFinals());
			((CellList) destinations).getRequirements().forEach(result::addRequirement);
		}
		return result;
	}

	default CellList m(CellList cells, IntFunction<Cell<Scalar>> adapter, IntFunction<Cell<Scalar>> destinations, IntFunction<Gene<Scalar>> transmission) {
		CellList layer = new CellList(cells);
		Iterator<Cell<Scalar>> itr = cells.iterator();

		for (AtomicInteger i = new AtomicInteger(); itr.hasNext(); i.incrementAndGet()) {
			Gene g = transmission.apply(i.get());
			Cell<Scalar> source = itr.next();

			List<Cell<Scalar>> dest = new ArrayList<>();
			IntStream.range(0, g.length()).mapToObj(j -> destinations.apply(j)).forEach(dest::add);

			layer.addRequirement(MultiCell.split(source, adapter.apply(i.get()), dest, g));
			layer.addAll(dest);
		}

		return layer;
	}

	default CellList seq(IntFunction<Producer<Scalar>> values, Producer<Scalar> duration, int steps) {
		CellList cells = new CellList();
		cells.addRoot(new ValueSequenceCell(values, duration, steps));
		return cells;
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
