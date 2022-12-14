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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.almostrealism.audio.data.ParameterFunction;
import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.audio.tone.KeyboardTuning;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PatternElementFactory {
	public static NoteDurationStrategy CHORD_STRATEGY = NoteDurationStrategy.FIXED;

	private String name;
	private List<PatternNote> notes;
	private boolean melodic;

	private ParameterizedPositionFunction noteSelection;
	private ParameterFunction noteLengthSelection;

	@Deprecated
	private ParameterizedPositionFunction scalePositionSelection;

	private ChordPositionFunction chordNoteSelection;

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
		noteLengthSelection = ParameterFunction.random();
		scalePositionSelection = ParameterizedPositionFunction.random();
		chordNoteSelection = ChordPositionFunction.random();
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

	public ParameterFunction getNoteLengthSelection() { return noteLengthSelection; }

	public void setNoteLengthSelection(ParameterFunction noteLengthSelection) { this.noteLengthSelection = noteLengthSelection; }

	@Deprecated
	public ParameterizedPositionFunction getScalePositionSelection() {
		return scalePositionSelection;
	}

	@Deprecated
	public void setScalePositionSelection(ParameterizedPositionFunction scalePositionSelection) {
		this.scalePositionSelection = scalePositionSelection;
	}

	public ChordPositionFunction getChordNoteSelection() {
		return chordNoteSelection;
	}
	public void setChordNoteSelection(ChordPositionFunction chordNoteSelection) {
		this.chordNoteSelection = chordNoteSelection;
	}

	public ParameterizedPositionFunction getRepeatSelection() {
		return repeatSelection;
	}
	public void setRepeatSelection(ParameterizedPositionFunction repeatSelection) {
		this.repeatSelection = repeatSelection;
	}

	public boolean isMelodic() { return melodic; }
	public void setMelodic(boolean melodic) { this.melodic = melodic; }

	public void setTuning(KeyboardTuning tuning) {
		notes.forEach(n -> n.setTuning(tuning));
	}

	@JsonIgnore
	public List<PatternNote> getValidNotes() {
		return notes.stream().filter(PatternNote::isValid).collect(Collectors.toList());
	}

	// TODO  This should take instruction for whether to apply note duration, relying just on isMelodic limits its use
	public Optional<PatternElement> apply(ElementParity parity, double position, double scale, double bias, int depth, boolean repeat, ParameterSet params) {
		if (parity == ElementParity.LEFT) {
			position -= scale;
		} else if (parity == ElementParity.RIGHT) {
			position += scale;
		}

		if (notes.isEmpty()) return Optional.empty();

		double note = noteSelection.apply(params, position, scale) + bias;
		while (note > 1) note -= 1;
		if (note < 0.0) return Optional.empty();

		List<PatternNote> notes = getValidNotes();

		PatternElement element = new PatternElement(notes.get((int) (note * notes.size())), position);
		element.setScalePosition(chordNoteSelection.applyAll(params, position, scale, depth));
		element.setNoteDurationSelection(noteLengthSelection.power(2.0, 3, -3).apply(params));
		element.setDurationStrategy(isMelodic() ?
				(depth > 1 ? CHORD_STRATEGY : NoteDurationStrategy.FIXED) :
					NoteDurationStrategy.NONE);

		double r = repeatSelection.apply(params, position, scale);

		if (!repeat || r <= 0) {
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
