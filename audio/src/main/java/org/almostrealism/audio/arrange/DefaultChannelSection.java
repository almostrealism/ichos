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

import io.almostrealism.relation.Producer;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.heredity.ConfigurableGenome;
import org.almostrealism.heredity.SimpleChromosome;

import java.util.function.Supplier;

public class DefaultChannelSection implements ChannelSection {
	private int position, length;
	private SimpleChromosome chromosome;

	public DefaultChannelSection() { }

	protected DefaultChannelSection(int position, int length, SimpleChromosome chromosome) {
		this.position = position;
		this.length = length;
		this.chromosome = chromosome;
	}

	@Override
	public int getPosition() { return position; }

	@Override
	public int getLength() { return length; }

	@Override
	public Supplier<Runnable> process(Producer<PackedCollection<?>> destination, Producer<PackedCollection<?>> source) {
		return () -> () -> {
			PackedCollection<?> in = source.get().evaluate();
			destination.get().evaluate().setMem(0, in.toArray(0, in.getMemLength())); // TODO  This wont work for stereo, etc.
		};
	}

	public static DefaultChannelSection createSection(int position, int length, ConfigurableGenome genome) {
		return new DefaultChannelSection(position, length, genome.addSimpleChromosome(6));
	}
}
