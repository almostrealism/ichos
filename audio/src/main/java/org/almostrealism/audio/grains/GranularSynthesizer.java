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

package org.almostrealism.audio.grains;

import org.almostrealism.audio.WaveOutput;
import org.almostrealism.audio.data.WaveData;
import org.almostrealism.audio.data.WaveDataProviderAdapter;

import java.util.UUID;

public class GranularSynthesizer extends WaveDataProviderAdapter {
	private String key;

	public GranularSynthesizer() {
		key = "synth://" + UUID.randomUUID().toString();
	}

	@Override
	public int getCount() {
		return WaveOutput.defaultTimelineFrames;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	protected WaveData load() {
		return null;
	}
}
