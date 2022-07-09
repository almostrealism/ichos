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

import java.util.List;

public class PatternFactoryChoice {
	private PatternElementFactory factory;
	private double weight;
	private double minScale;
	private double maxScale;

	public PatternFactoryChoice() { this(null); }

	public PatternFactoryChoice(PatternElementFactory factory) {
		this(factory, 1.0);
	}

	public PatternFactoryChoice(PatternElementFactory factory, double weight) {
		this(factory, weight, 0.0, 1.0);
	}

	public PatternFactoryChoice(PatternElementFactory factory, double weight, double minScale, double maxScale) {
		setFactory(factory);
		setWeight(weight);
		setMinScale(minScale);
		setMaxScale(maxScale);
	}

	public PatternElementFactory getFactory() {
		return factory;
	}

	public void setFactory(PatternElementFactory factory) {
		this.factory = factory;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getMinScale() {
		return minScale;
	}

	public void setMinScale(double minScale) {
		this.minScale = minScale;
	}

	public double getMaxScale() {
		return maxScale;
	}

	public void setMaxScale(double maxScale) {
		this.maxScale = maxScale;
	}

	public PatternFactoryLayer initial(double position) {
		return new PatternFactoryLayer(this, List.of(new PatternElement(factory.getNotes().get((int) (Math.random() * factory.getNotes().size())), position)));
	}

	public PatternFactoryLayer apply(List<PatternElement> elements, double scale, ParameterSet params) {
		PatternFactoryLayer layer = new PatternFactoryLayer();
		layer.setChoice(this);
		elements.forEach(e -> layer.getElements().addAll(apply(e, scale, params).getElements()));
		return layer;
	}

	public PatternFactoryLayer apply(PatternElement element, double scale, ParameterSet params) {
		PatternFactoryLayer layer = new PatternFactoryLayer();
		layer.setChoice(this);

		getFactory().apply(ElementParity.LEFT, element.getPosition(), scale, params).ifPresent(layer.getElements()::add);
		getFactory().apply(ElementParity.RIGHT, element.getPosition(), scale, params).ifPresent(layer.getElements()::add);
		return layer;
	}
}
