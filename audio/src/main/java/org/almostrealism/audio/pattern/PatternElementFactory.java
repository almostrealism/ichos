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
import java.util.function.DoubleUnaryOperator;

public class PatternElementFactory {
	private List<PatternNote> notes;
	private ParameterFunction regularitySelection;
	private ParameterFunction offsetSelection;
	private ParameterFunction noteSelection;

	public PatternElementFactory() {
		this(new PatternNote[0]);
	}

	public PatternElementFactory(PatternNote... notes) {
		setNotes(new ArrayList<>());
		getNotes().addAll(List.of(notes));
		initSelectionFunctions();
	}

	public void initSelectionFunctions() {
		regularitySelection = ParameterFunction.random();
		offsetSelection = ParameterFunction.random();
		noteSelection = ParameterFunction.random(100);
	}

	public List<PatternNote> getNotes() {
		return notes;
	}

	public void setNotes(List<PatternNote> notes) {
		this.notes = notes;
	}

	public ParameterFunction getRegularitySelection() {
		return regularitySelection;
	}

	public void setRegularitySelection(ParameterFunction regularitySelection) {
		this.regularitySelection = regularitySelection;
	}

	public ParameterFunction getOffsetSelection() {
		return offsetSelection;
	}

	public void setOffsetSelection(ParameterFunction offsetSelection) {
		this.offsetSelection = offsetSelection;
	}

	public ParameterFunction getNoteSelection() {
		return noteSelection;
	}

	public void setNoteSelection(ParameterFunction noteSelection) {
		this.noteSelection = noteSelection;
	}

	public Optional<PatternElement> apply(ElementParity parity, double position, double scale, ParameterSet params) {
		if (parity == ElementParity.LEFT) {
			position -= scale;
		} else if (parity == ElementParity.RIGHT) {
			position += scale;
		}

		double note = applyPositional(regularitySelection.apply(params),
						position + offsetSelection.apply(params), scale,
								v -> noteSelection.apply(new ParameterSet(0.0, 0.0, v)));
		if (note < 0.0) return Optional.empty();

		return Optional.of(new PatternElement(getNotes().get((int) (note * getNotes().size())), position));
	}

	private static double applyPositional(double selection, double position, double scale, DoubleUnaryOperator operator) {
		if (selection > 0.0) selection = Math.floor(3 * selection);
		if (selection < 0.0) selection = Math.ceil(3 * selection);

		while (position < 0.0) position = position + 1.0;
		while (position > 1.0) position = position - 1.0;

		double regularity = scale * Math.pow(2.0, selection);

		int i;
		for (i = 0; position > 0; i++) {
			position -= regularity;
		}

		if (i % 2 == 0) {
			return operator.applyAsDouble(position);
		} else {
			return -operator.applyAsDouble(position);
		}
	}
}
