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
	private double position;
	private double duration;
	private double scale;

	private List<PatternFactoryChoice> choices;
	private ParameterFunction factorySelection;

	private List<PatternFactoryLayer> layers;
	private List<PatternElement> elements;

	private PackedCollection volume;
	private RootDelegateSegmentsAdd sum;
	private Runnable runSum;

	public PatternLayerManager(List<PatternFactoryChoice> choices) {
		this(choices, null);
	}

	public PatternLayerManager(List<PatternFactoryChoice> choices, PackedCollection destination) {
		this.position = 0.0;
		this.duration = 1.0;
		this.scale = 1.0;
		this.choices = choices;
		this.layers = new ArrayList<>();
		this.elements = new ArrayList<>();
		if (destination != null) init(destination);
	}

	public void init(PackedCollection destination) {
		factorySelection = ParameterFunction.random();

		volume = new PackedCollection(1);
		volume.setMem(0, 0.1);

		sum = new RootDelegateSegmentsAdd<>(256, destination.traverse(1));

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

	protected void addLayer(PatternFactoryLayer layer) {
		layers.add(layer);
		elements.addAll(layer.getElements());
		increment();
	}

	protected void decrement() { scale *= 2; }
	protected void increment() {
		scale /= 2;
	}

	public PatternFactoryLayer lastLayer() {
		if (layers.isEmpty()) return null;
		return layers.get(layers.size() - 1);
	}

	public int layerCount() { return layers.size(); }

	public void addLayer(ParameterSet params) {
		if (layerCount() <= 0) {
			addLayer(choose(1.0, new ParameterSet(0.0, 0.0, 0.0)).initial(position));
		} else {
			// TODO  Each layer should be processed separately, with lower probability for higher layers
			PatternFactoryLayer layer = choose(scale, params).apply(elements, scale, params);
			layer.trim(duration);
			addLayer(layer);
		}
	}

	public void removeLayer() {
		decrement();
		lastLayer().getElements().forEach(elements::remove);
		layers.remove(layers.size() - 1);
	}

	public void replaceLayer(ParameterSet params) {
		removeLayer();
		addLayer(params);
	}

	public void clear() {
		while (layerCount() > 0) removeLayer();
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

	public void sum(DoubleToIntFunction offsetForPosition) {
		sum.getInput().clear();
		elements.stream()
				.map(e -> e.getNoteDestinations(offsetForPosition))
				.flatMap(List::stream)
				.forEach(sum.getInput()::add);
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

	public static String layerString(PatternFactoryLayer layer) {
		int count = 128;
		int divide = count / 8;
		double scale = 1.0 / count;

		StringBuffer buf = new StringBuffer();

		i: for (int i = 0; i < count; i++) {
			if (i % divide == 0) buf.append("|");
			for (PatternElement e : layer.getElements()) {
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
