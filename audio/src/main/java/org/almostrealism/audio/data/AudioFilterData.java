package org.almostrealism.audio.data;

import io.almostrealism.relation.Provider;
import org.almostrealism.algebra.Scalar;

public interface AudioFilterData extends BaseAudioData {
	Scalar get(int index);

	default Scalar frequency() { return get(0); }
	default Scalar resonance() { return get(1); }
	default Scalar sampleRate() { return get(2); }
	default Scalar c() { return get(3); }
	default Scalar a1() { return get(4); }
	default Scalar a2() { return get(5); }
	default Scalar a3() { return get(6); }
	default Scalar b1() { return get(7); }
	default Scalar b2() { return get(8); }
	default Scalar output() { return get(9); }
	default Scalar inputHistory0() { return get(10); }
	default Scalar inputHistory1() { return get(11); }
	default Scalar outputHistory0() { return get(12); }
	default Scalar outputHistory1() { return get(13); }
	default Scalar outputHistory2() { return get(14); }

	default Provider<Scalar> getFrequency() { return new Provider<>(frequency()); }
	default void setFrequency(double frequency) { frequency().setValue(frequency); }

	default Provider<Scalar> getResonance() { return new Provider<>(resonance()); }
	default void setResonance(double resonance) { resonance().setValue(resonance); }

	default Provider<Scalar> getSampleRate() { return new Provider<>(sampleRate()); }
	default void setSampleRate(double sampleRate) { sampleRate().setValue(sampleRate); }

	default Provider<Scalar> getC() { return new Provider<>(c()); }
	default Provider<Scalar> getA1() { return new Provider<>(a1()); }
	default Provider<Scalar> getA2() { return new Provider<>(a2()); }
	default Provider<Scalar> getA3() { return new Provider<>(a3()); }
	default Provider<Scalar> getB1() { return new Provider<>(b1()); }
	default Provider<Scalar> getB2() { return new Provider<>(b2()); }
	default Provider<Scalar> getOutput() { return new Provider<>(output()); }
	default Provider<Scalar> getInputHistory0() { return new Provider<>(inputHistory0()); }
	default Provider<Scalar> getInputHistory1() { return new Provider<>(inputHistory1()); }
	default Provider<Scalar> getOutputHistory0() { return new Provider<>(outputHistory0()); }
	default Provider<Scalar> getOutputHistory1() { return new Provider<>(outputHistory1()); }
	default Provider<Scalar> getOutputHistory2() { return new Provider<>(outputHistory2()); }
}
