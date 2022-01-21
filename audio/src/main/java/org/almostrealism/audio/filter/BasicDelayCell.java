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

package org.almostrealism.audio.filter;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.graph.Adjustable;
import org.almostrealism.graph.SummationCell;
import io.almostrealism.relation.Producer;
import org.almostrealism.time.Updatable;
import org.almostrealism.CodeFeatures;

import java.util.function.Supplier;

public class BasicDelayCell extends SummationCell implements Adjustable<Scalar>, CodeFeatures {
	public static int bufferDuration = 10;
	
	private double buffer[] = new double[bufferDuration * OutputLine.sampleRate];
	private int cursor;
	private int delay;
	
	private Updatable updatable;
	
	public BasicDelayCell(int delay) {
		setDelay(delay);
	}

	public synchronized void setDelay(int msec) {
		this.delay = (int) ((msec / 1000d) * OutputLine.sampleRate);
	}

	public synchronized int getDelay() { return 1000 * delay / OutputLine.sampleRate; }

	public synchronized void setDelayInFrames(long frames) {
		if (frames != delay) System.out.println("Delay frames: " + frames);
		this.delay = (int) frames;
		if (delay <= 0) delay = 1;
	}

	public synchronized long getDelayInFrames() { return this.delay; }

	public synchronized Position getPosition() {
		Position p = new Position();
		if (delay == 0) delay = 1;
		p.pos = (cursor % delay) / (double) delay;
		p.value = buffer[cursor];
		return p;
	}
	
	public void setUpdatable(Updatable ui) { this.updatable = ui; }

	@Override
	public Supplier<Runnable> updateAdjustment(Producer<Scalar> value) {
		return () -> () -> { };
	}

	@Override
	public synchronized Supplier<Runnable> push(Producer<Scalar> protein) {
		Scalar value = new Scalar();
		Supplier<Runnable> push = super.push(p(value));

		return () -> () -> {
			int dPos = (cursor + delay) % buffer.length;

			this.buffer[dPos] = buffer[dPos] + protein.get().evaluate().getValue();

			value.setMem(new double[] { buffer[cursor], 1.0 });

			if (updatable != null && cursor % updatable.getResolution() == 0) updatable.update();

			this.buffer[cursor] = 0;
			cursor++;
			cursor = cursor % buffer.length;
			push.get().run();
		};
	}

	@Override
	public void reset() {
		super.reset();
		// TODO throw new UnsupportedOperationException();
	}

	public static class Position {
		public double pos;
		public double value;
	}
}