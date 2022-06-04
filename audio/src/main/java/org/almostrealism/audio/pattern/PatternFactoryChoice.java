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

public class PatternFactoryChoice {
	private PatternFactoryNode node;
	private double weight;

	public PatternFactoryChoice() { this(null); }

	public PatternFactoryChoice(PatternFactoryNode node) {
		this(node, 1.0);
	}

	public PatternFactoryChoice(PatternFactoryNode node, double weight) {
		setNode(node);
		setWeight(weight);
	}

	public PatternFactoryNode getNode() {
		return node;
	}

	public void setNode(PatternFactoryNode node) {
		this.node = node;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
}
