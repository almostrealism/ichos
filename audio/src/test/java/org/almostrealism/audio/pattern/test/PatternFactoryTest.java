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
import org.almostrealism.audio.data.ParameterSet;
import org.almostrealism.audio.pattern.PatternElement;
import org.almostrealism.audio.pattern.PatternElementFactory;
import org.almostrealism.audio.pattern.PatternFactoryChoice;
import org.almostrealism.audio.pattern.PatternFactoryLayer;
import org.almostrealism.audio.pattern.PatternFactoryNode;
import org.almostrealism.audio.pattern.PatternLayerManager;
import org.almostrealism.audio.pattern.PatternNote;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class PatternFactoryTest {

	public PatternFactoryNode createNodes() {
		PatternFactoryNode kick = new PatternFactoryNode(new PatternElementFactory(new PatternNote("0")));
		kick.setSelfWeight(10);
		kick.setMinimumScale(0.5);

		PatternFactoryNode clap = new PatternFactoryNode(new PatternElementFactory(new PatternNote("1")));
		kick.getChoices().add(new PatternFactoryChoice(clap));

		PatternFactoryNode toms = new PatternFactoryNode(
				new PatternElementFactory(new PatternNote("2"),
						new PatternNote("3")));
		clap.getChoices().add(new PatternFactoryChoice(toms));

		return kick;
	}

	public PatternFactoryNode readNodes() throws IOException {
		return new ObjectMapper().readValue(new File("pattern-factory.json"), PatternFactoryNode.class);
	}

	@Test
	public void storeNodes() throws IOException {
		new ObjectMapper().writeValue(new File("pattern-factory.json"), createNodes());
	}

	@Test
	public void runLayers() throws IOException {
		PatternLayerManager manager = new PatternLayerManager(readNodes());

		System.out.println(layerHeader());

		System.out.println(layerString(manager.lastLayer()));

		for (int i = 0; i < 4; i++) {
			manager.addLayer(new ParameterSet(0.6, 0.2, 0.7));
			System.out.println(layerString(manager.lastLayer()));
		}

		System.out.println("Produced " + manager.layerCount() + " layers");
	}

	private String layerHeader() {
		int count = 128;
		int divide = count / 4;

		StringBuffer buf = new StringBuffer();

		i: for (int i = 0; i < count; i++) {
			if (i % (divide / 2) == 0) {
				if (i % divide == 0) {
					buf.append("|");
				} else {
					buf.append(" ");
				}
			}

			buf.append(" ");
		}

		buf.append("|");
		return buf.toString();
	}

	private String layerString(PatternFactoryLayer layer) {
		int count = 128;
		int divide = count / 8;
		double scale = 1.0 / count;

		StringBuffer buf = new StringBuffer();

		i: for (int i = 0; i < count; i++) {
			if (i % divide == 0) buf.append("|");
			for (PatternElement e : layer.getElements()) {
				if (e.getPosition() >= i * scale && e.getPosition() < (i + 1) * scale) {
					buf.append(e.getNote().getSource());
					continue i;
				}
			}
			buf.append(" ");
		}

		buf.append("|");
		return buf.toString();
	}
}
