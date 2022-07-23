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

package org.almostrealism.audio.pattern.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.data.ParameterFunction;
import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.audio.data.WaveData;
import org.almostrealism.audio.pattern.PatternElementFactory;
import org.almostrealism.audio.pattern.PatternFactoryChoice;
import org.almostrealism.audio.pattern.PatternFactoryChoiceList;
import org.almostrealism.audio.pattern.PatternLayerManager;
import org.almostrealism.audio.pattern.PatternNote;
import org.almostrealism.audio.tone.DefaultKeyboardTuning;
import org.almostrealism.audio.tone.Scale;
import org.almostrealism.audio.tone.WesternChromatic;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.collect.PackedCollectionHeap;
import org.almostrealism.collect.ProducerWithOffset;
import org.almostrealism.collect.computations.RootDelegateSegmentsAdd;
import org.almostrealism.hardware.cl.HardwareOperator;
import org.almostrealism.time.Frequency;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PatternFactoryTest implements CellFeatures {

	@Test
	public void fixChoices() throws IOException {
		List<PatternFactoryChoice> choices = readChoices();

		choices.forEach(choice -> {
			choice.getFactory().setNoteLengthSelection(ParameterFunction.random());
		});

//		new ObjectMapper().writeValue(new File("pattern-factory.json"), choices);
	}

	public List<PatternFactoryChoice> createChoices() {
		List<PatternFactoryChoice> choices = new ArrayList<>();

		PatternFactoryChoice kick = new PatternFactoryChoice(new PatternElementFactory("Kicks", new PatternNote("Kit/Kick.wav")));
		kick.setMinScale(0.25);
		choices.add(kick);

		PatternFactoryChoice clap = new PatternFactoryChoice(new PatternElementFactory("Clap/Snare", new PatternNote("Kit/Clap.wav")));
		clap.setMaxScale(0.5);
		choices.add(clap);

		PatternFactoryChoice toms = new PatternFactoryChoice(
				new PatternElementFactory("Toms", new PatternNote("Kit/Tom1.wav"),
						new PatternNote("Kit/Tom2.wav")));
		toms.setMaxScale(0.25);
		choices.add(toms);

		PatternFactoryChoice hats = new PatternFactoryChoice(new PatternElementFactory("Hats"));
		hats.setMaxScale(0.25);
		choices.add(hats);

		return choices;
	}

	public PatternFactoryChoiceList readChoices() throws IOException {
		return new ObjectMapper().readValue(new File("pattern-factory.json"), PatternFactoryChoiceList.class);
	}

	@Test
	public void storeChoices() throws IOException {
		new ObjectMapper().writeValue(new File("pattern-factory.json"), createChoices());
	}

	@Test
	public void sum() throws IOException {
		HardwareOperator.enableLog = true;
		HardwareOperator.enableVerboseLog = true;
		WaveData.setCollectionHeap(() -> new PackedCollectionHeap(600 * OutputLine.sampleRate), PackedCollectionHeap::destroy);

		WaveData kick = WaveData.load(new File("Kit/Kick.wav"));

		Frequency bpm = bpm(120);
		PackedCollection destination = new PackedCollection((int) (bpm.l(16) * OutputLine.sampleRate));
		RootDelegateSegmentsAdd<PackedCollection> op = new RootDelegateSegmentsAdd<>(List.of(new ProducerWithOffset<>(v(kick.getCollection()), 0)), destination);
		op.get().run();

		WaveData out = new WaveData(destination, OutputLine.sampleRate);
		out.save(new File("sum-test.wav"));
	}

	@Test
	public void runLayers() throws IOException {
		Frequency bpm = bpm(120);

		WaveData.setCollectionHeap(() -> new PackedCollectionHeap(600 * OutputLine.sampleRate), PackedCollectionHeap::destroy);
		PackedCollection destination = new PackedCollection((int) (bpm.l(16) * OutputLine.sampleRate));

		PatternLayerManager manager = new PatternLayerManager(readChoices(), false, destination);
		manager.setTuning(new DefaultKeyboardTuning());

		System.out.println(PatternLayerManager.layerHeader());
		System.out.println(PatternLayerManager.layerString(manager.getTailElements()));

		for (int i = 0; i < 4; i++) {
			manager.addLayer(new ParameterSet(0.6, 0.2, 0.7));
			System.out.println(PatternLayerManager.layerString(manager.getTailElements()));
		}

		manager.sum(pos -> (int) (pos * bpm.l(16) * OutputLine.sampleRate), Scale.of(WesternChromatic.C1));

		WaveData out = new WaveData(destination, OutputLine.sampleRate);
		out.save(new File("pattern-test.wav"));
	}
}
