/*
 * Copyright 2022 Michael Murray
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

package org.almostrealism.audio;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Supplier;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.graph.CollectionCachedStateCell;
import org.almostrealism.graph.Source;
import org.almostrealism.graph.SummationCell;
import org.almostrealism.time.Temporal;
import org.almostrealism.hardware.OperationList;

public class Mixer extends ArrayList<Source<PackedCollection<?>>> implements Temporal {
	private SummationCell sum;

	private CollectionCachedStateCell channels[] = new CollectionCachedStateCell[24];
	
	public Mixer(SummationCell receptor) {
		this.sum = receptor;

		for (int i = 0; i < channels.length; i++) {
			channels[i] = new CollectionCachedStateCell();
		}
	}

	public CollectionCachedStateCell getChannel(int i) {
		return channels[i];
	}
	
	@Override
	public Supplier<Runnable> tick() {
		OperationList tick = new OperationList("Mixer Tick");
		stream().map(s -> sum.push(s.next())).forEach(tick::add);

		tick.add(() -> () -> {
			Iterator<Source<PackedCollection<?>>> itr = iterator();
			while (itr.hasNext()) if (itr.next().isDone()) itr.remove();
		});
		
		tick.add(sum.tick());
		return tick;
	}
}
