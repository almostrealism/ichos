package org.almostrealism.audio.data;

import io.almostrealism.relation.Provider;
import org.almostrealism.algebra.Scalar;

public interface BaseAudioData {
	Scalar get(int index);

	default Scalar wavePosition() { return get(0); }
	default Scalar waveLength() { return get(1); }
	default Scalar amplitude() { return get(2); }

	default Provider<Scalar> getWavePosition() { return new Provider<>(wavePosition()); }
	default void setWavePosition(double wavePosition) { wavePosition().setValue(wavePosition); }
	default Provider<Scalar> getWaveLength() { return new Provider<>(waveLength()); }
	default void setWaveLength(double waveLength) { waveLength().setValue(waveLength); }
	default Provider<Scalar> getAmplitude() { return new Provider<>(amplitude()); }
	default void setAmplitude(double amplitude) { amplitude().setValue(amplitude); }
}
