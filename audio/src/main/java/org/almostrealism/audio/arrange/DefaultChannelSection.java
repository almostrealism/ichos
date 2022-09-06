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

package org.almostrealism.audio.arrange;

import io.almostrealism.cycle.Setup;
import io.almostrealism.relation.Producer;
import org.almostrealism.CodeFeatures;
import org.almostrealism.audio.optimize.LinearInterpolationChromosome;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.hardware.KernelizedEvaluable;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.heredity.ConfigurableGenome;
import org.almostrealism.heredity.SimpleChromosome;
import org.almostrealism.heredity.SimpleGene;

import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public class DefaultChannelSection implements ChannelSection, CodeFeatures {
	private int position, length;

	private LinearInterpolationChromosome interpolation;
	private int geneIndex;

	public DefaultChannelSection() { }

	protected DefaultChannelSection(int position, int length, LinearInterpolationChromosome interpolation, int geneIndex) {
		this.position = position;
		this.length = length;
		this.interpolation = interpolation;
		this.geneIndex = geneIndex;
	}

	@Override
	public int getPosition() { return position; }

	@Override
	public int getLength() { return length; }

	@Override
	public Supplier<Runnable> process(Producer<PackedCollection<?>> destination, Producer<PackedCollection<?>> source) {
		KernelizedEvaluable product = _multiply(v(0, 0), v(0, 1)).get();

		return () -> () ->
			// TODO  Should be able to just use ::evaluate here...
			product.kernelEvaluate(destination.get().evaluate().traverse(1),
									source.get().evaluate().traverse(1),
									interpolation.getKernelList(0).valueAt(geneIndex));
	}

	public static class Factory implements Setup {
		private ConfigurableGenome genome;
		private SimpleChromosome chromosome;
		private LinearInterpolationChromosome interpolation;
		private int channel, channels;

		private DoubleSupplier measureDuration;
		private int length;
		private int sampleRate;

		public Factory(ConfigurableGenome genome, int channels, DoubleSupplier measureDuration, int length, int sampleRate) {
			this.genome = genome;
			this.channels = channels;
			this.measureDuration = measureDuration;
			this.length = length;
			this.sampleRate = sampleRate;

			this.chromosome = genome.addSimpleChromosome(LinearInterpolationChromosome.SIZE);

			for (int i = 0; i < channels; i++) {
				// TODO  Testing interpolation from 0.2 to 0.95 - this should be removed
				SimpleGene g = chromosome.addGene();
				g.set(0, 0.2);
				g.set(1, 0.95);
			}

			this.interpolation = new LinearInterpolationChromosome(chromosome, 0.0, 1.0, sampleRate);
		}

		public DefaultChannelSection createSection(int position) {
			if (channel >= channels) throw new IllegalArgumentException();
			return new DefaultChannelSection(position, length, interpolation, channel++);
		}

		@Override
		public Supplier<Runnable> setup() {
			OperationList setup = new OperationList();
			setup.add(() -> () -> interpolation.setDuration(length * measureDuration.getAsDouble()));
			setup.add(interpolation.expand());
			return setup;
		}
	}
}
