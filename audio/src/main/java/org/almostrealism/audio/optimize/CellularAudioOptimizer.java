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

package org.almostrealism.audio.optimize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.IntToDoubleFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.almostrealism.audio.AudioScene;
import org.almostrealism.audio.WaveSet;
import org.almostrealism.audio.data.FileWaveDataProvider;
import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.audio.data.WaveData;
import org.almostrealism.audio.grains.GrainGenerationSettings;
import org.almostrealism.audio.grains.GranularSynthesizer;
import org.almostrealism.audio.health.AudioHealthComputation;
import org.almostrealism.audio.health.SilenceDurationHealthComputation;
import org.almostrealism.audio.health.StableDurationHealthComputation;
import org.almostrealism.algebra.Pair;
import org.almostrealism.audio.Cells;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.WaveOutput;
import org.almostrealism.audio.Waves;
import org.almostrealism.audio.pattern.PatternElementFactory;
import org.almostrealism.audio.pattern.PatternFactoryChoice;
import org.almostrealism.audio.pattern.PatternFactoryChoiceList;
import org.almostrealism.audio.pattern.PatternLayerManager;
import org.almostrealism.audio.pattern.PatternNote;
import org.almostrealism.audio.sequence.GridSequencer;
import org.almostrealism.audio.tone.DefaultKeyboardTuning;
import org.almostrealism.audio.tone.WesternChromatic;
import org.almostrealism.audio.tone.WesternScales;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.PackedCollectionHeap;
import org.almostrealism.graph.AdjustableDelayCell;
import org.almostrealism.heredity.Breeders;
import org.almostrealism.hardware.Hardware;
import org.almostrealism.hardware.cl.CLComputeContext;
import org.almostrealism.hardware.cl.HardwareOperator;
import org.almostrealism.hardware.jni.NativeComputeContext;
import org.almostrealism.heredity.ChromosomeFactory;
import org.almostrealism.heredity.DefaultGenomeBreeder;
import org.almostrealism.heredity.GenomeFromChromosomes;
import org.almostrealism.heredity.RandomChromosomeFactory;
import org.almostrealism.heredity.Genome;
import org.almostrealism.heredity.GenomeBreeder;
import org.almostrealism.heredity.ScaleFactor;
import org.almostrealism.optimize.PopulationOptimizer;

public class CellularAudioOptimizer extends AudioPopulationOptimizer<Cells> {
	public static final int verbosity = 0;
	public static final boolean enableSourcesJson = true;
	public static final boolean enableStems = false;

	public static String LIBRARY = "Library";
	public static String STEMS = "Stems";

	static {
		String env = System.getenv("AR_RINGS_LIBRARY");
		if (env != null) LIBRARY = env;

		String arg = System.getProperty("AR_RINGS_LIBRARY");
		if (arg != null) LIBRARY = arg;
		
		env = System.getenv("AR_RINGS_STEMS");
		if (env != null) STEMS = env;

		arg = System.getProperty("AR_RINGS_STEMS");
		if (arg != null) STEMS = arg;
	}

	private AudioScenePopulation<PackedCollection<?>> population;

	public CellularAudioOptimizer(AudioScene<?> scene,
								  Supplier<GenomeBreeder<PackedCollection<?>>> breeder, Supplier<Supplier<Genome<PackedCollection<?>>>> generator,
								  int totalCycles) {
		super(null, breeder, generator, "Population.xml", totalCycles);
		setChildrenFunction(
				children -> {
					if (population == null) {
						population = new AudioScenePopulation<>(scene, children);
						AudioHealthComputation hc = (AudioHealthComputation) getHealthComputation();
						population.init(population.getGenomes().get(0), hc.getMeasures(), hc.getOutput());
					} else {
						population.setGenomes(children);
					}

					return population;
				});
	}

	public static Supplier<Supplier<Genome<PackedCollection<?>>>> generator(AudioScene<?> scene) {
		return generator(scene, new GeneratorConfiguration(scene.getSourceCount()));
	}

	public static Supplier<Supplier<Genome<PackedCollection<?>>>> generator(AudioScene<?> scene, GeneratorConfiguration config) {
		int sources = scene.getSourceCount();
		int delayLayers = scene.getDelayLayerCount();

		Supplier<GenomeFromChromosomes> oldGenome = () -> {
			// Random genetic material generators
			ChromosomeFactory<PackedCollection<?>> generators = DefaultAudioGenome.generatorFactory(config.minChoice, config.maxChoice,
													config.offsetChoices, config.repeatChoices,
													config.repeatSpeedUpDurationMin, config.repeatSpeedUpDurationMax);   // GENERATORS
			RandomChromosomeFactory parameters = new RandomChromosomeFactory();   // PARAMETERS
			RandomChromosomeFactory volume = new RandomChromosomeFactory();       // VOLUME
			RandomChromosomeFactory filterUp = new RandomChromosomeFactory();     // MAIN FILTER UP
			RandomChromosomeFactory wetIn = new RandomChromosomeFactory();		  // WET IN
			RandomChromosomeFactory processors = new RandomChromosomeFactory();   // DELAY
			RandomChromosomeFactory transmission = new RandomChromosomeFactory(); // ROUTING
			RandomChromosomeFactory wetOut = new RandomChromosomeFactory();		  // WET OUT
			RandomChromosomeFactory filters = new RandomChromosomeFactory();      // FILTERS
			RandomChromosomeFactory masterFilterDown = new RandomChromosomeFactory(); // MASTER FILTER DOWN

			generators.setChromosomeSize(sources, 0); // GENERATORS

			parameters.setChromosomeSize(sources, 3);
			IntStream.range(0, sources).forEach(i -> {
				parameters.setRange(i, 0, new Pair(config.minX.applyAsDouble(i), config.maxX.applyAsDouble(i)));
				parameters.setRange(i, 1, new Pair(config.minY.applyAsDouble(i), config.maxY.applyAsDouble(i)));
				parameters.setRange(i, 2, new Pair(config.minZ.applyAsDouble(i), config.maxZ.applyAsDouble(i)));
			});

			volume.setChromosomeSize(sources, 6);     // VOLUME
			Pair periodicVolumeDurationRange = new Pair(
					DefaultAudioGenome.factorForPeriodicAdjustmentDuration(config.periodicVolumeDurationMin),
					DefaultAudioGenome.factorForPeriodicAdjustmentDuration(config.periodicVolumeDurationMax));
			Pair overallVolumeDurationRange = new Pair(
					DefaultAudioGenome.factorForPolyAdjustmentDuration(config.overallVolumeDurationMin),
					DefaultAudioGenome.factorForPolyAdjustmentDuration(config.overallVolumeDurationMax));
			Pair overallVolumeExponentRange = new Pair(
					DefaultAudioGenome.factorForPolyAdjustmentExponent(config.overallVolumeExponentMin),
					DefaultAudioGenome.factorForPolyAdjustmentExponent(config.overallVolumeExponentMax));
			Pair overallVolumeOffsetRange = new Pair(
					DefaultAudioGenome.factorForAdjustmentOffset(config.overallVolumeOffsetMin),
					DefaultAudioGenome.factorForAdjustmentOffset(config.overallVolumeOffsetMax));

			IntStream.range(0, sources).forEach(i -> {
				volume.setRange(i, 0, periodicVolumeDurationRange);
				volume.setRange(i, 1, overallVolumeDurationRange);
				volume.setRange(i, 2, overallVolumeExponentRange);
				volume.setRange(i, 3, new Pair(
						DefaultAudioGenome.factorForAdjustmentInitial(config.minVolume.applyAsDouble(i)),
						DefaultAudioGenome.factorForAdjustmentInitial(config.maxVolume.applyAsDouble(i))));
				volume.setRange(i, 4, new Pair(-1.0, -1.0));
				volume.setRange(i, 5, overallVolumeOffsetRange);
			});

			filterUp.setChromosomeSize(sources, 6); // MAIN FILTER UP
			Pair periodicFilterUpDurationRange = new Pair(
					DefaultAudioGenome.factorForPeriodicAdjustmentDuration(config.periodicFilterUpDurationMin),
					DefaultAudioGenome.factorForPeriodicAdjustmentDuration(config.periodicFilterUpDurationMax));
			Pair overallFilterUpDurationRange = new Pair(
					DefaultAudioGenome.factorForPolyAdjustmentDuration(config.overallFilterUpDurationMin),
					DefaultAudioGenome.factorForPolyAdjustmentDuration(config.overallFilterUpDurationMax));
			Pair overallFilterUpExponentRange = new Pair(
					DefaultAudioGenome.factorForPolyAdjustmentExponent(config.overallFilterUpExponentMin),
					DefaultAudioGenome.factorForPolyAdjustmentExponent(config.overallFilterUpExponentMax));
			Pair overallFilterUpOffsetRange = new Pair(
					DefaultAudioGenome.factorForAdjustmentOffset(config.overallFilterUpOffsetMin),
					DefaultAudioGenome.factorForAdjustmentOffset(config.overallFilterUpOffsetMax));
			IntStream.range(0, sources).forEach(i -> filterUp.setRange(i, 0, periodicFilterUpDurationRange));
			IntStream.range(0, sources).forEach(i -> filterUp.setRange(i, 1, overallFilterUpDurationRange));
			IntStream.range(0, sources).forEach(i -> filterUp.setRange(i, 2, overallFilterUpExponentRange));
			IntStream.range(0, sources).forEach(i -> filterUp.setRange(i, 3, new Pair(0.0, 0.0)));
			IntStream.range(0, sources).forEach(i -> filterUp.setRange(i, 4, new Pair(1.0, 1.0)));
			IntStream.range(0, sources).forEach(i -> filterUp.setRange(i, 5, overallFilterUpOffsetRange));

			wetIn.setChromosomeSize(sources, 6);		 // WET IN
			Pair periodicWetInDurationRange = new Pair(
					DefaultAudioGenome.factorForPeriodicAdjustmentDuration(config.periodicWetInDurationMin),
					DefaultAudioGenome.factorForPeriodicAdjustmentDuration(config.periodicWetInDurationMax));
			Pair overallWetInDurationRange = new Pair(
					DefaultAudioGenome.factorForPolyAdjustmentDuration(config.overallWetInDurationMin),
					DefaultAudioGenome.factorForPolyAdjustmentDuration(config.overallWetInDurationMax));
			Pair overallWetInExponentRange = new Pair(
					DefaultAudioGenome.factorForPolyAdjustmentExponent(config.overallWetInExponentMin),
					DefaultAudioGenome.factorForPolyAdjustmentExponent(config.overallWetInExponentMax));
			Pair overallWetInOffsetRange = new Pair(
					DefaultAudioGenome.factorForAdjustmentOffset(config.overallWetInOffsetMin),
					DefaultAudioGenome.factorForAdjustmentOffset(config.overallWetInOffsetMax));
			IntStream.range(0, sources).forEach(i -> wetIn.setRange(i, 0, periodicWetInDurationRange));
			IntStream.range(0, sources).forEach(i -> wetIn.setRange(i, 1, overallWetInDurationRange));
			IntStream.range(0, sources).forEach(i -> wetIn.setRange(i, 2, overallWetInExponentRange));
			IntStream.range(0, sources).forEach(i -> wetIn.setRange(i, 3, new Pair(0.0, 0.0)));
			IntStream.range(0, sources).forEach(i -> wetIn.setRange(i, 4, new Pair(1.0, 1.0)));
			IntStream.range(0, sources).forEach(i -> wetIn.setRange(i, 5, overallWetInOffsetRange));

			processors.setChromosomeSize(delayLayers, 7); // DELAY
			Pair delayRange = new Pair(DefaultAudioGenome.factorForDelay(config.minDelay),
									DefaultAudioGenome.factorForDelay(config.maxDelay));
			Pair periodicSpeedUpDurationRange = new Pair(
					DefaultAudioGenome.factorForSpeedUpDuration(config.periodicSpeedUpDurationMin),
					DefaultAudioGenome.factorForSpeedUpDuration(config.periodicSpeedUpDurationMax));
			Pair periodicSpeedUpPercentageRange = new Pair(
					DefaultAudioGenome.factorForSpeedUpPercentage(config.periodicSpeedUpPercentageMin),
					DefaultAudioGenome.factorForSpeedUpPercentage(config.periodicSpeedUpPercentageMax));
			Pair periodicSlowDownDurationRange = new Pair(
					DefaultAudioGenome.factorForSlowDownDuration(config.periodicSlowDownDurationMin),
					DefaultAudioGenome.factorForSlowDownDuration(config.periodicSlowDownDurationMax));
			Pair periodicSlowDownPercentageRange = new Pair(
					DefaultAudioGenome.factorForSlowDownPercentage(config.periodicSlowDownPercentageMin),
					DefaultAudioGenome.factorForSlowDownPercentage(config.periodicSlowDownPercentageMax));
			Pair overallSpeedUpDurationRange = new Pair(
					DefaultAudioGenome.factorForPolySpeedUpDuration(config.overallSpeedUpDurationMin),
					DefaultAudioGenome.factorForPolySpeedUpDuration(config.overallSpeedUpDurationMax));
			Pair overallSpeedUpExponentRange = new Pair(
					DefaultAudioGenome.factorForPolySpeedUpExponent(config.overallSpeedUpExponentMin),
					DefaultAudioGenome.factorForPolySpeedUpExponent(config.overallSpeedUpExponentMax));
			IntStream.range(0, delayLayers).forEach(i -> processors.setRange(i, 0, delayRange));
			IntStream.range(0, delayLayers).forEach(i -> processors.setRange(i, 1, periodicSpeedUpDurationRange));
			IntStream.range(0, delayLayers).forEach(i -> processors.setRange(i, 2, periodicSpeedUpPercentageRange));
			IntStream.range(0, delayLayers).forEach(i -> processors.setRange(i, 3, periodicSlowDownDurationRange));
			IntStream.range(0, delayLayers).forEach(i -> processors.setRange(i, 4, periodicSlowDownPercentageRange));
			IntStream.range(0, delayLayers).forEach(i -> processors.setRange(i, 5, overallSpeedUpDurationRange));
			IntStream.range(0, delayLayers).forEach(i -> processors.setRange(i, 6, overallSpeedUpExponentRange));

			transmission.setChromosomeSize(delayLayers, delayLayers);    // ROUTING
			Pair transmissionRange = new Pair(config.minTransmission, config.maxTransmission);
			IntStream.range(0, delayLayers).forEach(i -> IntStream.range(0, delayLayers)
					.forEach(j -> transmission.setRange(i, j, transmissionRange)));

			wetOut.setChromosomeSize(1, delayLayers);		 // WET OUT
			Pair wetOutRange = new Pair(config.minWetOut, config.maxWetOut);
			IntStream.range(0, delayLayers).forEach(i -> wetOut.setRange(0, i, wetOutRange));

			filters.setChromosomeSize(sources, 2);    // FILTERS
			Pair hpRange = new Pair(DefaultAudioGenome.factorForFilterFrequency(config.minHighPass),
					DefaultAudioGenome.factorForFilterFrequency(config.maxHighPass));
			Pair lpRange = new Pair(DefaultAudioGenome.factorForFilterFrequency(config.minLowPass),
					DefaultAudioGenome.factorForFilterFrequency(config.maxLowPass));
			IntStream.range(0, sources).forEach(i -> {
				filters.setRange(i, 0, hpRange);
				filters.setRange(i, 1, lpRange);
			});

			masterFilterDown.setChromosomeSize(sources, 6);     // VOLUME
			Pair periodicMasterFilterDownDurationRange = new Pair(
					DefaultAudioGenome.factorForPeriodicAdjustmentDuration(config.periodicMasterFilterDownDurationMin),
					DefaultAudioGenome.factorForPeriodicAdjustmentDuration(config.periodicMasterFilterDownDurationMax));
			Pair overallMasterFilterDownDurationRange = new Pair(
					DefaultAudioGenome.factorForPolyAdjustmentDuration(config.overallMasterFilterDownDurationMin),
					DefaultAudioGenome.factorForPolyAdjustmentDuration(config.overallMasterFilterDownDurationMax));
			Pair overallMasterFilterDownExponentRange = new Pair(
					DefaultAudioGenome.factorForPolyAdjustmentExponent(config.overallMasterFilterDownExponentMin),
					DefaultAudioGenome.factorForPolyAdjustmentExponent(config.overallMasterFilterDownExponentMax));
			Pair overallMasterFilterDownInitialRange = new Pair(
					DefaultAudioGenome.factorForAdjustmentInitial(1.0),
					DefaultAudioGenome.factorForAdjustmentInitial(1.0));
			Pair overallMasterFilterDownOffsetRange = new Pair(
					DefaultAudioGenome.factorForAdjustmentOffset(config.overallMasterFilterDownOffsetMin),
					DefaultAudioGenome.factorForAdjustmentOffset(config.overallMasterFilterDownOffsetMax));
			IntStream.range(0, sources).forEach(i -> masterFilterDown.setRange(i, 0, periodicMasterFilterDownDurationRange));
			IntStream.range(0, sources).forEach(i -> masterFilterDown.setRange(i, 1, overallMasterFilterDownDurationRange));
			IntStream.range(0, sources).forEach(i -> masterFilterDown.setRange(i, 2, overallMasterFilterDownExponentRange));
			IntStream.range(0, sources).forEach(i -> masterFilterDown.setRange(i, 3, overallMasterFilterDownInitialRange));
			IntStream.range(0, sources).forEach(i -> masterFilterDown.setRange(i, 4, new Pair(-1.0, -1.0)));
			IntStream.range(0, sources).forEach(i -> masterFilterDown.setRange(i, 5, overallMasterFilterDownOffsetRange));

			return Genome.fromChromosomes(generators, parameters, volume, filterUp, wetIn, processors, transmission, wetOut, filters, masterFilterDown);
		};

		return () -> {
			GenomeFromChromosomes old = oldGenome.get();
			return () -> new AudioSceneGenome(scene.getGenome().random(), old.get());
		};
	}

	public static CellularAudioOptimizer build(AudioScene<?> scene, int cycles) {
		return build(generator(scene), scene, cycles);
	}

	public static CellularAudioOptimizer build(Supplier<Supplier<Genome<PackedCollection<?>>>> generator, AudioScene<?> scene, int cycles) {
		return new CellularAudioOptimizer(scene, scene::getBreeder, generator, cycles);
	}

	/**
	 * Build a {@link CellularAudioOptimizer} and initialize and run it.
	 *
	 * @see  CellularAudioOptimizer#build(AudioScene, int)
	 * @see  CellularAudioOptimizer#init
	 * @see  CellularAudioOptimizer#run()
	 */
	public static void main(String args[]) throws IOException {
		CLComputeContext.enableFastQueue = false;
		StableDurationHealthComputation.enableTimeout = true;
		AudioScene.enableMainFilterUp = true;
		AudioScene.enableEfxFilters = true;
		AudioScene.enableEfx = true;
		AudioScene.enableWetInAdjustment = true;
		AudioScene.enableMasterFilterDown = true;
		AudioScene.disableClean = false;
		AudioScene.enableSourcesOnly = false;
		SilenceDurationHealthComputation.enableSilenceCheck = false;
		AudioPopulationOptimizer.enableIsolatedContext = false;

		PopulationOptimizer.enableVerbose = verbosity > 0;
		Hardware.enableVerbose = verbosity > 0;
		WaveOutput.enableVerbose = verbosity > 1;
		PopulationOptimizer.enableDisplayGenomes = verbosity > 2;
		NativeComputeContext.enableVerbose = verbosity > 2;
		SilenceDurationHealthComputation.enableVerbose = verbosity > 2;
		HardwareOperator.enableLog = verbosity > 2;
		HardwareOperator.enableVerboseLog = verbosity > 3;

		// PopulationOptimizer.THREADS = verbosity < 1 ? 2 : 1;
		PopulationOptimizer.enableBreeding = verbosity < 3;

		AdjustableDelayCell.defaultPurgeFrequency = 1.0;
		// HealthCallable.setComputeRequirements(ComputeRequirement.C);
		// HealthCallable.setComputeRequirements(ComputeRequirement.PROFILING);
		// Hardware.getLocalHardware().setMaximumOperationDepth(7);

		WaveData.setCollectionHeap(() -> new PackedCollectionHeap(2000 * OutputLine.sampleRate), PackedCollectionHeap::destroy);

		AudioScene<?> scene = createScene();
		CellularAudioOptimizer opt = build(scene, PopulationOptimizer.enableBreeding ? 25 : 1);
		opt.init();
		opt.run();
	}

	public static AudioScene<?> createScene() throws IOException {
		double bpm = 120.0; // 116.0;
		int sourceCount = 5;
		AudioScene<?> scene = new AudioScene<>(null, bpm, sourceCount, 3, OutputLine.sampleRate);

		Set<Integer> choices = IntStream.range(0, sourceCount).mapToObj(i -> i).collect(Collectors.toSet());

		GranularSynthesizer synth = new GranularSynthesizer();
		synth.setGain(3.0);
		synth.addFile("Library/organ.wav");
		synth.addGrain(new GrainGenerationSettings());
		synth.addGrain(new GrainGenerationSettings());

		WaveSet synthNotes = new WaveSet(synth);
		synthNotes.setRoot(WesternChromatic.C3);
		synthNotes.setNotes(WesternScales.major(WesternChromatic.C3, 1));

		GridSequencer sequencer = new GridSequencer();
		sequencer.setStepCount(8);
		sequencer.initParamSequence();
		// sequencer.getSamples().add(synthNotes);
		sequencer.getSamples().add(new WaveSet(new FileWaveDataProvider("Library/MD_SNARE_09.wav")));
		sequencer.getSamples().add(new WaveSet(new FileWaveDataProvider("Library/MD_SNARE_11.wav")));
		sequencer.getSamples().add(new WaveSet(new FileWaveDataProvider("Library/Snare Perc DD.wav")));

		Waves seqWaves = new Waves("Sequencer", new WaveSet(sequencer));
		seqWaves.getChoices().setChoices(choices);

		Waves group = new Waves("Group");
		group.getChoices().setChoices(choices);
		group.getChildren().add(seqWaves);


		File sources = new File("sources.json");
		Waves waves = scene.getWaves();

		if (enableSourcesJson && sources.exists()) {
			waves = Waves.load(sources);
			scene.setWaves(waves);
		} else if (enableStems) {
			waves.addSplits(Arrays.asList(new File(STEMS).listFiles()), bpm, Math.pow(10, -6), choices, 1.0, 2.0, 4.0);
		} else {
			waves.getChildren().add(group);
			waves.getChoices().setChoices(choices);
		}

		scene.getPatternManager().getChoices().addAll(createChoices());
		scene.setTuning(new DefaultKeyboardTuning());
		scene.setTotalMeasures(64);
		scene.addSection(0, 32);
		scene.addSection(32, 32);
		// scene.addBreak(24);

		int channel = 0;

		PatternLayerManager layer = scene.getPatternManager().addPattern(channel++, 0.25, false);
		layer.setSeedBias(1.0);
		layer.addLayer(new ParameterSet());

		layer = scene.getPatternManager().addPattern(channel++, 0.5, false);
		layer.setSeedBias(0.5);
		layer.addLayer(new ParameterSet());
		layer.addLayer(new ParameterSet());

		layer = scene.getPatternManager().addPattern(channel++, 1.0, false);
		layer.setSeedBias(0.2);
		layer.addLayer(new ParameterSet());
		layer.addLayer(new ParameterSet());
		layer.addLayer(new ParameterSet());
		layer.addLayer(new ParameterSet());

		layer = scene.getPatternManager().addPattern(channel++, 4.0, true);
		layer.setSeedBias(0.4);
		layer.setChordDepth(3);
		layer.addLayer(new ParameterSet());
		layer.addLayer(new ParameterSet());
		layer.addLayer(new ParameterSet());

		layer = scene.getPatternManager().addPattern(channel++, 4.0, true);
		layer.setSeedBias(0.0);
		layer.addLayer(new ParameterSet());
		layer.addLayer(new ParameterSet());
		layer.addLayer(new ParameterSet());

		scene.saveSettings(new File("scene-settings.json"));
		return scene;
	}

	private static List<PatternFactoryChoice> createChoices() throws IOException {
		if (enableSourcesJson) {
			PatternFactoryChoiceList choices = new ObjectMapper()
					.readValue(new File("pattern-factory.json"), PatternFactoryChoiceList.class);
			return choices;
		} else {
			List<PatternFactoryChoice> choices = new ArrayList<>();

			PatternFactoryChoice kick = new PatternFactoryChoice(new PatternElementFactory("Kicks", new PatternNote("Kit/Kick.wav")));
			kick.setSeed(true);
			kick.setMinScale(0.25);
			choices.add(kick);

			PatternFactoryChoice clap = new PatternFactoryChoice(new PatternElementFactory("Clap/Snare", new PatternNote("Kit/Clap.wav")));
			clap.setMaxScale(0.5);
			choices.add(clap);

			PatternFactoryChoice toms = new PatternFactoryChoice(
					new PatternElementFactory("Toms", new PatternNote("Kit/Tom1.wav"),
							new PatternNote("Kit/Tom2.wav")));
			toms.setMaxScale(0.25);
			choices.add(toms);

			PatternFactoryChoice hats = new PatternFactoryChoice(new PatternElementFactory("Hats"));
			hats.setMaxScale(0.25);
			choices.add(hats);

			return choices;
		}
	}

	public static class GeneratorConfiguration {
		public IntToDoubleFunction minChoice, maxChoice;
		public double minChoiceValue, maxChoiceValue;

		public double repeatSpeedUpDurationMin, repeatSpeedUpDurationMax;

		public IntToDoubleFunction minX, maxX;
		public IntToDoubleFunction minY, maxY;
		public IntToDoubleFunction minZ, maxZ;
		public double minXValue, maxXValue;
		public double minYValue, maxYValue;
		public double minZValue, maxZValue;

		public IntToDoubleFunction minVolume, maxVolume;
		public double minVolumeValue, maxVolumeValue;
		public double periodicVolumeDurationMin, periodicVolumeDurationMax;
		public double overallVolumeDurationMin, overallVolumeDurationMax;
		public double overallVolumeExponentMin, overallVolumeExponentMax;
		public double overallVolumeOffsetMin, overallVolumeOffsetMax;

		public double periodicFilterUpDurationMin, periodicFilterUpDurationMax;
		public double overallFilterUpDurationMin, overallFilterUpDurationMax;
		public double overallFilterUpExponentMin, overallFilterUpExponentMax;
		public double overallFilterUpOffsetMin, overallFilterUpOffsetMax;

		public double minTransmission, maxTransmission;
		public double minDelay, maxDelay;

		public double periodicSpeedUpDurationMin, periodicSpeedUpDurationMax;
		public double periodicSpeedUpPercentageMin, periodicSpeedUpPercentageMax;
		public double periodicSlowDownDurationMin, periodicSlowDownDurationMax;
		public double periodicSlowDownPercentageMin, periodicSlowDownPercentageMax;
		public double overallSpeedUpDurationMin, overallSpeedUpDurationMax;
		public double overallSpeedUpExponentMin, overallSpeedUpExponentMax;

		public double periodicWetInDurationMin, periodicWetInDurationMax;
		public double overallWetInDurationMin, overallWetInDurationMax;
		public double overallWetInExponentMin, overallWetInExponentMax;
		public double overallWetInOffsetMin, overallWetInOffsetMax;

		public double minWetOut, maxWetOut;
		public double minHighPass, maxHighPass;
		public double minLowPass, maxLowPass;

		public double periodicMasterFilterDownDurationMin, periodicMasterFilterDownDurationMax;
		public double overallMasterFilterDownDurationMin, overallMasterFilterDownDurationMax;
		public double overallMasterFilterDownExponentMin, overallMasterFilterDownExponentMax;
		public double overallMasterFilterDownOffsetMin, overallMasterFilterDownOffsetMax;

		public double offsetChoices[];
		public double repeatChoices[];

		public GeneratorConfiguration() { this(1); }

		public GeneratorConfiguration(int scale) {
			double offset = 80;
			double duration = 5;

			minChoiceValue = 0.0;
			maxChoiceValue = 1.0;
			repeatSpeedUpDurationMin = 1;
			repeatSpeedUpDurationMax = 90;

			minVolumeValue = 0.5 / scale;
			maxVolumeValue = 1 / scale;
			periodicVolumeDurationMin = 0.5;
			periodicVolumeDurationMax = 180;
//			overallVolumeDurationMin = 60;
//			overallVolumeDurationMax = 240;
			overallVolumeDurationMin = duration - 1;
			overallVolumeDurationMax = duration + 5;
			overallVolumeExponentMin = 1;
			overallVolumeExponentMax = 1;
			overallVolumeOffsetMin = offset + 3;
			overallVolumeOffsetMax = offset + 5;

			periodicFilterUpDurationMin = 0.5;
			periodicFilterUpDurationMax = 180;
			overallFilterUpDurationMin = duration + 25;
			overallFilterUpDurationMax = duration + 175;
			overallFilterUpExponentMin = 0.5;
			overallFilterUpExponentMax = 3.5;
			overallFilterUpOffsetMin = offset;
			overallFilterUpOffsetMax = offset + 15; // + 5;

			minTransmission = 0.0;
			maxTransmission = 0.5;
			minDelay = 0.5;
			maxDelay = 60;

			periodicSpeedUpDurationMin = 0.5;
			periodicSpeedUpDurationMax = 180;
			periodicSpeedUpPercentageMin = 0.0;
			periodicSpeedUpPercentageMax = 10;

			periodicSlowDownDurationMin = 1;
			periodicSlowDownDurationMax = 180;
			periodicSlowDownPercentageMin = 0.0;
			periodicSlowDownPercentageMax = 0.9;

			overallSpeedUpDurationMin = 10;
			overallSpeedUpDurationMax = 180;
			overallSpeedUpExponentMin = 1;
			overallSpeedUpExponentMax = 1;

			periodicWetInDurationMin = 0.5;
			periodicWetInDurationMax = 180;
//			overallWetInDurationMin = 30;
//			overallWetInDurationMax = 120;
			overallWetInDurationMin = duration + 1;
			overallWetInDurationMax = duration + 15;
			overallWetInExponentMin = 0.5;
			overallWetInExponentMax = 2.5;
			overallWetInOffsetMin = offset;
			overallWetInOffsetMax = offset + 30; // + 10;

			minWetOut = 0.8;
			maxWetOut = 1.0;
			minHighPass = 0;
			maxHighPass = 20000;
			minLowPass = 0;
			maxLowPass = 20000;

			periodicMasterFilterDownDurationMin = 0.5;
			periodicMasterFilterDownDurationMax = 90;
			overallMasterFilterDownDurationMin = duration;
			overallMasterFilterDownDurationMax = duration + 25;
			overallMasterFilterDownExponentMin = 0.5;
			overallMasterFilterDownExponentMax = 3.5;
			overallMasterFilterDownOffsetMin = offset;
			overallMasterFilterDownOffsetMax = offset + 30; // 120;

			offsetChoices = IntStream.range(0, 7)
					.mapToDouble(i -> Math.pow(2, -i))
					.toArray();
			offsetChoices[0] = 0.0;

			repeatChoices = IntStream.range(0, 9)
					.map(i -> i - 2)
					.mapToDouble(i -> Math.pow(2, i))
//					.map(DefaultAudioGenome::factorForRepeat)
					.toArray();

			repeatChoices = new double[] { 16 };


			minChoice = i -> minChoiceValue;
			maxChoice = i -> maxChoiceValue;
			minX = i -> minXValue;
			maxX = i -> maxXValue;
			minY = i -> minYValue;
			maxY = i -> maxYValue;
			minZ = i -> minZValue;
			maxZ = i -> maxZValue;
			minVolume = i -> minVolumeValue;
			maxVolume = i -> maxVolumeValue;
		}
	}
}
