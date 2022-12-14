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

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SceneSection {
	private int position, length;
	private List<ChannelSection> channels;

	protected SceneSection(int position, int length, List<ChannelSection> channels) {
		this.position = position;
		this.length = length;
		this.channels = channels;
	}

	public int getPosition() { return position; }
	public int getLength() { return length; }

	public ChannelSection getChannelSection(int channel) { return channels.get(channel); }

	public static SceneSection createSection(int position, int length, int channels, Supplier<ChannelSection> supply) {
		return new SceneSection(position, length, IntStream.range(0, channels).mapToObj(i -> supply.get()).collect(Collectors.toList()));
	}
}
