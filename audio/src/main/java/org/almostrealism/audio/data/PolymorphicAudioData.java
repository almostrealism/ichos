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

package org.almostrealism.audio.data;

import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.audio.sources.SineWaveCellData;
import org.almostrealism.audio.sources.WavCellData;
import org.almostrealism.hardware.MemoryData;

public class PolymorphicAudioData extends ScalarBank implements SineWaveCellData, WavCellData, AudioFilterData {
	public static final int SIZE = 15;

	public PolymorphicAudioData() {
		super(SIZE);
	}

	public PolymorphicAudioData(MemoryData delegate, int delegateOffset) {
		super(SIZE, delegate, delegateOffset, null);
	}
}
