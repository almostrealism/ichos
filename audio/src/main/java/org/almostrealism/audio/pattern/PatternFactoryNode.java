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

import org.almostrealism.audio.data.ParameterFunction;
import org.almostrealism.audio.data.ParameterSet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Deprecated
public class PatternFactoryNode {
	private PatternElementFactory factory;
	private double selfWeight;
	private double minScale;
	private List<PatternFactoryChoice> choices;
	private ParameterFunction nodeSelection;

	public PatternFactoryNode() {
		this(null);
	}

	public PatternFactoryNode(PatternElementFactory factory) {
		setFactory(factory);
		setSelfWeight(1.0);
		setChoices(new ArrayList<>());
		initSelectionFunctions();
	}

	public void initSelectionFunctions() {
		nodeSelection = ParameterFunction.random();
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

	public ParameterFunction getNodeSelection() {
		return nodeSelection;
	}

	public void setNodeSelection(ParameterFunction nodeSelection) {
		this.nodeSelection = nodeSelection;
	}

	public PatternFactoryLayer apply(List<PatternElement> elements, double scale, ParameterSet params) {
		PatternFactoryLayer layer = new PatternFactoryLayer();
//		layer.setNode(choose(scale, params));
		elements.forEach(e -> layer.getElements().addAll(apply(e, scale, params).getElements()));
		return layer;
	}

	public PatternFactoryLayer apply(PatternElement element, double scale, ParameterSet params) {
		PatternFactoryLayer layer = new PatternFactoryLayer();
		PatternFactoryChoice node = null; // choose(scale, params);
		layer.setChoice(node);

		node.getFactory().apply(ElementParity.LEFT, element.getPosition(), scale, params).ifPresent(layer.getElements()::add);
		node.getFactory().apply(ElementParity.RIGHT, element.getPosition(), scale, params).ifPresent(layer.getElements()::add);
		return layer;
	}

	protected PatternElementFactory choose(double scale, ParameterSet params) {
		double total = (scale < minScale ? 0 : selfWeight) + choices.stream().mapToDouble(PatternFactoryChoice::getWeight).sum();
		double c = nodeSelection.apply(params);
		if (c < 0) c = c + 1.0;
		double choice = c * total - (scale < minScale ? 0 : selfWeight);
		if (choice < 0) return factory;

		for (int i = 0; i < choices.size() - 1; i++) {
			if (choice < IntStream.range(0, i + 1).mapToObj(choices::get).mapToDouble(PatternFactoryChoice::getWeight).sum()) {
				return choices.get(i).getFactory();
			}
		}

		return choices.get(choices.size() - 1).getFactory();
	}
}
