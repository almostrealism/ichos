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

public class PatternElementFactory {
	private List<PatternNote> notes;

	public PatternElementFactory() {
		this(new PatternNote[0]);
	}

	public PatternElementFactory(PatternNote... notes) {
		setNotes(new ArrayList<>());
		getNotes().addAll(List.of(notes));
	}

	public List<PatternNote> getNotes() {
		return notes;
	}

	public void setNotes(List<PatternNote> notes) {
		this.notes = notes;
	}

	public PatternElement apply(double position, double scale, double x, double y, double z) {
		return new PatternElement(getNotes().get((int) (Math.random() * getNotes().size())), position + (Math.random() > 0.5 ? scale : -scale));
	}
}
