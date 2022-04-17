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

package org.almostrealism.audio.sequence;

import io.almostrealism.relation.Evaluable;
import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.CellList;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.WaveOutput;
import org.almostrealism.audio.data.DynamicWaveDataProvider;
import org.almostrealism.audio.data.ParameterFunctionSequence;
import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.audio.data.ParameterizedWaveDataProviderFactory;
import org.almostrealism.audio.data.WaveData;
import org.almostrealism.audio.data.WaveDataProvider;
import org.almostrealism.graph.ReceptorCell;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.time.Frequency;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

public class GridSequencer implements ParameterizedWaveDataProviderFactory, CellFeatures {
	private Frequency bpm;
	private double stepSize;
	private int stepCount;
	private List<ParameterizedWaveDataProviderFactory> samples;

	private ParameterFunctionSequence sequence;

	public GridSequencer() {
		setBpm(120);
		setStepSize(1.0);
		setStepCount(16);
		setSamples(new ArrayList<>());
	}

	protected void initParamSequence() {
		sequence = ParameterFunctionSequence.random(getStepCount());
	}

	public double getBpm() { return bpm.asBPM(); }
	public void setBpm(double bpm) { this.bpm = bpm(bpm); }

	public double getStepSize() { return stepSize; }
	public void setStepSize(double stepSize) { this.stepSize = stepSize; }

	public int getStepCount() { return stepCount; }
	public void setStepCount(int stepCount) {
		this.stepCount = stepCount;
		initParamSequence();
	}

	public List<ParameterizedWaveDataProviderFactory> getSamples() { return samples; }
	public void setSamples(List<ParameterizedWaveDataProviderFactory> samples) { this.samples = samples; }

	public double getDuration() { return bpm.l(getStepCount() * getStepSize()); }

	@Override
	public int getCount() { return (int) (getDuration() * OutputLine.sampleRate); }

	@Override
	public WaveDataProvider create(Producer<Scalar> x, Producer<Scalar> y, Producer<Scalar> z) {
		ScalarBank export = new ScalarBank(getCount());
		WaveData destination = new WaveData(export, OutputLine.sampleRate);

		Evaluable<Scalar> evX = x.get();
		Evaluable<Scalar> evY = y.get();
		Evaluable<Scalar> evZ = z.get();

		WaveOutput output = new WaveOutput();
		CellList cells = silence();

		OperationList setup = new OperationList();

		for (ParameterizedWaveDataProviderFactory s : samples) {
			WaveDataProvider provider = s.create(x, y, z);
			setup.add(provider.setup());
			cells = cells.and(w(v(bpm.l(1)), provider.get()));
		}

		cells = cells
				.grid(getDuration(), getStepCount(),
						(IntFunction<Producer<Scalar>>) i -> () -> args -> {
							ParameterSet params = new ParameterSet(evX.evaluate().getValue(), evY.evaluate().getValue(), evZ.evaluate().getValue());
							Scalar s = new Scalar(sequence.apply(i).apply(params));
							// System.out.println("GridSequencer: Computed step - " + s.getValue());
							return s;
						})
				.sum().map(i -> new ReceptorCell<>(output));

		setup.add(cells.iter(getCount()));
		setup.add(output.export(export));
		return new DynamicWaveDataProvider("seq://" + UUID.randomUUID(), destination, setup);
	}
}