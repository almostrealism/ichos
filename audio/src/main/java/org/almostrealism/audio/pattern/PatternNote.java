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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.Waves;
import org.almostrealism.audio.data.FileWaveDataProvider;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.TraversalPolicy;

import java.io.File;

public class PatternNote {
	private String source;
	private PackedCollection audio;
	private Boolean valid;

	@JsonIgnore
	private FileWaveDataProvider provider;

	public PatternNote() { this((String) null); }

	public PatternNote(String source) {
		setSource(source);
	}

	public PatternNote(PackedCollection audio) {
		setAudio(audio);
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
		this.valid = null;
	}

	public PackedCollection getAudio(double duration) {
		return getAudio().range(new TraversalPolicy((int) (duration * OutputLine.sampleRate)));
	}

	@JsonIgnore
	public PackedCollection getAudio() {
		if (audio == null) {
			if (provider == null) provider = new FileWaveDataProvider(source);
			audio = provider.get().getCollection();
		}

		return audio;
	}

	@JsonIgnore
	public void setAudio(PackedCollection audio) {
		this.audio = audio;
	}

	public boolean isValid() {
		if (audio != null) return true;
		if (valid != null) return valid;
		valid = Waves.isValid(new File(source), w -> w.getSampleRate() == OutputLine.sampleRate);
		return valid;
	}
}
