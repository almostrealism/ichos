/*
 * Copyright 2020 Michael Murray
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

import io.almostrealism.relation.Provider;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;

public class SineWaveCellData extends ScalarBank {
	public SineWaveCellData() {
		super(7);
	}

	protected Scalar wavePosition() { return get(0); }
	protected Scalar waveLength() { return get(1); }
	protected Scalar notePosition() { return get(2); }
	protected Scalar noteLength() { return get(3); }
	protected Scalar phase() { return get(4); }
	protected Scalar amplitude() { return get(5); }
	protected Scalar depth() { return get(6); }

	public Provider<Scalar> getWavePosition() { return new Provider<>(wavePosition()); }
	public void setWavePosition(double wavePosition) { wavePosition().setValue(wavePosition); }

	public Provider<Scalar> getWaveLength() { return new Provider<>(waveLength()); }
	public void setWaveLength(double waveLength) { waveLength().setValue(waveLength); }

	public Provider<Scalar> getNotePosition() { return new Provider<>(notePosition()); }
	public void setNotePosition(double notePosition) { notePosition().setValue(notePosition); }

	public Provider<Scalar> getNoteLength() { return new Provider<>(noteLength()); }
	public void setNoteLength(double noteLength) { noteLength().setValue(noteLength); }

	public Provider<Scalar> getPhase() { return new Provider<>(phase()); }
	public void setPhase(double phase) { phase().setValue(phase); }

	public Provider<Scalar> getAmplitude() { return new Provider<>(amplitude()); }
	public void setAmplitude(double amplitude) { amplitude().setValue(amplitude); }

	public Provider<Scalar> getDepth() { return new Provider<>(depth()); }
	public void setDepth(double depth) { depth().setValue(depth); }
}
