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

import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.data.ParameterizedWaveDataProviderFactory;
import org.almostrealism.audio.data.StaticWaveDataProviderFactory;
import org.almostrealism.audio.data.WaveDataProvider;
import org.almostrealism.audio.data.WaveDataProviderList;
import org.almostrealism.time.Frequency;

import java.util.List;

public class WaveSet {
	private ParameterizedWaveDataProviderFactory source;

	public WaveSet() { }

	public WaveSet(WaveDataProvider source) {
		this(new StaticWaveDataProviderFactory(source));
	}

	public WaveSet(ParameterizedWaveDataProviderFactory source) {
		setSource(source);
	}

	public ParameterizedWaveDataProviderFactory getSource() { return source; }

	public void setSource(ParameterizedWaveDataProviderFactory source) { this.source = source; }

	public int getCount() { return source.getCount(); }

	public WaveDataProviderList create(Producer<Scalar> x, Producer<Scalar> y, Producer<Scalar> z) {
		return source.create(x, y, z, List.of(new Frequency(1.0)));
	}
}
