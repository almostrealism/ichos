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

package org.almostrealism.audio.pattern;

import org.almostrealism.collect.PackedCollection;

public class PatternNote {
	private PackedCollection audio;

	public PatternNote() { this(null); }

	public PatternNote(PackedCollection audio) {
		setAudio(audio);
	}

	public PackedCollection getAudio() {
		return audio;
	}

	public void setAudio(PackedCollection audio) {
		this.audio = audio;
	}
}
