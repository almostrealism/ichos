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
import io.almostrealism.code.CachedValue;
import io.almostrealism.relation.Evaluable;
import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.WaveOutput;
import org.almostrealism.audio.Waves;
import org.almostrealism.audio.data.FileWaveDataProvider;
import org.almostrealism.audio.data.WaveData;
import org.almostrealism.audio.tone.KeyPosition;
import org.almostrealism.audio.tone.KeyboardTuning;
import org.almostrealism.audio.tone.WesternChromatic;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.TraversalPolicy;
import org.almostrealism.hardware.KernelizedEvaluable;
import org.almostrealism.hardware.PassThroughProducer;
import org.almostrealism.hardware.ctx.ContextSpecific;
import org.almostrealism.hardware.ctx.DefaultContextSpecific;
import org.almostrealism.time.Frequency;
import org.almostrealism.time.computations.Interpolate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PatternNote {
	private static ContextSpecific<KernelizedEvaluable<PackedCollection>> interpolate;

	static {
		interpolate = new DefaultContextSpecific<>(() ->
				new Interpolate(
						new PassThroughProducer<>(-1, 0),
						new PassThroughProducer<>(1, 1),
						new PassThroughProducer<>(2, 2)).get());
	}

	private String source;
	private PackedCollection audio;
	private Boolean valid;
	private KeyPosition<?> root;

	@JsonIgnore
	private FileWaveDataProvider provider;

	private KeyboardTuning tuning;
	private Map<KeyPosition, CachedValue<PackedCollection>> notes;

	public PatternNote() { this((String) null); }

	public PatternNote(String source) {
		this(source, WesternChromatic.C1);
	}

	public PatternNote(PackedCollection audio) {
		this(audio, WesternChromatic.C1);
	}

	public PatternNote(String source, KeyPosition root) {
		setSource(source);
		setRoot(root);
		notes = new HashMap<>();
	}

	public PatternNote(PackedCollection audio, KeyPosition root) {
		setAudio(audio);
		setRoot(root);
		notes = new HashMap<>();
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
		this.valid = null;
	}

	public KeyPosition<?> getRoot() { return root; }

	public void setRoot(KeyPosition<?> root) { this.root = root; }

	@JsonIgnore
	public void setTuning(KeyboardTuning tuning) {
		if (tuning != this.tuning) {
			this.tuning = tuning;
			notes.clear();
		}
	}

	public Producer<PackedCollection> getAudio(KeyPosition<?> target, double duration) {
		return () -> {
			Evaluable<PackedCollection> audio = getAudio(target).get();
			return args -> audio.evaluate().range(new TraversalPolicy((int) (duration * OutputLine.sampleRate)));
		};
	}

	public PackedCollection getAudio(double duration) {
		return getAudio().range(new TraversalPolicy((int) (duration * OutputLine.sampleRate)));
	}

	public CachedValue<PackedCollection> getAudio(KeyPosition<?> target) {
		if (!notes.containsKey(target)) {
			notes.put(target, new CachedValue<>(args -> {
				double r = tuning.getTone(target).asHertz() / tuning.getTone(getRoot()).asHertz();

				PackedCollection rate = new PackedCollection(1);
				rate.setMem(0, r);

				PackedCollection audio = getAudio();
				PackedCollection dest = WaveData.allocateCollection((int) (r * audio.getMemLength()));

				interpolate.getValue().kernelEvaluate(dest.traverse(1), audio.traverse(0), WaveOutput.timeline.getValue(), rate.traverse(0));
				return dest;
			}));
		}

		return notes.get(target);
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
