/*
 * Copyright 2021 Michael Murray
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

package org.almostrealism.audio.computations;

import io.almostrealism.code.ArrayVariable;
import io.almostrealism.code.HybridScope;
import io.almostrealism.code.Scope;
import io.almostrealism.code.expressions.Expression;
import org.almostrealism.algebra.Pair;
import org.almostrealism.algebra.PairBank;
import org.almostrealism.hardware.DynamicProducerComputationAdapter;
import org.almostrealism.hardware.jni.NativeComputationEvaluable;
import org.almostrealism.hardware.jni.NativeSupport;
import org.almostrealism.util.Ops;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class NativeFFT extends DynamicProducerComputationAdapter<PairBank, PairBank> implements NativeSupport<NativeComputationEvaluable> {
	public NativeFFT(int count, boolean forward) {
		super(count, () -> args -> new PairBank(count),
				i -> { throw new UnsupportedOperationException(); },
				new Supplier[] { Ops.ops().v(2 * count, 0) },
				config(count, forward));
		int powerOfTwo = 31 - Integer.numberOfLeadingZeros(count);

		if (1 << powerOfTwo != count) {
			throw new IllegalArgumentException("ComplexFFT not supported for " +
					count + " bins (use " + (1 << powerOfTwo) + ")");
		}

		if (!forward) {
			// TODO  Support backward
			throw new UnsupportedOperationException();
		}

		initNative();
	}

	@Override
	public IntFunction<Expression<Double>> getValueFunction() { throw new UnsupportedOperationException(); }

	@Override
	public Scope<PairBank> getScope() {
		HybridScope s = new HybridScope(this);
		Consumer<String> code = s.code();
		code.accept("transform(");
		code.accept(((ArrayVariable<Double>) getArgument(0).getRootDelegate()).getName());
		code.accept(", ");
		code.accept(((ArrayVariable<Double>) getArgument(1).getRootDelegate()).getName());
		code.accept(", ");
		code.accept(((ArrayVariable<Double>) getArgument(2).getRootDelegate()).getName());
		code.accept(", ");
		code.accept(((ArrayVariable<Double>) getArgument(0).getRootDelegate()).getName());
		code.accept("Offset + ");
		code.accept(String.valueOf(getArgument(0).getOffset()));
		code.accept(", ");
		code.accept(((ArrayVariable<Double>) getArgument(1).getRootDelegate()).getName());
		code.accept("Offset + ");
		code.accept(String.valueOf(getArgument(1).getOffset()));
		code.accept(", ");
		code.accept(((ArrayVariable<Double>) getArgument(2).getRootDelegate()).getName());
		code.accept("Offset + ");
		code.accept(String.valueOf(getArgument(2).getOffset()));
		code.accept(", ");
		code.accept(((ArrayVariable<Double>) getArgument(0).getRootDelegate()).getName());
		code.accept("Size");
		code.accept(", ");
		code.accept(((ArrayVariable<Double>) getArgument(1).getRootDelegate()).getName());
		code.accept("Size");
		code.accept(", ");
		code.accept(((ArrayVariable<Double>) getArgument(2).getRootDelegate()).getName());
		code.accept("Size");
		code.accept(");");
		return s;
	}

	@Override
	public NativeComputationEvaluable<PairBank> get() {
		NativeComputationEvaluable<PairBank> ev = new NativeComputationEvaluable<>(this);
		ev.setHead(getHead());
		return ev;
	}

	protected String getHead() {
		StringBuilder buf = new StringBuilder();

		try (InputStream in = NativeFFT.class.getResourceAsStream("NativeFFT.c");
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			Stream.generate(() -> {
				try {
					return reader.readLine();
				} catch (IOException e) {
					return null;
				}
			}).takeWhile(Objects::nonNull)
					.map(v -> v + "\n")
					.forEach(buf::append);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return buf.toString();
	}

	private static Object[] config(int count, boolean forward) {
		return new Object[] { new Pair(count, forward ? 0 : 1) };
	}
}
