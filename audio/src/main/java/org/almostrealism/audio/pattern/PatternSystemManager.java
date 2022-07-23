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

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleToIntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PatternSystemManager implements CodeFeatures {
	private List<PatternFactoryChoice> choices;
	private List<PatternLayerManager> patterns;

	private Supplier<PackedCollection> intermediateDestination;
	private List<ProducerWithOffset<PackedCollection>> patternOutputs;
	private PackedCollection volume;
	private RootDelegateSegmentsAdd<PackedCollection> sum;
	private Runnable runSum;

	public PatternSystemManager(List<PatternFactoryChoice> choices) {
		this.choices = choices;
		this.patterns = new ArrayList<>();
		this.patternOutputs = new ArrayList<>();
	}

	public void init(PackedCollection destination, Supplier<PackedCollection> intermediateDestination) {
		this.intermediateDestination = intermediateDestination;

		volume = new PackedCollection(1);
		volume.setMem(0, 1.0);

		sum = new RootDelegateSegmentsAdd<>(8, destination.traverse(1));

		KernelizedEvaluable<PackedCollection> scale = multiply(new TraversalPolicy(1),
				new PassThroughProducer<>(1, 0), new PassThroughProducer<>(1, 1, -1)).get();

		OperationList generate = new OperationList();
		generate.add(sum);
		generate.add(() -> () ->
				scale.kernelEvaluate(destination.traverse(1), destination.traverse(1), volume));
		runSum = generate.get();
	}

	public List<PatternFactoryChoice> getChoices() {
		return choices;
	}

	public void setTuning(KeyboardTuning tuning) {
		getChoices().forEach(c -> c.setTuning(tuning));
		patterns.forEach(l -> l.setTuning(tuning));
	}

	public PatternLayerManager addPattern(boolean melodic) {
		PackedCollection out = intermediateDestination.get();
		patternOutputs.add(new ProducerWithOffset<>(v(out), 0));

		// TODO  This is a problem, because if the user changes the choices,
		// TODO  they will expect those changes to take effect, but they will
		// TODO  not until the next pattern is created
		PatternLayerManager pattern = new PatternLayerManager(
				choices.stream()
					.filter(c -> c.getFactory().isMelodic() == melodic)
					.collect(Collectors.toList()), melodic,
				out);
		patterns.add(pattern);

		return pattern;
	}

	public void sum(DoubleToIntFunction offsetForPosition, Scale<?> scale) {
		patterns.forEach(p -> p.sum(offsetForPosition, scale));

		sum.getInput().clear();
		sum.getInput().addAll(patternOutputs);

		if (sum.getInput().size() > sum.getMaxInputs()) {
			System.out.println("PatternSystemManager: Too many patterns (" + sum.getInput().size() + ") for sum");
			return;
		}

		runSum.run();
	}
}
