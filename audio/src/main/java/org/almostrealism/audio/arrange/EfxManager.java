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

import org.almostrealism.audio.CellList;
import org.almostrealism.heredity.ConfigurableGenome;

import java.util.function.DoubleSupplier;

public class EfxManager {
	private ConfigurableGenome genome;
	private int channels;

	private DoubleSupplier beatDuration;
	private int sampleRate;

	public EfxManager(ConfigurableGenome genome, int channels, DoubleSupplier beatDuration, int sampleRate) {
		this.genome = genome;
		this.channels = channels;
		this.beatDuration = beatDuration;
		this.sampleRate = sampleRate;
	}

	public CellList apply(int channel, CellList cells) {
		// TODO Auto-generated method stub
		return cells;
	}
}
