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

package com.almostrealism.audio.filter.test;

import com.almostrealism.audio.SineWaveCell;
import com.almostrealism.audio.filter.AdjustableDelayCell;
import com.almostrealism.audio.test.SineWaveCellTest;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarProducer;
import org.almostrealism.hardware.mem.MemoryBankAdapter;
import org.almostrealism.hardware.OperationList;
import org.almostrealism.time.AcceleratedTimeSeries;
import org.almostrealism.time.CursorPair;
import org.almostrealism.time.TemporalScalar;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.stream.IntStream;

public class AdjustableDelayCellTest extends SineWaveCellTest {
	public static int DELAY_FRAMES = 1000;

	@BeforeClass
	public static void init() {
		AcceleratedTimeSeries.defaultCacheLevel = MemoryBankAdapter.CacheLevel.ALL;
	}

	@AfterClass
	public static void shutdown() {
		AcceleratedTimeSeries.defaultCacheLevel = MemoryBankAdapter.CacheLevel.NONE;
	}

	protected AdjustableDelayCell adjustableDelay() {
		AdjustableDelayCell cell = new AdjustableDelayCell(1);
		cell.setDelayInFrames(DELAY_FRAMES);
		return cell;
	}

	public OperationList computation(AdjustableDelayCell delay) {
		Scalar multiplier = new Scalar(0.1);
		ScalarProducer product = v(1.0).multiply(p(multiplier));

		OperationList ops = new OperationList();
		ops.add(delay.push(product));
		ops.add(delay.tick());
		return ops;
	}

	@Test
	public void computationSeparately() {
		AdjustableDelayCell delay = adjustableDelay();
		OperationList ops = computation(delay);

		Runnable push = ops.get(0).get();
		Runnable tick = ops.get(1).get();

		IntStream.range(0, 25).forEach(i -> {
			push.run();
			tick.run();
		});
		assertions(delay);
	}

	@Test
	public void computationTogether() {
		AdjustableDelayCell delay = adjustableDelay();
		OperationList ops = computation(delay);

		Runnable op = ops.get();

		IntStream.range(0, 25).forEach(i -> op.run());
		assertions(delay);
	}

	@Test
	public void computationLoop() {
		AdjustableDelayCell delay = adjustableDelay();
		OperationList ops = computation(delay);

		Runnable op = loop(ops, 25).get();
		op.run();
		assertions(delay);
	}

	protected void assertions(AdjustableDelayCell delay) {
		CursorPair cursors = delay.getCursors();
		assertEquals(25.0, cursors.getCursor());
		assertEquals(DELAY_FRAMES + 25, cursors.getDelayCursor());

		AcceleratedTimeSeries buffer = delay.getBuffer();
		TemporalScalar t = buffer.valueAt(delay.getDelayInFrames());
		System.out.println(t);
		assertEquals(0.1, t.getValue());
	}

	@Test
	public void withAdjustableDelayCell() {
		AdjustableDelayCell delay = adjustableDelay();
		delay.setReceptor(loggingReceptor());

		SineWaveCell cell = cell();
		cell.setReceptor(delay);

		Runnable push = cell.push(null).get();
		Runnable tick = delay.tick().get();

		IntStream.range(0, 200).forEach(i -> {
			push.run();
			tick.run();
			System.out.println("AdjustableDelayCellTest: " + delay.getCursors());
			// TODO  Assertions
		});
	}
}
