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

package org.almostrealism.audio.filter;

import io.almostrealism.relation.Producer;
import io.almostrealism.relation.Provider;
import org.almostrealism.time.CursorPair;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.graph.SummationCell;
import org.almostrealism.time.AcceleratedTimeSeries;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.CodeFeatures;
import org.almostrealism.Ops;

import java.util.function.Supplier;

public class AdjustableDelayCell extends SummationCell implements CodeFeatures {
	public static double defaultPurgeFrequency = 1.0;

	private final AcceleratedTimeSeries buffer;
	private CursorPair cursors;

	private final Producer<Scalar> delay;
	private final Producer<Scalar> scale;

	public AdjustableDelayCell(double delay) {
		this(new Scalar(delay));
	}

	public AdjustableDelayCell(Scalar delay) {
		this(Ops.ops().v(delay), Ops.ops().v(1.0));
	}

	public AdjustableDelayCell(Producer<Scalar> delay, Producer<Scalar> scale) {
		initCursors();
		buffer = AcceleratedTimeSeries.defaultSeries();
		this.delay = delay;
		this.scale = scale;
	}

	protected void initCursors() {
		cursors = new CursorPair();
	}

	public CursorPair getCursors() { return cursors; }

	public AcceleratedTimeSeries getBuffer() { return buffer; }

	public Producer<Scalar> getDelay() { return delay; }

	public Producer<Scalar> getScale() { return scale; }

	@Override
	public Supplier<Runnable> setup() {
		OperationList setup = new OperationList("AdjustableDelayCell Setup");
		setup.add(super.setup());
		setup.add(a(2, p(cursors), pair(v(0.0), v(OutputLine.sampleRate).multiply(delay))));
		return setup;
	}

//	@Override
//	public Supplier<Runnable> push(Producer<Scalar> protein) {
//		Scalar value = new Scalar();
//
//		OperationList push = new OperationList("AdjustableDelayCell Push");
//		push.add(buffer.add(temporal(r(p(cursors)), protein)));
//		push.add(a(1, p(value), buffer.valueAt(p(cursors))));
//		push.add(buffer.purge(p(cursors), defaultPurgeFrequency));
//		push.add(cursors.increment(scale));
//		push.add(super.push(p(value)));
//		return push;
//	}

	@Override
	public Supplier<Runnable> tick() {
		OperationList tick = new OperationList("AdjustableDelayCell Tick");
		tick.add(buffer.add(temporal(r(p(cursors)), p(getCachedValue()))));
		tick.add(a(1, p(getOutputValue()), buffer.valueAt(p(cursors))));
		tick.add(buffer.purge(p(cursors), defaultPurgeFrequency));
		tick.add(cursors.increment(scale));
		tick.add(reset(p(getCachedValue())));
		tick.add(pushValue());
		return tick;
	}

	@Override
	public void reset() {
		super.reset();
		buffer.reset();
	}
}
