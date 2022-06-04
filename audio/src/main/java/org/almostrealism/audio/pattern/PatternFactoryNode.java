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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class PatternFactoryNode {
	private PatternElementFactory factory;
	private double selfWeight;
	private double minScale;
	private List<PatternFactoryChoice> choices;

	public PatternFactoryNode() {
		this(null);
	}

	public PatternFactoryNode(PatternElementFactory factory) {
		setFactory(factory);
		setSelfWeight(1.0);
		setChoices(new ArrayList<>());
	}

	public PatternElementFactory getFactory() {
		return factory;
	}

	public void setFactory(PatternElementFactory factory) {
		this.factory = factory;
	}

	public double getSelfWeight() {
		return selfWeight;
	}

	public void setSelfWeight(double selfWeight) {
		this.selfWeight = selfWeight;
	}

	public double getMinimumScale() {
		return minScale;
	}

	public void setMinimumScale(double minScale) {
		this.minScale = minScale;
	}

	public List<PatternFactoryChoice> getChoices() {
		return choices;
	}

	public void setChoices(List<PatternFactoryChoice> choices) {
		this.choices = choices;
	}

	public PatternFactoryLayer apply(List<PatternElement> elements, double scale, double x, double y, double z) {
		PatternFactoryLayer layer = new PatternFactoryLayer();
		layer.setNext(choose(scale, x, y, z));
		elements.forEach(e -> layer.getElements().addAll(apply(e, scale, x, y, z).getElements()));
		return layer;
	}

	public PatternFactoryLayer apply(PatternElement element, double scale, double x, double y, double z) {
		PatternFactoryLayer layer = new PatternFactoryLayer();
		IntStream.range(0, 2).forEach(i -> layer.getElements().add(factory.apply(element.getPosition(), scale, x, y, z)));
		return layer;
	}

	public PatternFactoryLayer initial(double position) {
		return new PatternFactoryLayer(this, List.of(new PatternElement(factory.getNotes().get((int) (Math.random() * factory.getNotes().size())), position)));
	}

	protected PatternFactoryNode choose(double scale, double x, double y, double z) {
		double total = (scale < minScale ? 0 : selfWeight) + choices.stream().mapToDouble(PatternFactoryChoice::getWeight).sum();
		double choice = Math.random() * total - (scale < minScale ? 0 : selfWeight);
		if (choice < 0) return this;

		for (int i = 0; i < choices.size() - 1; i++) {
			if (choice < IntStream.range(0, i + 1).mapToObj(choices::get).mapToDouble(PatternFactoryChoice::getWeight).sum()) {
				return choices.get(i).getNode();
			}
		}

		return choices.get(choices.size() - 1).getNode();
	}
}
