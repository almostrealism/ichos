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
import org.almostrealism.audio.data.ParameterFunction;
import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.audio.tone.Scale;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.computations.RootDelegateSegmentsAdd;
import org.almostrealism.hardware.KernelizedEvaluable;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.hardware.PassThroughProducer;
import org.almostrealism.heredity.Gene;
import org.almostrealism.heredity.SimpleChromosome;
import org.almostrealism.heredity.SimpleGene;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleToIntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PatternLayerManager implements CodeFeatures {
	public static final boolean enableVolume = false;

	private int channel;
	private double duration;
	private double scale;
	private int chordDepth;
	private boolean melodic;
	private boolean applyNoteDuration;

	private Supplier<List<PatternFactoryChoice>> percChoices;
	private Supplier<List<PatternFactoryChoice>> melodicChoices;
	private SimpleChromosome chromosome;
	private ParameterFunction factorySelection;

	private List<PatternLayer> roots;
	private List<ParameterSet> layerParams;

	private PackedCollection volume;
	private PackedCollection destination;
	private RootDelegateSegmentsAdd sum;
	private OperationList runSum;

	public PatternLayerManager(List<PatternFactoryChoice> choices, SimpleChromosome chromosome, int channel, double measures,
							   boolean melodic, PackedCollection destination) {
		this(PatternFactoryChoice.choices(choices, false), PatternFactoryChoice.choices(choices, true),
				chromosome, channel, measures, melodic, destination);
	}

	public PatternLayerManager(Supplier<List<PatternFactoryChoice>> percChoices, Supplier<List<PatternFactoryChoice>> melodicChoices,
							   SimpleChromosome chromosome, int channel, double measures, boolean melodic, PackedCollection destination) {
		this.channel = channel;
		this.duration = measures;
		this.scale = 1.0;
		this.chordDepth = 1;
		setMelodic(melodic);

		this.percChoices = percChoices;
		this.melodicChoices = melodicChoices;
		this.chromosome = chromosome;
		this.roots = new ArrayList<>();
		this.layerParams = new ArrayList<>();
		if (destination != null) init(destination);
	}

	public void init(PackedCollection destination) {
		factorySelection = ParameterFunction.random();

		volume = new PackedCollection(1);
		volume.setMem(0, 0.1);

		updateDestination(destination);

		KernelizedEvaluable<PackedCollection<?>> scale = _multiply(
				new PassThroughProducer<>(1, 0), new PassThroughProducer<>(1, 1, -1)).get();

		OperationList generate = new OperationList("PatternLayerManager Sum");
		generate.add(() -> sum.get());

		if (enableVolume) {
			// TODO  This creates problems for multiple sum steps in ::sum
			// TODO  because volume adjustment will be applied multiple times
			// TODO  to earlier measures.
			generate.add(() -> () ->
					scale.kernelEvaluate(this.destination.traverse(1), this.destination.traverse(1), volume));
		}

		runSum = generate;
	}

	public void updateDestination(PackedCollection destination) {
		this.destination = destination;
		this.sum = new RootDelegateSegmentsAdd<>(512, this.destination.traverse(1));
	}

	public List<PatternFactoryChoice> getChoices() {
		return melodic ? melodicChoices.get() : percChoices.get();
	}

	public int getChannel() { return channel; }
	public void setChannel(int channel) { this.channel = channel; }

	public void setDuration(double measures) { duration = measures; }
	public double getDuration() { return duration; }

	public int getChordDepth() { return chordDepth; }
	public void setChordDepth(int chordDepth) { this.chordDepth = chordDepth; }

	public void setMelodic(boolean melodic) {
		this.melodic = melodic;
		this.applyNoteDuration = melodic;
	}

	public boolean isMelodic() { return melodic; }

	public PatternLayerSeeds getSeeds(ParameterSet params) {
		List<PatternLayerSeeds> options = getChoices().stream()
				.filter(PatternFactoryChoice::isSeed)
				.map(choice -> choice.seeds(params))
				.collect(Collectors.toList());

		if (options.isEmpty()) return null;

		double c = factorySelection.apply(params);
		if (c < 0) c = c + 1.0;
		return options.get((int) (options.size() * c));
	}

	public List<PatternElement> getTailElements() {
		return roots.stream()
				.map(PatternLayer::getTail)
				.map(PatternLayer::getElements)
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}

	public List<PatternElement> getAllElements(double start, double end) {
		return roots.stream()
				.map(l -> l.getAllElements(start, end))
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}

	public Settings getSettings() {
		Settings settings = new Settings();
		settings.setChannel(channel);
		settings.setDuration(duration);
		settings.setMelodic(melodic);
		settings.setFactorySelection(factorySelection);
		settings.getLayers().addAll(layerParams);
		return settings;
	}

	public void setSettings(Settings settings) {
		channel = settings.getChannel();
		duration = settings.getDuration();
		chordDepth = settings.getChordDepth();
		melodic = settings.isMelodic();
		factorySelection = settings.getFactorySelection();

		clear(true);
		settings.getLayers().forEach(this::addLayer);
	}

	protected void decrement() { scale *= 2; }
	protected void increment() {
		scale /= 2;
	}

	public int rootCount() { return roots.size(); }

	public int depth() {
		if (rootCount() <= 0) return 0;
		return roots.stream()
				.map(PatternLayer::depth)
				.max(Integer::compareTo).orElse(0);
	}

	public int getLayerCount() {
		return chromosome.length();
	}

	public void setLayerCount(int count) {
		if (count < 0) throw new IllegalArgumentException(count + " is not a valid number of layers");
		if (count == getLayerCount()) return;

		if (getLayerCount() < count) {
			while (getLayerCount() < count) addLayer(new ParameterSet());
		} else {
			while (getLayerCount() > count) removeLayer(true);
		}
	}

	public void addLayer(ParameterSet params) {
		SimpleGene g = chromosome.addGene();
		g.set(0, params.getX());
		g.set(1, params.getY());
		g.set(2, params.getZ());
		layer(params);
	}

	public void layer(Gene<PackedCollection<?>> gene) {
		ParameterSet params = new ParameterSet();
		params.setX(gene.valueAt(0).getResultant(c(1.0)).get().evaluate().toDouble(0));
		params.setY(gene.valueAt(1).getResultant(c(1.0)).get().evaluate().toDouble(0));
		params.setZ(gene.valueAt(2).getResultant(c(1.0)).get().evaluate().toDouble(0));
		layer(params);
	}

	protected void layer(ParameterSet params) {
		if (rootCount() <= 0) {
			PatternLayerSeeds seeds = getSeeds(params);
			seeds.generator(0, duration, chordDepth).forEach(roots::add);
			scale = seeds.getScale();
		} else {
			roots.forEach(layer -> {
				// TODO  Each layer should be processed separately, with lower probability for higher layers (?)
				PatternLayer next = choose(scale, params).apply(layer.getAllElements(0, 2 * duration), scale, chordDepth, params);
				next.trim(2 * duration);
				layer.getTail().setChild(next);
			});
		}

		layerParams.add(params);
		increment();
	}

	public void removeLayer(boolean removeGene) {
		if (removeGene) chromosome.removeGene(chromosome.length() - 1);
		layerParams.remove(layerParams.size() - 1);
		decrement();

		if (depth() <= 0) return;
		if (depth() <= 1) {
			roots.clear();
			return;
		}

		roots.forEach(layer -> layer.getLastParent().setChild(null));
	}

	public void replaceLayer(ParameterSet params) {
		removeLayer(true);
		addLayer(params);
	}

	public void clear(boolean removeGenes) {
		if (removeGenes) {
			while (getLayerCount() > 0) removeLayer(true);
		} else {
			while (depth() > 0) removeLayer(false);
		}
	}

	public void refresh() {
		clear(false);
		IntStream.range(0, chromosome.length()).forEach(i -> layer(chromosome.valueAt(i)));
	}

	public PatternFactoryChoice choose(double scale, ParameterSet params) {
		List<PatternFactoryChoice> options = getChoices().stream()
				.filter(c -> scale >= c.getMinScale())
				.filter(c -> scale <= c.getMaxScale())
				.collect(Collectors.toList());

		double c = factorySelection.apply(params);
		if (c < 0) c = c + 1.0;
		return options.get((int) (options.size() * c));
	}

	public void sum(DoubleToIntFunction offsetForPosition, int measures, Scale<?> scale) {
		List<PatternElement> elements = getAllElements(0.0, duration);
		if (elements.isEmpty()) {
			System.out.println("PatternLayerManager: No pattern elements");
			return;
		}

		destination.clear();

		int count = (int) (measures / duration);
		if (measures / duration - count > 0.0001) {
			System.out.println("PatternLayerManager: Pattern duration does not divide measures; there will be gaps");
		}

		IntStream.range(0, count).forEach(i -> {
			DoubleToIntFunction offset = pos -> offsetForPosition.applyAsInt(pos + i * duration);

			sum.getInput().clear();
			elements.stream()
					.map(e -> e.getNoteDestinations(offset, scale))
					.flatMap(List::stream)
					.forEach(sum.getInput()::add);

			if (sum.getInput().size() > sum.getMaxInputs()) {
				System.out.println("PatternLayerManager: Too many inputs (" + sum.getInput().size() + ") for sum");
				return;
			}

			if (sum.getInput().size() <= 0) {
				System.out.println("PatternLayerManager: No inputs for sum");
				return;
			}

			runSum.get().run();
		});
	}

	public static String layerHeader() {
		int count = 128;
		int divide = count / 4;

		StringBuffer buf = new StringBuffer();

		i: for (int i = 0; i < count; i++) {
			if (i % (divide / 2) == 0) {
				if (i % divide == 0) {
					buf.append("|");
				} else {
					buf.append(" ");
				}
			}

			buf.append(" ");
		}

		buf.append("|");
		return buf.toString();
	}

	public static String layerString(PatternLayer layer) {
		return layerString(layer.getElements());
	}

	public static String layerString(List<PatternElement> elements) {
		int count = 128;
		int divide = count / 8;
		double scale = 1.0 / count;

		StringBuffer buf = new StringBuffer();

		i: for (int i = 0; i < count; i++) {
			if (i % divide == 0) buf.append("|");
			for (PatternElement e : elements) {
				if (e.isPresent(i * scale, (i + 1) * scale)) {
					String s = e.getNote().getSource();
					if (s.contains("/")) s = s.substring(s.lastIndexOf("/") + 1, s.lastIndexOf("/") + 2);
					buf.append(s);
					continue i;
				}
			}
			buf.append(" ");
		}

		buf.append("|");
		return buf.toString();
	}

	public static class Settings {
		private int channel;
		private double duration;
		private int chordDepth;
		private boolean melodic;
		private ParameterFunction factorySelection;
		private List<ParameterSet> layers = new ArrayList<>();

		public int getChannel() { return channel; }
		public void setChannel(int channel) { this.channel = channel; }

		public double getDuration() { return duration; }
		public void setDuration(double duration) { this.duration = duration; }

		public int getChordDepth() { return chordDepth; }
		public void setChordDepth(int chordDepth) { this.chordDepth = chordDepth; }

		public boolean isMelodic() { return melodic; }
		public void setMelodic(boolean melodic) { this.melodic = melodic; }

		public ParameterFunction getFactorySelection() { return factorySelection; }
		public void setFactorySelection(ParameterFunction factorySelection) { this.factorySelection = factorySelection; }

		public List<ParameterSet> getLayers() { return layers; }
		public void setLayers(List<ParameterSet> layers) { this.layers = layers; }
	}
}
