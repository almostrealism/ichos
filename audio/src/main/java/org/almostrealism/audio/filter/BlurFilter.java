package org.almostrealism.audio.filter;

import io.almostrealism.code.Scope;
import org.almostrealism.graph.ByteFunction;
import io.almostrealism.code.NameProvider;

public class BlurFilter implements ByteFunction<byte[]> {
	@Override
	public byte[] operate(byte[] b) {
		return b;
	}

	@Override
	public Scope<byte[]> getScope() { throw new RuntimeException("Not implemented"); }
}
