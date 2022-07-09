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
import java.util.Iterator;
import java.util.List;

public class PatternFactoryLayer {
	private PatternFactoryChoice choice;
	private List<PatternElement> elements;

	public PatternFactoryLayer() { this(null, new ArrayList<>()); }

	public PatternFactoryLayer(PatternFactoryChoice choice, List<PatternElement> elements) {
		this.choice = choice;
		this.elements = elements;
	}

	public PatternFactoryChoice getChoice() {
		return choice;
	}

	public void setChoice(PatternFactoryChoice node) {
		this.choice = node;
	}

	public List<PatternElement> getElements() {
		return elements;
	}

	public void setElements(List<PatternElement> elements) {
		this.elements = elements;
	}

	public void trim(double duration) {
		trim(0.0, duration);
	}

	public void trim(double start, double end) {
		Iterator<PatternElement> itr = elements.iterator();
		while (itr.hasNext()) {
			PatternElement e = itr.next();
			if (e.getPosition() < start || e.getPosition() >= end) itr.remove();
		}
	}
}
