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

package org.almostrealism.audio.arrange;

import org.almostrealism.heredity.ConfigurableGenome;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SceneSectionManager {
	private List<SceneSection> sections;
	private ConfigurableGenome genome;
	private int channels;

	public SceneSectionManager(int channels) {
		this.sections = new ArrayList<>();
		this.genome = new ConfigurableGenome();
		this.channels = channels;
	}

	public ConfigurableGenome getGenome() {
		return genome;
	}

	public List<SceneSection> getSections() { return sections; }

	public List<ChannelSection> getChannelSections(int channel) {
		return sections.stream().map(s -> s.getChannelSection(channel)).collect(Collectors.toList());
	}

	public SceneSection addSection(int position, int length) {
		SceneSection s = SceneSection.createSection(position, length, channels, () -> DefaultChannelSection.createSection(position, length, genome));
		sections.add(s);
		return s;
	}
}
