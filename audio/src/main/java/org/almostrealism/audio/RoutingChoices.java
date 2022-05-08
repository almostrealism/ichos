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

package org.almostrealism.audio;

import java.util.ArrayList;
import java.util.List;

public class RoutingChoices {
//	private int sourceCount, delayCount;
	private List<Integer> choices;

	public RoutingChoices() { setChoices(new ArrayList<>()); }

//	public RoutingChoices(int sourceCount, int delayCount) {
//		setSourceCount(sourceCount);
//		setDelayCount(delayCount);
//		setChoices(new ArrayList<>());
//	}

//	public int getSourceCount() { return sourceCount; }
//	public void setSourceCount(int sourceCount) { this.sourceCount = sourceCount; }
//
//	public int getDelayCount() { return delayCount; }
//	public void setDelayCount(int delayCount) { this.delayCount = delayCount; }

	public List<Integer> getChoices() { return choices; }
	public void setChoices(List<Integer> choices) { this.choices = choices; }
}