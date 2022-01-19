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

package org.almostrealism.audio.sources;

import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.data.PolymorphicAudioData;
import org.almostrealism.audio.data.ValueSequenceData;
import org.almostrealism.audio.filter.AudioCellAdapter;
import org.almostrealism.hardware.OperationList;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ValueSequenceCell extends AudioCellAdapter {
	private ValueSequenceData data;
	private List<Producer<Scalar>> values;
	private Producer<Scalar> durationFrames;

	public ValueSequenceCell(IntFunction<Producer<Scalar>> values, Producer<Scalar> duration, int steps) {
		this(new PolymorphicAudioData(), values, duration, steps);
	}

	public ValueSequenceCell(ValueSequenceData data, IntFunction<Producer<Scalar>> values, Producer<Scalar> duration, int steps) {
		this.data = data;
		this.values = IntStream.range(0, steps).mapToObj(values).collect(Collectors.toList());
		this.durationFrames = toFrames(duration);
		addSetup(a(1, data::getWaveLength, v(1)));
	}

	@Override
	public Supplier<Runnable> push(Producer<Scalar> protein) {
		Scalar value = new Scalar();
		OperationList push = new OperationList("ValueSequenceCell Push");
		push.add(new ValueSequencePush(data, durationFrames, value, values.toArray(Producer[]::new)));
		push.add(super.push(p(value)));
		return push;
	}

	@Override
	public Supplier<Runnable> tick() {
		OperationList tick = new OperationList("ValueSequenceCell Tick");
		tick.add(new ValueSequenceTick(data, durationFrames, values.toArray(Producer[]::new)));
		tick.add(super.tick());
		return tick;
	}
}
