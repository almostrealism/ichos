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
import org.almostrealism.audio.data.ParameterFunction;
import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.audio.tone.KeyboardTuning;
import org.almostrealism.audio.tone.Scale;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.TraversalPolicy;
import org.almostrealism.collect.computations.RootDelegateSegmentsAdd;
import org.almostrealism.hardware.KernelizedEvaluable;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.hardware.PassThroughProducer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleToIntFunction;
import java.util.stream.Collectors;

public class PatternLayerManager implements CodeFeatures {
	private double duration;
	private double scale;
	private boolean applyNoteDuration;

	private List<PatternFactoryChoice> choices;
	private ParameterFunction factorySelection;

	private List<PatternLayer> roots;

	private PackedCollection volume;
	private RootDelegateSegmentsAdd sum;
	private Runnable runSum;

	public PatternLayerManager(List<PatternFactoryChoice> choices, boolean applyNoteDuration, PackedCollection destination) {
		this.duration = 1.0;
		this.scale = 1.0;
		this.applyNoteDuration = applyNoteDuration;

		this.choices = choices;
		this.roots = new ArrayList<>();
		if (destination != null) init(destination);
	}

	public void init(PackedCollection destination) {
		factorySelection = ParameterFunction.random();

		volume = new PackedCollection(1);
		volume.setMem(0, 0.1);

		sum = new RootDelegateSegmentsAdd<>(512, destination.traverse(1));

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
	}

	public PatternLayerSeeds getSeeds(ParameterSet params) {
		return getChoices().stream()
				.filter(PatternFactoryChoice::isSeed)
				.map(choice -> choice.seeds(params))
				.findFirst().orElseThrow();
	}

	public List<PatternElement> getTailElements() {
		return roots.stream()
				.map(PatternLayer::getTail)
				.map(PatternLayer::getElements)
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}

	public List<PatternElement> getAllElements() {
		return roots.stream()
				.map(PatternLayer::getAllElements)
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}

	protected void decrement() { scale *= 2; }
	protected void increment() {
		scale /= 2;
	}

	public int rootCount() { return roots.size(); }
	public int depth() {
		if (rootCount() <= 0) return 0;
		return roots.stream()
				.map(PatternLayer::depth)
				.max(Integer::compareTo).orElse(0);
	}

	public void addLayer(ParameterSet params) {
		if (rootCount() <= 0) {
			PatternLayerSeeds seeds = getSeeds(params);
			seeds.generator(applyNoteDuration).forEach(roots::add);
			scale = seeds.getScale();
		} else {
			roots.forEach(layer -> {
				// TODO  Each layer should be processed separately, with lower probability for higher layers
				PatternLayer next = choose(scale, params).apply(layer.getAllElements(), scale, params);
				next.trim(2 * duration);
				layer.getTail().setChild(next);
			});
		}

		increment();
	}

	public void removeLayer() {
		decrement();

		if (depth() <= 0) return;
		if (depth() <= 1) {
			roots.clear();
			return;
		}

		roots.forEach(layer -> layer.getLastParent().setChild(null));
	}

	public void replaceLayer(ParameterSet params) {
		removeLayer();
		addLayer(params);
	}

	public void clear() {
		while (depth() > 0) removeLayer();
	}

	public PatternFactoryChoice choose(double scale, ParameterSet params) {
		List<PatternFactoryChoice> options = choices.stream()
				.filter(c -> scale >= c.getMinScale())
				.filter(c -> scale <= c.getMaxScale())
				.collect(Collectors.toList());

		double c = factorySelection.apply(params);
		if (c < 0) c = c + 1.0;
		return options.get((int) (options.size() * c));
	}

	public void sum(DoubleToIntFunction offsetForPosition, Scale<?> scale) {
		sum.getInput().clear();
		getAllElements().stream()
				.map(e -> e.getNoteDestinations(offsetForPosition, scale))
				.flatMap(List::stream)
				.forEach(sum.getInput()::add);

		if (sum.getInput().size() > sum.getMaxInputs()) {
			System.out.println("PatternLayerManager: Too many inputs (" + sum.getInput().size() + ") for sum");
			return;
		}

		runSum.run();
	}

	public static String layerHeader() {
		int count = 128;
		int divide = count / 4;

		StringBuffer buf = new StringBuffer();

		i: for (int i = 0; i < count; i++) {
			if (i % (divide / 2) == 0) {
				if (i % divide == 0) {
					buf.append("|");
				} else {
					buf.append(" ");
				}
			}

			buf.append(" ");
		}

		buf.append("|");
		return buf.toString();
	}

	public static String layerString(PatternLayer layer) {
		return layerString(layer.getElements());
	}

	public static String layerString(List<PatternElement> elements) {
		int count = 128;
		int divide = count / 8;
		double scale = 1.0 / count;

		StringBuffer buf = new StringBuffer();

		i: for (int i = 0; i < count; i++) {
			if (i % divide == 0) buf.append("|");
			for (PatternElement e : elements) {
				if (e.isPresent(i * scale, (i + 1) * scale)) {
					String s = e.getNote().getSource();
					if (s.contains("/")) s = s.substring(s.lastIndexOf("/") + 1, s.lastIndexOf("/") + 2);
					buf.append(s);
					continue i;
				}
			}
			buf.append(" ");
		}

		buf.append("|");
		return buf.toString();
	}
}
