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

package org.almostrealism.audio;

import io.almostrealism.relation.Producer;
import org.almostrealism.Ops;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.optimize.DefaultAudioGenome;
import org.almostrealism.graph.AdjustableDelayCell;
import org.almostrealism.graph.Receptor;
import org.almostrealism.graph.ReceptorCell;
import org.almostrealism.graph.temporal.WaveCell;
import org.almostrealism.heredity.ArrayListGene;
import org.almostrealism.heredity.Factor;
import org.almostrealism.heredity.Gene;
import org.almostrealism.heredity.TemporalFactor;
import org.almostrealism.space.ShadableSurface;
import org.almostrealism.space.Animation;
import io.almostrealism.uml.ModelEntity;
import org.almostrealism.time.Frequency;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@ModelEntity
public class AudioScene<T extends ShadableSurface> implements DesirablesProvider, CellFeatures {
	public static final int mixdownDuration = 140;

	public static final boolean enableRepeat = true;
	public static boolean enableMainFilterUp = true;
	public static boolean enableEfxFilters = true;
	public static boolean enableEfx = true;
	public static boolean enableWetInAdjustment = true;
	public static boolean enableMasterFilterDown = false;

	public static boolean enableMixdown = false;
	public static boolean enableSourcesOnly = false;
	public static boolean disableClean = false;

	public static Waves sourceOverride = null;

	private double bpm;
	private int sourceCount;
	private int delayLayerCount;

	private Animation<T> scene;
	private Waves sources;
	private DefaultAudioGenome genome;

	private List<Consumer<Frequency>> tempoListeners;

	public AudioScene(Animation<T> scene, int bpm, int sources, int delayLayers, int sampleRate) {
		this.bpm = bpm;
		this.sourceCount = sources;
		this.delayLayerCount = delayLayers;
		this.scene = scene;
		this.tempoListeners = new ArrayList<>();
		this.genome = new DefaultAudioGenome(sources, delayLayers, sampleRate);
		initSources();
	}

	protected void initSources() {
		sources = new Waves();
		IntStream.range(0, sourceCount).forEach(sources.getChoices().getChoices()::add);
	}

	public void setBPM(double bpm) {
		this.bpm = bpm;
		tempoListeners.forEach(l -> l.accept(Frequency.forBPM(bpm)));
	}

	public double getBPM() { return this.bpm; }

	public Animation<T> getScene() { return scene; }

	public DefaultAudioGenome getGenome() { return genome; }

	public void addTempoListener(Consumer<Frequency> listener) { this.tempoListeners.add(listener); }

	public void removeTempoListener(Consumer<Frequency> listener) { this.tempoListeners.remove(listener); }

	@Override
	public double getBeatPerMinute() { return bpm; }

	public int getSourceCount() { return sourceCount; }

	public int getDelayLayerCount() { return delayLayerCount;}

	public void setWaves(Waves waves) {this.sources = waves;}

	@Override
	public Waves getWaves() {return sources;}

	public Cells getCells(List<? extends Receptor<Scalar>> measures, Receptor<Scalar> output) {
		sources.setBpm(getBPM());

		BiFunction<Gene<Scalar>, Gene<Scalar>, IntFunction<WaveCell>> generator = (g, p) -> channel -> {
			Producer<Scalar> duration = g.valueAt(2).getResultant(v(bpm(getBeatPerMinute()).l(1)));

			Producer<Scalar> x = p.valueAt(0).getResultant(v(1.0));
			Producer<Scalar> y = p.valueAt(1).getResultant(v(1.0));
			Producer<Scalar> z = p.valueAt(2).getResultant(v(1.0));

			if (sourceOverride == null) {
				return getWaves().getChoiceCell(channel,
						g.valueAt(0).getResultant(Ops.ops().v(1.0)),
						x, y, z, g.valueAt(1).getResultant(duration),
						enableRepeat ? duration : null);
			} else {
				return sourceOverride.getChoiceCell(channel, g.valueAt(0).getResultant(Ops.ops().v(1.0)),
						v(0.0), v(0.0), v(0.0), v(0.0), null);
			}
		};

		Supplier<Runnable> genomeSetup = genome.setup();

		// Generators
		CellList cells = cells(genome.valueAt(DefaultAudioGenome.GENERATORS).length(),
				i -> generator.apply(genome.valueAt(DefaultAudioGenome.GENERATORS, i),
									genome.valueAt(DefaultAudioGenome.PARAMETERS, i))
										.apply(i));

		cells.addSetup(() -> genomeSetup);

		if (enableMainFilterUp) {
			// Apply dynamic high pass filters
			cells = cells.map(fc(i -> {
				TemporalFactor<Scalar> f = (TemporalFactor<Scalar>) genome.valueAt(DefaultAudioGenome.MAIN_FILTER_UP, i, 0);
				return hp(scalarsMultiply(v(20000), f.getResultant(v(1.0))), v(DefaultAudioGenome.defaultResonance));
			}));
		}

		cells = cells
				.addRequirements(genome.getTemporals().toArray(TemporalFactor[]::new));

		if (enableSourcesOnly) {
			return cells.map(fc(i -> factor(genome.valueAt(DefaultAudioGenome.VOLUME, i, 0))))
					.sum().map(fc(i -> sf(0.2))).map(i -> new ReceptorCell<>(Receptor.to(output, measures.get(0), measures.get(1))));
		}

		if (enableMixdown)
			cells = cells.mixdown(mixdownDuration);

		// Volume adjustment
		CellList branch[] = cells.branch(
				fc(i -> factor(genome.valueAt(DefaultAudioGenome.VOLUME, i, 0))),
				enableEfxFilters ?
						fc(i -> factor(genome.valueAt(DefaultAudioGenome.VOLUME, i, 0))
								.andThen(genome.valueAt(DefaultAudioGenome.FX_FILTERS, i, 0))) :
						fc(i -> factor(genome.valueAt(DefaultAudioGenome.VOLUME, i, 0))));

		CellList main = branch[0];
		CellList efx = branch[1];

		// Sum the main layer
		main = main.sum();

		if (enableEfx) {
			// Create the delay layers
			int delayLayers = genome.valueAt(DefaultAudioGenome.PROCESSORS).length();
			CellList delays = IntStream.range(0, delayLayers)
					.mapToObj(i -> new AdjustableDelayCell(OutputLine.sampleRate,
							genome.valueAt(DefaultAudioGenome.PROCESSORS, i, 0).getResultant(v(1.0)),
							genome.valueAt(DefaultAudioGenome.PROCESSORS, i, 1).getResultant(v(1.0))))
					.collect(CellList.collector());

			// Route each line to each delay layer
			efx = efx.m(fi(), delays, i -> delayGene(delayLayers, genome.valueAt(DefaultAudioGenome.WET_IN, i)))
					// Feedback grid
					.mself(fi(), genome.valueAt(DefaultAudioGenome.TRANSMISSION),
							fc(genome.valueAt(DefaultAudioGenome.WET_OUT, 0)))
					.sum();

			if (disableClean) {
				efx.get(0).setReceptor(Receptor.to(output, measures.get(0), measures.get(1)));
				return efx;
			} else {
				// Mix efx with main and measure #2
				efx.get(0).setReceptor(Receptor.to(main.get(0), measures.get(1)));

				if (enableMasterFilterDown) {
					// Apply dynamic low pass filter
					main = main.map(fc(i -> {
						TemporalFactor<Scalar> f = (TemporalFactor<Scalar>) genome.valueAt(DefaultAudioGenome.MASTER_FILTER_DOWN, i, 0);
						return lp(scalarsMultiply(v(20000), f.getResultant(v(1.0))), v(DefaultAudioGenome.defaultResonance));
//							return lp(scalarsMultiply(v(20000), v(1.0)), v(DefaultAudioGenome.defaultResonance));
					}));
				}

				// Deliver main to the output and measure #1
				main = main.map(i -> new ReceptorCell<>(Receptor.to(output, measures.get(0))));

				return cells(main, efx);
			}
		} else {
			// Deliver main to the output and measure #1 and #2
			return main.map(i -> new ReceptorCell<>(Receptor.to(output, measures.get(0), measures.get(1))));
		}
	}

	/**
	 * This method wraps the specified {@link Factor} to prevent it from
	 * being detected as Temporal by {@link org.almostrealism.graph.FilteredCell}s
	 * that would proceed to invoke the {@link org.almostrealism.time.Temporal#tick()} operation.
	 * This is not a good solution, and this process needs to be reworked so
	 * it is clear who bears the responsibility for invoking {@link org.almostrealism.time.Temporal#tick()}
	 * and it doesn't get invoked multiple times.
	 */
	private Factor<Scalar> factor(Factor<Scalar> f) {
		return v -> f.getResultant(v);
	}

	/**
	 * Create a {@link Gene} for routing delays.
	 * The current implementation delivers audio to
	 * the first delay based on the wet level, and
	 * delivers nothing to the others.
	 */
	private Gene<Scalar> delayGene(int delays, Gene<Scalar> wet) {
		ArrayListGene<Scalar> gene = new ArrayListGene<>();

		if (enableWetInAdjustment) {
			gene.add(factor(wet.valueAt(0)));
		} else {
			gene.add(p -> v(0.2).multiply(p));
		}

		IntStream.range(0, delays - 1).forEach(i -> gene.add(p -> v(0.0)));
		return gene;
	}
}
