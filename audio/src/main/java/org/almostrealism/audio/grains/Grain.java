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

package org.almostrealism.audio.grains;

import org.almostrealism.collect.PackedCollection;

public class Grain extends PackedCollection {
	public Grain() {
		super(3);
	}

	public double getStart() { return toArray(0, 1)[0]; }
	public void setStart(double start) { setMem(0, start); }

	public double getDuration() { return toArray(1, 1)[0]; }
	public void setDuration(double duration) { setMem(1, duration); }

	public double getRate() { return toArray(2, 1)[0]; }
	public void setRate(double rate) { setMem(2, rate); }
}
