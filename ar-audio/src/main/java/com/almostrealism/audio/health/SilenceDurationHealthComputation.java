/*
 * Copyright 2016 Michael Murray
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

package com.almostrealism.audio.health;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.AudioMeter;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.graph.CellAdapter;
import org.almostrealism.organs.Organ;
import org.almostrealism.organs.SimpleOrgan;

import java.util.ArrayList;
import java.util.List;

public class SilenceDurationHealthComputation extends HealthComputationAdapter {
	public static boolean enableVerbose = false;
	
	private int maxSilence;
	private double silenceValue = 0.001; // Lowest permissable volume

	private long max = standardDuration;

	private AudioMeter meter;
	private List<Runnable> silenceListeners;
	
	public SilenceDurationHealthComputation() {
		this(2);
	}
	

	public SilenceDurationHealthComputation(int maxSilenceSec) {
		setMaxSilence(maxSilenceSec);
		silenceListeners = new ArrayList<>();
	}
	
	public void setMaxSilence(int sec) { this.maxSilence = (int) (sec * OutputLine.sampleRate); }
	
	public void setStandardDuration(int sec) {
		this.standardDuration = (int) (sec * OutputLine.sampleRate);
	}

	public void addSilenceListener(Runnable listener) { silenceListeners.add(listener); }

	public AudioMeter getMeter() {
		AudioMeter meter = super.getMeter();
//		meter.setTextOutputEnabled(false);
//		meter.setReportingFrequency(100);
		meter.setSilenceValue(silenceValue);
		return meter;
	}

	public boolean checkForSilence(AudioMeter meter) {
		if (meter.getSilenceDuration() > maxSilence) {
			silenceListeners.forEach(Runnable::run);
			return true;
		}

		return false;
	}

	public double computeHealth(Organ<Scalar> organ) {
		super.init();
		
		AudioMeter meter = getMeter();
		
		((CellAdapter<Scalar>) ((SimpleOrgan) organ).lastCell()).setMeter(meter);
		
		setReceptor(organ);
		
		long l;

		Runnable push = push().get();
		Runnable tick = organ.tick().get();
		
		l: for (l = 0; l < max; l++) {
			push.run();
			
			// If silence occurs for too long, report the health score
			if (checkForSilence(meter)) {
				return ((double) l) / standardDuration;
			}
			
			tick.run();
		}
		
		// Report the health score as an inverse
		// percentage of the expected duration
		if (enableVerbose)
			System.out.println("SilenceDurationHealthComputation: " + l + " frames of survival");
		
		// If no silence which was too long in duration
		// has occurred, return a perfect health score.
		return 1.0;
	}
}
