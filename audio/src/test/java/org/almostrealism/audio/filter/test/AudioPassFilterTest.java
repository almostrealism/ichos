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

package org.almostrealism.audio.filter.test;

import io.almostrealism.relation.Evaluable;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.audio.WavFile;
import org.almostrealism.audio.WaveOutput;
import org.almostrealism.audio.filter.AudioPassFilter;
import org.almostrealism.collect.PackedCollection;
import org.almostrealism.util.TestFeatures;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class AudioPassFilterTest implements TestFeatures {
	@Test
	public void highPass() throws IOException {
		WavFile f = WavFile.openWavFile(new File("src/test/resources/Snare Perc DD.wav"));

		double data[][] = new double[f.getNumChannels()][(int) f.getFramesRemaining()];
		f.readFrames(data, (int) f.getFramesRemaining());

		PackedCollection<?> values = WavFile.channel(data, 0);
		PackedCollection<?> out = new PackedCollection<>(values.getCount());
		Scalar current = new Scalar();

		AudioPassFilter filter = new AudioPassFilter((int) f.getSampleRate(), c(2000), v(0.1), true);
		Evaluable<PackedCollection<?>> ev = filter.getResultant(p(current)).get();
		Runnable tick = filter.tick().get();

		for (int i = 0; i < values.getCount(); i++) {
			current.setValue(values.get(i).toDouble(0));
			out.set(i, ev.evaluate().toDouble(0));
			tick.run();
		}

		WavFile wav = WavFile.newWavFile(new File("results/filter-test.wav"), 1, out.getCount(),
				f.getValidBits(), f.getSampleRate());

		for (int i = 0; i < out.getCount(); i++) {
			double value = out.get(i).toDouble(0);

			try {
				wav.writeFrames(new double[][]{{value}}, 1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		wav.close();
	}
}
