/*
 * Copyright 2022 Michael Murray
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.almostrealism.audio.pattern;

import org.almostrealism.audio.data.ParameterFunction;
import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.audio.tone.KeyPosition;
import org.almostrealism.audio.tone.Scale;
import org.almostrealism.audio.tone.StaticScale;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.heredity.ConfigurableGenome;
import org.almostrealism.heredity.SimpleChromosome;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChordProgressionManager {
	public static final int MAX_SIZE = 64;

	private ConfigurableGenome genome;
	private SimpleChromosome chromosome;

	private Scale<?> key;
	private int chordDepth;
	private int size;
	private double duration;
	private List<ParameterFunction> regionLengthSelection;
	private List<ChordPositionFunction> chordSelection;

	private List<Region> regions;

	public ChordProgressionManager() {
		genome = new ConfigurableGenome();
	}

	public ChordProgressionManager(ConfigurableGenome genome, Scale<?> key) {
		this.genome = genome;
		setKey(key);
		setChordDepth(4);
		init();
	}

	private void init() {
		regionLengthSelection = IntStream.range(0, MAX_SIZE)
				.mapToObj(i -> ParameterFunction.random())
				.collect(Collectors.toUnmodifiableList());

		chordSelection = IntStream.range(0, MAX_SIZE)
				.mapToObj(i -> ChordPositionFunction.random())
				.collect(Collectors.toUnmodifiableList());

		chromosome = genome.addSimpleChromosome(3);
		chromosome.addGene();
	}

	protected ParameterSet getParams() {
		PackedCollection data = chromosome.getParameters(0);

		ParameterSet params = new ParameterSet();
		params.setX(data.toDouble(0));
		params.setY(data.toDouble(1));
		params.setZ(data.toDouble(2));
		return params;
	}

	protected double getRegionLength(int index) {
		List<Double> choices = new ArrayList<>();
		int last = 1;
		double ratio = duration / size;

		while (last < size) {
			choices.add(last * ratio);
			last *= 2;
		}

		double choice = regionLengthSelection.get(index).positive().apply(getParams());
		return choices.get((int) (choice * choices.size()));
	}

	protected Scale<?> getScale(int index, double position) {
		List<KeyPosition<?>> notes = new ArrayList<>();
		getKey().forEach(notes::add);

		List<Double> choices = chordSelection.get(index).applyAll(getParams(), position, duration, chordDepth);

		List<KeyPosition<?>> result = new ArrayList<>();
		for (int i = 0; result.size() < chordDepth && !notes.isEmpty(); i++) {
			result.add(notes.remove((int) (choices.get(i) * notes.size())));
		}

		return new StaticScale<>(result.toArray(new KeyPosition[0]));
	}

	public void refreshParameters() {
		regions = new ArrayList<>();
		double length = 0.0;

		while (length < duration) {
			double regionLength = getRegionLength(regions.size());

			Region region = new Region(length, regionLength, getScale(regions.size(), length));
			regions.add(region);
			length += regionLength;
		}
	}

	public ConfigurableGenome getGenome() { return genome; }

	public Settings getSettings() {
		return null; // TODO
	}

	public void setSettings(Settings settings) {
		// TODO
	}

	public Scale<?> getKey() { return key; }
	public void setKey(Scale<?> scale) { this.key = scale; }

	public int getChordDepth() { return chordDepth; }

	public void setChordDepth(int chordDepth) { this.chordDepth = chordDepth; }

	public int getSize() { return size; }
	public void setSize(int size) {
		if (size > MAX_SIZE) throw new IllegalArgumentException();
		this.size = size;
	}

	public double getDuration() { return duration; }
	public void setDuration(double duration) { this.duration = duration; }

	public List<ParameterFunction> getRegionLengthSelection() {
		return regionLengthSelection;
	}

	public void setRegionLengthSelection(List<ParameterFunction> regionLengthSelection) {
		this.regionLengthSelection = regionLengthSelection;
	}

	public Scale<?> forPosition(double position) {
		// System.out.println("ChordProgressionManager: Retrieving scale for position = " + position);
		while (position >= duration) position -= duration;
		for (Region region : regions) {
			if (region.contains(position)) {
				return region.getScale();
			}
		}

		System.out.println("WARN: Exhausted regions");
		return getKey();
	}

	public String getRegionString() {
		StringBuilder sb = new StringBuilder();
		for (Region region : regions) {
			sb.append("X");
			IntStream.range(1, (int) region.length).forEach(i -> sb.append("_"));
		}

		return sb.toString();
	}

	public class Region {
		private double start, length;
		private Scale<?> scale;

		public Region(double start, double length, Scale<?> scale) {
			this.start = start;
			this.length = length;
			this.scale = scale;
		}

		public Scale<?> getScale() {
			return scale;
		}

		public boolean contains(double position) {
			return position >= start && position < start + length;
		}
	}

	public static class Settings {
		// TODO  Store scale for the key
		private int size;
		private double duration;
		private List<ParameterFunction> regionLengthSelection;

		public Settings() { }

		public int getSize() { return size; }
		public void setSize(int size) { this.size = size; }

		public double getDuration() { return duration; }
		public void setDuration(double duration) { this.duration = duration; }

		public List<ParameterFunction> getRegionLengthSelection() {
			return regionLengthSelection;
		}

		public void setRegionLengthSelection(List<ParameterFunction> regionLengthSelection) {
			this.regionLengthSelection = regionLengthSelection;
		}
	}
}
