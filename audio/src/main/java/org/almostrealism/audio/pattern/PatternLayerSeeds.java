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

import java.util.List;
import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PatternLayerSeeds {
	/**
	 * When units is higher than 1, the seed process can easily produce
	 * more notes than the specified count. There are ways this could be
	 * handled directly via the positional function the determines note
	 * presence/absence, but trimming is a simpler solution. While the
	 * number of generated notes is above count, alternating notes are
	 * removed.
	 */
	public static boolean enableTrimming = true;

	private double position;
	private double scale;
	private int units;
	private double count;

	private PatternElementFactory factory;
	private ParameterSet params;

	public PatternLayerSeeds() {
		this(0, 1.0, 1, 1, null, null);
	}

	public PatternLayerSeeds(double position, double scale, int units, double count, PatternElementFactory factory, ParameterSet params) {
		this.position = position;
		this.scale = scale;
		this.units = units;
		this.count = count;
		this.factory = factory;
		this.params = params;
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

	public double getCount() {
		return count;
	}

	public void setCount(double count) {
		this.count = count;
	}

	public double getDuration() {
		return count * scale;
	}

	public Stream<PatternLayer> generator(double offset, double duration, double bias, int chordDepth) {
		List<PatternLayer> layers = IntStream.range(0, (int) (duration * count * units))
				.mapToObj(i ->
						factory.apply(null, position + offset + i * scale / units, scale / units, bias, chordDepth, params).orElse(null))
				.filter(Objects::nonNull)
				.map(List::of)
				.map(PatternLayer::new)
				.collect(Collectors.toList());

		if (enableTrimming) {
			while (layers.size() > (count * duration)) {
				layers = IntStream.range(0, layers.size())
						.filter(i -> i % 2 == 1)
						.mapToObj(layers::get)
						.collect(Collectors.toList());
			}
		}

		return layers.stream();
	}
}
