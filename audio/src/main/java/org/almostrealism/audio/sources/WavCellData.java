/*
 * Copyright 2021 Michael Murray
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

package org.almostrealism.audio.sources;

import io.almostrealism.relation.Provider;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;

public class WavCellData extends ScalarBank {
	public WavCellData(int count, double amplitude) {
		super(6);
		setWavePosition(0.0);
		setWaveLength(1.0);
		setWaveCount(count);
		setAmplitude(amplitude);
		setDuration(1.0);
	}

	protected Scalar wavePosition() { return get(0); }
	protected Scalar waveLength() { return get(1); }
	protected Scalar waveCount() { return get(2); }
	protected Scalar amplitude() { return get(3); }
	protected Scalar duration() { return get(4); }

	public Provider<Scalar> getWavePosition() { return new Provider<>(wavePosition()); }
	public void setWavePosition(double wavePosition) { wavePosition().setValue(wavePosition); }

	public Provider<Scalar> getWaveLength() { return new Provider<>(waveLength()); }
	public void setWaveLength(double waveLength) { waveLength().setValue(waveLength); }

	public Provider<Scalar> getWaveCount() { return new Provider<>(waveCount()); }
	public void setWaveCount(int count) { waveCount().setValue(count); }

	public Provider<Scalar> getAmplitude() { return new Provider<>(amplitude()); }
	public void setAmplitude(double amplitude) { amplitude().setValue(amplitude); }

	public Provider<Scalar> getDuration() { return new Provider<>(duration()); }
	public void setDuration(double duration) { duration().setValue(duration); }
}

