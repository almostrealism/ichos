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
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.WaveOutput;
import org.almostrealism.audio.data.FileWaveDataProvider;
import org.almostrealism.audio.data.WaveData;
import org.almostrealism.audio.data.WaveDataProviderAdapter;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.TraversalPolicy;
import org.almostrealism.graph.ReceptorCell;
import org.almostrealism.hardware.MemoryBank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GranularSynthesizer extends WaveDataProviderAdapter implements CellFeatures {
	private String key;
	private List<GrainSet> grains;

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

		for (GrainSet grainSet : grains) {
			for (Grain grain : grainSet.getGrains()) {
				WaveData source = grainSet.getSource().get();

				WaveOutput sourceRec = new WaveOutput();
				w(source).map(i -> new ReceptorCell<>(sourceRec)).iter(source.getWave().getCount(), false).get().run();

				TraversalPolicy grainShape = new TraversalPolicy(3);
				Producer<PackedCollection> g = v(PackedCollection.class, 1, -1);

				ScalarProducer pos = scalar(grainShape, g, 0).add(
								mod(scalar(grainShape, g, 2).multiply(
										v(Scalar.class, 0)), scalar(grainShape, g, 1)))
						.multiply(source.getSampleRate());
				Producer cursor = pair(pos, v(0.0));

				ScalarBank result = new ScalarBank(getCount());
				System.out.println("GranularSynthesizer: Evaluating timeline kernel...");
				sourceRec.getData().valueAt(cursor).get().kernelEvaluate(result, new MemoryBank[]{WaveOutput.timeline.getValue(), grain});
				System.out.println("GranularSynthesizer: Timeline kernel evaluated");

				results.add(result);
			}
		}

		return new WaveData(results.get(0), OutputLine.sampleRate);
	}
}
