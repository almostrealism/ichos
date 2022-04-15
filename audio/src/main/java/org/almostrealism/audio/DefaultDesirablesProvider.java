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

package org.almostrealism.audio;

import org.almostrealism.audio.tone.DefaultKeyboardTuning;
import  org.almostrealism.audio.tone.KeyPosition;
import  org.almostrealism.audio.tone.KeyboardTuning;
import  org.almostrealism.audio.tone.Scale;
import org.almostrealism.time.Frequency;

import java.util.HashSet;
import java.util.Set;

public class DefaultDesirablesProvider<T extends KeyPosition<T>> implements DesirablesProvider {
	private final double bpm;
	private final Set<Frequency> frequencies;
	private Waves waves;

	public DefaultDesirablesProvider(double bpm) {
		this(bpm, Scale.of());
	}

	public DefaultDesirablesProvider(double bpm, Scale<T> scale) {
		this(bpm, scale, new DefaultKeyboardTuning());
	}

	public DefaultDesirablesProvider(double bpm, Scale<T> scale, KeyboardTuning tuning) {
		this.bpm = bpm;
		this.frequencies = new HashSet<>();
		this.frequencies.addAll(tuning.getTones(scale));
		this.waves = new Waves();
	}

	@Override
	public double getBeatPerMinute() { return bpm; }

	public Set<Frequency> getFrequencies() { return frequencies; }

	public void setWaves(Waves waves) { this.waves = waves; }

	@Override
	public Waves getWaves() { return waves; }
}
