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

import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.ProducerWithOffset;
import org.almostrealism.collect.computations.RootDelegateSegmentsAdd;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleToIntFunction;
import java.util.stream.Collectors;

public class PatternLayerManager {
	private double position;
	private double duration;
	private double scale;
	private PatternFactoryNode root;
	private List<PatternFactoryLayer> layers;
	private List<PatternElement> elements;

	private RootDelegateSegmentsAdd sum;
	private Runnable runSum;

	public PatternLayerManager(PatternFactoryNode root, PackedCollection destination) {
		this.position = 0.0;
		this.duration = 1.0;
		this.scale = 1.0;
		this.root = root;
		this.layers = new ArrayList<>();
		this.elements = new ArrayList<>();
		init(destination);
	}

	protected void init(PackedCollection destination) {
		addLayer(root.initial(position));
		sum = new RootDelegateSegmentsAdd<>(256, destination.traverse(1));
		runSum = sum.get();
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
		return layers.get(layers.size() - 1);
	}

	public int layerCount() { return layers.size(); }

	public void addLayer(ParameterSet params) {
		// TODO  Each layer should be processed separately, with lower probability for higher layers
		PatternFactoryLayer layer = lastLayer().getNode().apply(elements, scale, params);
		layer.trim(duration);
		addLayer(layer);
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
