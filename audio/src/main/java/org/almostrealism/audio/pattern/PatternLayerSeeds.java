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

import java.util.List;
import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PatternLayerSeeds {
	private double position;
	private double scale;
	private int units;
	private int count;

	private DoubleFunction<PatternNote> notes;

	public PatternLayerSeeds() {
		this(0, 1.0, 1, 1, null);
	}

	public PatternLayerSeeds(double position, double scale, int units, int count, DoubleFunction<PatternNote> notes) {
		this.position = position;
		this.scale = scale;
		this.units = units;
		this.count = count;
		this.notes = notes;
	}

	public double getPosition() {
		return position;
	}

	public void setPosition(double position) {
		this.position = position;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public int getUnits() {
		return units;
	}

	public void setUnits(int units) {
		this.units = units;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Stream<PatternLayer> generator(boolean applyNoteDuration) {
		return IntStream.range(0, count)
				.mapToObj(i -> {
					PatternNote note = notes.apply(position + i * scale / units);
					if (note == null) return null;
					PatternElement e = new PatternElement(note, position + i * scale / units);
					e.setApplyNoteDuration(applyNoteDuration);
					e.setNoteDuration(scale);
					return List.of(e);
				})
				.filter(Objects::nonNull)
				.map(PatternLayer::new);
	}
}
