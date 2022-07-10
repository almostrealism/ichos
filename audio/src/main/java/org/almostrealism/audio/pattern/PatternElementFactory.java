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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PatternElementFactory {
	private String name;
	private List<PatternNote> notes;
	private ParameterizedPositionFunction noteSelection;
	private ParameterizedPositionFunction repeatSelection;

	public PatternElementFactory() {
		this(new PatternNote[0]);
	}

	public PatternElementFactory(PatternNote... notes) {
		this(null, notes);
	}

	public PatternElementFactory(String name, PatternNote... notes) {
		setName(name);
		setNotes(new ArrayList<>());
		getNotes().addAll(List.of(notes));
		initSelectionFunctions();
	}

	public void initSelectionFunctions() {
		noteSelection = ParameterizedPositionFunction.random();
		repeatSelection = ParameterizedPositionFunction.random();
	}

	public String getName() { return name; }

	public void setName(String name) { this.name = name; }

	public List<PatternNote> getNotes() {
		return notes;
	}

	public void setNotes(List<PatternNote> notes) {
		this.notes = notes;
	}

	public ParameterizedPositionFunction getNoteSelection() {
		return noteSelection;
	}

	public void setNoteSelection(ParameterizedPositionFunction noteSelection) {
		this.noteSelection = noteSelection;
	}

	public ParameterizedPositionFunction getRepeatSelection() {
		return repeatSelection;
	}

	public void setRepeatSelection(ParameterizedPositionFunction repeatSelection) {
		this.repeatSelection = repeatSelection;
	}

	public Optional<PatternElement> apply(ElementParity parity, double position, double scale, ParameterSet params) {
		if (parity == ElementParity.LEFT) {
			position -= scale;
		} else if (parity == ElementParity.RIGHT) {
			position += scale;
		}

		if (notes.isEmpty()) return Optional.empty();

		double note = noteSelection.apply(params, position, scale);
		if (note < 0.0) return Optional.empty();

		PatternElement element = new PatternElement(getNotes().get((int) (note * getNotes().size())), position);

		double r = repeatSelection.apply(params, position, scale);

		if (r <= 0) {
			element.setRepeatCount(1);
		} else {
			int c;
			for (c = 0; r < 1.0 & c < 4; c++) {
				r *= 2;
			}

			element.setRepeatCount(c);
		}

		element.setRepeatDuration(scale / 2.0);
		return Optional.of(element);
	}
}
