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

package org.almostrealism.audio.filter;

import io.almostrealism.relation.Producer;
import org.almostrealism.time.CursorPair;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.graph.Adjustable;
import org.almostrealism.graph.SummationCell;
import org.almostrealism.time.AcceleratedTimeSeries;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.util.CodeFeatures;

import java.util.function.Supplier;

public class AdjustableDelayCell extends SummationCell implements Adjustable<Scalar>, CodeFeatures {

	private AcceleratedTimeSeries buffer;
	private CursorPair cursors;

	private Scalar scale;

	public AdjustableDelayCell(int delay) {
		initCursors();
		buffer = AcceleratedTimeSeries.defaultSeries();
		setDelay(delay);
		scale = new Scalar(1.0);
	}

	protected void initCursors() {
		cursors = new CursorPair();
	}

	public CursorPair getCursors() { return cursors; }

	public AcceleratedTimeSeries getBuffer() { return buffer; }

	public synchronized void setDelay(double msec) {
		cursors.setDelayCursor(cursors.getCursor() + (msec / 1000d) * OutputLine.sampleRate);
	}

	public synchronized double getDelay() {
		return 1000 * (cursors.getDelayCursor() - cursors.getCursor()) / OutputLine.sampleRate;
	}

	public synchronized void setDelayInFrames(double frames) {
		cursors.setDelayCursor(cursors.getCursor() + frames);
	}

	public synchronized double getDelayInFrames() { return cursors.getDelayCursor() - cursors.getCursor(); }

	@Override
	public Supplier<Runnable> updateAdjustment(Producer<Scalar> value) {
		return a(2, p(scale), value);
	}

	@Override
	public Supplier<Runnable> push(Producer<Scalar> protein) {
		Scalar value = new Scalar();

		OperationList push = new OperationList();
		push.add(buffer.add(temporal(r(p(cursors)), protein)));
		push.add(a(2, p(value), buffer.valueAt(p(cursors))));
		push.add(buffer.purge(p(cursors)));
		push.add(cursors.increment(p(scale)));
		push.add(super.push(p(value)));
		return push;
	}
}
