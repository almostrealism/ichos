/*
 * Copyright 2016 Michael Murray
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

package org.almostrealism.audio.filter;

import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.graph.Adjustable;
import org.almostrealism.graph.CellAdapter;
import org.almostrealism.graph.ScalarCachedStateCell;
import org.almostrealism.hardware.OperationList;

import java.util.function.Supplier;

public abstract class AudioCellAdapter extends ScalarCachedStateCell implements Adjustable<Scalar> {
	public static double depth = 1.0;
	public static double PI = Math.PI;

	protected int toFrames(int msec) { return (int) (OutputLine.sampleRate * msec / 1000d); }

	@Override
	public Supplier<Runnable> updateAdjustment(Producer<Scalar> value) {
		return new OperationList();
	}
}
