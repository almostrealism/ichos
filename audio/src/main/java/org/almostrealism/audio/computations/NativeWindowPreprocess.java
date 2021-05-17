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

import io.almostrealism.relation.Evaluable;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.hardware.jni.NativeComputationEvaluable;
import org.almostrealism.hardware.jni.NativeSupport;

import java.util.function.Supplier;

public abstract class NativeWindowPreprocess extends WindowPreprocess implements NativeSupport<NativeComputationEvaluable> {
	public NativeWindowPreprocess(int windowSize, int paddedWindowSize, Supplier<Evaluable<? extends ScalarBank>> input) {
		this(windowSize, paddedWindowSize, "povey", new Scalar(0.42), new Scalar(0.97), input);
	}

	public NativeWindowPreprocess(int windowSize, int paddedWindowSize, String windowType, Scalar blackmanCoeff, Scalar preemphCoeff, Supplier<Evaluable<? extends ScalarBank>> input) {
		super(windowSize, paddedWindowSize, windowType, blackmanCoeff, preemphCoeff, input);
		initNative();
	}

	@Override
	public NativeComputationEvaluable<ScalarBank> get() {
		return new NativeComputationEvaluable<>(this);
	}
}
