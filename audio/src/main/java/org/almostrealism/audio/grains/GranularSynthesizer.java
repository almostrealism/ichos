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

package org.almostrealism.audio.grains;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.ScalarProducer;
import org.almostrealism.algebra.computations.ScalarSum;
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.WaveOutput;
import org.almostrealism.audio.data.FileWaveDataProvider;
import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.audio.data.Parameterized;
import org.almostrealism.audio.data.WaveData;
import org.almostrealism.audio.data.WaveDataProviderAdapter;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.TraversalPolicy;
import org.almostrealism.graph.ReceptorCell;
import org.almostrealism.hardware.Input;
import org.almostrealism.hardware.MemoryBank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GranularSynthesizer extends WaveDataProviderAdapter implements Parameterized, CellFeatures {
	public static double ampModWavelengthMin = 0.1;
	public static double ampModWavelengthMax = 10;

	private String key;
	private List<GrainSet> grains;
	private ParameterSet params;

	public GranularSynthesizer() {
		key = "synth://" + UUID.randomUUID().toString();
		grains = new ArrayList<>();
	}

	@JsonIgnore
	@Override
	public int getCount() {
		return 10 * OutputLine.sampleRate;
		// return WaveOutput.defaultTimelineFrames;
	}

	@JsonIgnore
	@Override
	public double getDuration() {
		return 10;
	}

	public List<GrainSet> getGrains() {
		return grains;
	}

	public void setGrains(List<GrainSet> grains) {
		this.grains = grains;
	}

	@Override
	public void setParameters(ParameterSet params) {
		this.params = params;
		unload();
	}

	@Override
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public GrainSet addFile(String file) {
		GrainSet g = new GrainSet(new FileWaveDataProvider(file));
		grains.add(g);
		return g;
	}

	public void addGrain(GrainGenerationSettings settings) {
		if (grains.isEmpty()) throw new UnsupportedOperationException();
		grains.get((int) (Math.random() * grains.size())).addGrain(settings);
	}

	@Override
	protected WaveData load() {
		List<ScalarBank> results = new ArrayList<>();

		int count = grains.stream().map(GrainSet::getGrains).mapToInt(List::size).sum();

		for (GrainSet grainSet : grains) {
			WaveData source = grainSet.getSource().get();

			TraversalPolicy grainShape = new TraversalPolicy(3);
			Producer<PackedCollection> g = v(PackedCollection.class, 1, -1);

			ScalarProducer pos = scalar(grainShape, g, 0).add(
							mod(scalar(grainShape, g, 2).multiply(
									v(Scalar.class, 0)), scalar(grainShape, g, 1)))
					.multiply(source.getSampleRate());
			Producer cursor = pair(pos, v(0.0));

			for (int n = 0; n < grainSet.getGrains().size(); n++) {
				Grain grain = grainSet.getGrain(n);
				GrainParameters gp = grainSet.getParams(n);

				WaveOutput sourceRec = new WaveOutput();
				w(source).map(i -> new ReceptorCell<>(sourceRec)).iter(source.getWave().getCount(), false).get().run();

				System.out.println("GranularSynthesizer: Processing grain...");

				ScalarBank raw = new ScalarBank(getCount());
				sourceRec.getData().valueAt(cursor).get().kernelEvaluate(raw, new MemoryBank[] { WaveOutput.timeline.getValue(), grain });

				ScalarBank result = new ScalarBank(getCount());
				double amp = gp.getAmp().apply(params);
				double phase = gp.getPhase().apply(params);
				double wavelength = ampModWavelengthMin + Math.abs(gp.getWavelength().apply(params)) * (ampModWavelengthMax - ampModWavelengthMin);
				ScalarProducer mod = sinw(scalarSubtract(v(Scalar.class, 0), v(phase)), v(wavelength), v(amp)).multiply(v(Scalar.class, 1));
				mod.get().kernelEvaluate(result, WaveOutput.timeline.getValue(), raw);

				results.add(result);
			}
		}

		ScalarBank output = new ScalarBank(getCount());
		ScalarProducer sum = scalarAdd(Input.generateArguments(2 * getCount(), 0, results.size())).divide(count);

		System.out.println("GranularSynthesizer: Summing grains...");
		sum.get().kernelEvaluate(output, results.stream().toArray(MemoryBank[]::new));
		System.out.println("GranularSynthesizer: Done");

		return new WaveData(output, OutputLine.sampleRate);
	}
}
