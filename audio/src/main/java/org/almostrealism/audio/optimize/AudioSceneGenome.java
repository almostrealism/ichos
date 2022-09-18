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

import org.almostrealism.collect.PackedCollection;
import org.almostrealism.heredity.Chromosome;
import org.almostrealism.heredity.Genome;

/*
 * TODO  This class is just a temporary solution to the problem that
 * TODO  not everything has been migrated to ConfigurableGenome.
 */
@Deprecated
public class AudioSceneGenome implements Genome<PackedCollection<?>> {
	private Genome<PackedCollection<?>> genome;
	private Genome<PackedCollection<?>> legacyGenome;

	public AudioSceneGenome() { }

	public AudioSceneGenome(Genome<PackedCollection<?>> genome, Genome<PackedCollection<?>> legacyGenome) {
		this.genome = genome;
		this.legacyGenome = legacyGenome;
	}

	public Genome<PackedCollection<?>> getGenome() {
		return genome;
	}

	public void setGenome(Genome<PackedCollection<?>> genome) {
		this.genome = genome;
	}

	public Genome<PackedCollection<?>> getLegacyGenome() {
		return legacyGenome;
	}

	public void setLegacyGenome(Genome<PackedCollection<?>> legacyGenome) {
		this.legacyGenome = legacyGenome;
	}

	@Override
	public Genome getHeadSubset() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Chromosome getLastChromosome() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int count() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Chromosome<PackedCollection<?>> valueAt(int pos) {
		throw new UnsupportedOperationException();
	}
}
