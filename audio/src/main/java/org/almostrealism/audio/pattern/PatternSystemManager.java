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

package org.almostrealism.audio.pattern;

import org.almostrealism.CodeFeatures;
import org.almostrealism.audio.tone.KeyboardTuning;
import org.almostrealism.audio.tone.Scale;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.ProducerWithOffset;
import org.almostrealism.collect.TraversalPolicy;
import org.almostrealism.collect.computations.RootDelegateSegmentsAdd;
import org.almostrealism.hardware.KernelizedEvaluable;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.hardware.PassThroughProducer;
import org.almostrealism.heredity.ConfigurableGenome;
import org.almostrealism.heredity.Genome;
import org.almostrealism.heredity.ParameterGenome;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleToIntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO  Excluded from the genome (manually configured):
// 	     1. The number of layers
// 	     2. Melodic/Percussive flag
// 	     3. The duration of each layer

public class PatternSystemManager implements CodeFeatures {
	private List<PatternFactoryChoice> choices;
	private List<PatternLayerManager> patterns;
	private ConfigurableGenome genome;

	private Supplier<PackedCollection> intermediateDestination;
	private List<ProducerWithOffset<PackedCollection>> patternOutputs;
	private PackedCollection volume;
	private PackedCollection destination;
	private RootDelegateSegmentsAdd<PackedCollection> sum;
	private OperationList runSum;

	public PatternSystemManager() {
		this(new ArrayList<>());
	}

	public PatternSystemManager(List<PatternFactoryChoice> choices) {
		this(choices, new ConfigurableGenome());
	}

	public PatternSystemManager(ConfigurableGenome genome) {
		this(new ArrayList<>(), genome);
	}

	public PatternSystemManager(List<PatternFactoryChoice> choices, ConfigurableGenome genome) {
		this.choices = choices;
		this.patterns = new ArrayList<>();
		this.genome = genome;

		this.patternOutputs = new ArrayList<>();
	}

	public void init(PackedCollection destination, Supplier<PackedCollection> intermediateDestination) {
		this.intermediateDestination = intermediateDestination;

		volume = new PackedCollection(1);
		volume.setMem(0, 1.0);

		updateDestination(destination, intermediateDestination);

		KernelizedEvaluable<PackedCollection<?>> scale = _multiply(
				new PassThroughProducer<>(1, 0), new PassThroughProducer<>(1, 1, -1)).get();

		OperationList generate = new OperationList("PatternSystemManager Sum");
		generate.add(() -> sum.get());
		generate.add(() -> () ->
				scale.kernelEvaluate(this.destination.traverse(1), this.destination.traverse(1), volume));
		runSum = generate;
	}

	public void updateDestination(PackedCollection destination, Supplier<PackedCollection> intermediateDestination) {
		this.destination = destination;
		this.intermediateDestination = intermediateDestination;
		this.sum = new RootDelegateSegmentsAdd<>(8, destination.traverse(1));
		IntStream.range(0, patterns.size()).forEach(i -> {
			PackedCollection out = intermediateDestination.get();
			patternOutputs.get(i).setProducer(v(out));
			patterns.get(i).updateDestination(out);
		});
	}

	public List<PatternFactoryChoice> getChoices() {
		return choices;
	}

	public List<PatternLayerManager> getPatterns() { return patterns; }

	public Settings getSettings() {
		Settings settings = new Settings();
		settings.getPatterns().addAll(patterns.stream().map(PatternLayerManager::getSettings).collect(Collectors.toList()));
		return settings;
	}

	public void setSettings(Settings settings) {
		patterns.clear();
		patternOutputs.clear();
		settings.getPatterns().forEach(s -> addPattern(s.getChannel(), s.getDuration(), s.isMelodic()).setSettings(s));
	}

	public void refreshParameters() {
		patterns.forEach(PatternLayerManager::refresh);
	}

	public void setTuning(KeyboardTuning tuning) {
		getChoices().forEach(c -> c.setTuning(tuning));
		patterns.forEach(l -> l.setTuning(tuning));
	}

	public PatternLayerManager addPattern(int channel, double measures, boolean melodic) {
		PackedCollection out = intermediateDestination.get();
		patternOutputs.add(new ProducerWithOffset<>(v(out), 0));

		// TODO  This is a problem, because if the user changes the choices,
		// TODO  they will expect those changes to take effect, but they will
		// TODO  not until the next pattern is created
		PatternLayerManager pattern = new PatternLayerManager(
				choices.stream()
					.filter(c -> c.getFactory().isMelodic() == melodic)
					.collect(Collectors.toList()), genome.addSimpleChromosome(3),
				channel, measures, melodic, out);
		patterns.add(pattern);

		return pattern;
	}

	public void clear() {
		patterns.clear();
		patternOutputs.clear();
	}

	public void sum(List<Integer> channels, DoubleToIntFunction offsetForPosition, int measures, Scale<?> scale) {
		List<Integer> patternsForChannel = IntStream.range(0, patterns.size())
				.filter(i -> channels == null || channels.contains(patterns.get(i).getChannel()))
				.boxed().collect(Collectors.toList());

		if (patternsForChannel.isEmpty()) {
			System.out.println("PatternSystemManager: No patterns");
			return;
		}

		sum.getInput().clear();
		patternsForChannel.forEach(i -> {
			patterns.get(i).sum(offsetForPosition, measures, scale);
			sum.getInput().add(patternOutputs.get(i));
		});

		if (sum.getInput().size() > sum.getMaxInputs()) {
			System.out.println("PatternSystemManager: Too many patterns (" + sum.getInput().size() + ") for sum");
			return;
		}

		runSum.get().run();
	}

	public static class Settings {
		private List<PatternLayerManager.Settings> patterns = new ArrayList<>();

		public List<PatternLayerManager.Settings> getPatterns() { return patterns; }
		public void setPatterns(List<PatternLayerManager.Settings> patterns) { this.patterns = patterns; }
	}
}
