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

import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.CellList;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.WaveOutput;
import org.almostrealism.audio.data.ParameterFunctionSequence;
import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.audio.data.Parameterized;
import org.almostrealism.audio.data.WaveData;
import org.almostrealism.audio.data.WaveDataProvider;
import org.almostrealism.audio.data.WaveDataProviderAdapter;
import org.almostrealism.graph.ReceptorCell;
import org.almostrealism.time.Frequency;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class GridSequencer extends WaveDataProviderAdapter implements Parameterized, CellFeatures {
	private String key;
	private Frequency bpm;
	private double stepSize;
	private int stepCount;
	private List<WaveDataProvider> samples;

	private ParameterFunctionSequence sequence;
	private ParameterSet params;

	public GridSequencer() {
		key = "seq://" + UUID.randomUUID().toString();
		setBpm(bpm(120));
		setStepSize(1.0);
		setStepCount(16);
		setSamples(new ArrayList<>());
	}

	protected void initParamSequence() {
		sequence = ParameterFunctionSequence.random(getStepCount());
	}

	@Override
	public String getKey() { return key; }

	public void setKey(String key) { this.key = key; }

	public Frequency getBpm() { return bpm; }
	public void setBpm(Frequency bpm) {
		this.bpm = bpm;
		unload();
	}

	public double getStepSize() { return stepSize; }
	public void setStepSize(double stepSize) {
		this.stepSize = stepSize;
		unload();
	}

	public int getStepCount() { return stepCount; }
	public void setStepCount(int stepCount) {
		this.stepCount = stepCount;
		initParamSequence();
		unload();
	}

	public List<WaveDataProvider> getSamples() { return samples; }
	public void setSamples(List<WaveDataProvider> samples) { this.samples = samples; }

	@Override
	public double getDuration() { return bpm.l(getStepCount() * getStepSize()); }

	@Override
	public int getCount() { return (int) (getDuration() * OutputLine.sampleRate); }

	@Override
	public void setParameters(ParameterSet params) {
		this.params = params;
		samples.stream()
				.map(s -> s instanceof Parameterized ? (Parameterized) s : null)
				.filter(Objects::nonNull)
				.forEach(p -> p.setParameters(params));
		unload();
	}

	@Override
	protected WaveData load() {
		WaveOutput output = new WaveOutput();

		CellList cells = silence();

		for (WaveDataProvider s : samples) {
			cells = cells.and(w(v(bpm(128).l(1)), s.get()));
		}

		cells = cells
				.grid(getDuration(), getStepCount(), i -> sequence.apply(i).apply(params))
				.sum().map(i -> new ReceptorCell<>(output));

		cells.iter(getCount()).get().run();

		ScalarBank export = new ScalarBank(getCount());
		output.export(export).get().run();
		return new WaveData(export, OutputLine.sampleRate);
	}
}
