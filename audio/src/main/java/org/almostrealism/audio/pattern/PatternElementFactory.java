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
import java.util.Optional;

public class PatternElementFactory {
	private List<PatternNote> notes;
	private ParameterFunction noteSelection;
	private ParameterFunction positionSelection;

	public PatternElementFactory() {
		this(new PatternNote[0]);
	}

	public PatternElementFactory(PatternNote... notes) {
		setNotes(new ArrayList<>());
		getNotes().addAll(List.of(notes));
		initSelectionFunctions();
	}

	public void initSelectionFunctions() {
		noteSelection = ParameterFunction.random();
		positionSelection = ParameterFunction.random();
	}

	public List<PatternNote> getNotes() {
		return notes;
	}

	public void setNotes(List<PatternNote> notes) {
		this.notes = notes;
	}

	public ParameterFunction getNoteSelection() {
		return noteSelection;
	}

	public void setNoteSelection(ParameterFunction noteSelection) {
		this.noteSelection = noteSelection;
	}

	public ParameterFunction getPositionSelection() {
		return positionSelection;
	}

	public void setPositionSelection(ParameterFunction positionSelection) {
		this.positionSelection = positionSelection;
	}

	public Optional<PatternElement> apply(double position, double scale, ParameterSet params) {
		double note = noteSelection.apply(params);
		if (note < 0.0) return Optional.empty();

		double pos = positionSelection.apply(params);
		return Optional.of(new PatternElement(getNotes().get((int) (note * getNotes().size())), position + (pos > 0.0 ? scale : -scale)));
	}
}
