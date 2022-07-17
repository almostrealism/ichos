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

import org.almostrealism.CodeFeatures;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.ProducerWithOffset;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleToIntFunction;

public class PatternElement implements CodeFeatures {
	private PatternNote note;
	private double position;

	private boolean applyNoteDuration;
	private double noteDuration;

	private PatternDirection direction;
	private int repeatCount;
	private double repeatDuration;


	public PatternElement() {
		this(null, 0.0);
	}

	public PatternElement(PatternNote note, double position) {
		setNote(note);
		setPosition(position);
		setDirection(PatternDirection.FORWARD);
		setRepeatCount(1);
		setRepeatDuration(1);
	}

	public PatternNote getNote() {
		return note;
	}

	public void setNote(PatternNote note) {
		this.note = note;
	}

	public double getPosition() {
		return position;
	}

	public void setPosition(double position) {
		this.position = position;
	}

	public boolean isApplyNoteDuration() { return applyNoteDuration; }

	public void setApplyNoteDuration(boolean applyNoteDuration) { this.applyNoteDuration = applyNoteDuration; }

	public double getNoteDuration() { return noteDuration; }

	public void setNoteDuration(double noteDuration) { this.noteDuration = noteDuration; }

	public PatternDirection getDirection() {
		return direction;
	}

	public void setDirection(PatternDirection direction) {
		this.direction = direction;
	}

	public int getRepeatCount() {
		return repeatCount;
	}

	public void setRepeatCount(int repeatCount) {
		this.repeatCount = repeatCount;
	}

	public double getRepeatDuration() {
		return repeatDuration;
	}

	public void setRepeatDuration(double repeatDuration) {
		this.repeatDuration = repeatDuration;
	}

	public List<ProducerWithOffset<PackedCollection>> getNoteDestinations(DoubleToIntFunction offsetForPosition) {
		List<ProducerWithOffset<PackedCollection>> destinations = new ArrayList<>();

		for (int i = 0; i < repeatCount; i++) {
			destinations.add(new ProducerWithOffset<>(v(getNoteAudio()), offsetForPosition.applyAsInt(getPosition() + i * repeatDuration)));
		}

		return destinations;
	}

	public PackedCollection getNoteAudio() {
		if (isApplyNoteDuration()) {
			return getNote().getAudio(getNoteDuration());
		} else {
			return getNote().getAudio();
		}
	}

	public boolean isPresent(double start, double end) {
		for (int i = 0; i < repeatCount; i++) {
			double pos = getPosition() + i * repeatDuration;
			if (pos >= start && pos < end) return true;
		}

		return false;
	}
}
