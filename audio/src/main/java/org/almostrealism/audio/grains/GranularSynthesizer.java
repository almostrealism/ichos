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
import io.almostrealism.relation.Evaluable;
import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.ScalarProducer;
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.WaveOutput;
import org.almostrealism.audio.data.DynamicWaveDataProvider;
import org.almostrealism.audio.data.FileWaveDataProvider;
import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.audio.data.ParameterizedWaveDataProviderFactory;
import org.almostrealism.audio.data.WaveData;
import org.almostrealism.audio.data.WaveDataProvider;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.TraversalPolicy;
import org.almostrealism.graph.ReceptorCell;
import org.almostrealism.hardware.Input;
import org.almostrealism.hardware.MemoryBank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GranularSynthesizer implements ParameterizedWaveDataProviderFactory, CellFeatures {
	public static double ampModWavelengthMin = 0.1;
	public static double ampModWavelengthMax = 10;

	private double gain;
	private List<GrainSet> grains;

	public GranularSynthesizer() {
		grains = new ArrayList<>();
	}

	@JsonIgnore
	@Override
	public int getCount() {
		return 10 * OutputLine.sampleRate;
		// return WaveOutput.defaultTimelineFrames;
	}

	@JsonIgnore
	public double getDuration() {
		return 10;
	}

	public double getGain() {
		return gain;
	}

	public void setGain(double gain) {
		this.gain = gain;
	}

	public List<GrainSet> getGrains() {
		return grains;
	}

	public void setGrains(List<GrainSet> grains) {
		this.grains = grains;
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
	public WaveDataProvider create(Producer<Scalar> x, Producer<Scalar> y, Producer<Scalar> z) {
		ScalarBank output = new ScalarBank(getCount());
		WaveData destination = new WaveData(output, OutputLine.sampleRate);

		Evaluable<Scalar> evX = x.get();
		Evaluable<Scalar> evY = y.get();
		Evaluable<Scalar> evZ = z.get();

		return new DynamicWaveDataProvider("synth://" + UUID.randomUUID(), destination, () -> () -> {
			ParameterSet params = new ParameterSet(evX.evaluate().getValue(), evY.evaluate().getValue(), evZ.evaluate().getValue());

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

			ScalarProducer sum = scalarAdd(Input.generateArguments(2 * getCount(), 0, results.size())).multiply(gain / count);

			System.out.println("GranularSynthesizer: Summing grains...");
			sum.get().kernelEvaluate(output, results.stream().toArray(MemoryBank[]::new));
			System.out.println("GranularSynthesizer: Done");
		});
	}
}