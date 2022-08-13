package org.almostrealism.audio.filter.test;

import io.almostrealism.relation.Evaluable;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.audio.WavFile;
import org.almostrealism.audio.WaveOutput;
import org.almostrealism.audio.filter.AudioPassFilter;
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

		ScalarBank values = WavFile.channel(data, 0);
		ScalarBank out = new ScalarBank(values.getCount());
		Scalar current = new Scalar();

		AudioPassFilter filter = new AudioPassFilter((int) f.getSampleRate(), v(2000), v(0.1), true);
		Evaluable<Scalar> ev = filter.getResultant(p(current)).get();
		Runnable tick = filter.tick().get();

		for (int i = 0; i < values.getCount(); i++) {
			current.setValue(values.get(i).getValue());
			out.set(i, ev.evaluate());
			tick.run();
		}

		WavFile wav = WavFile.newWavFile(new File("results/filter-test.wav"), 1, out.getCount(),
				f.getValidBits(), f.getSampleRate());

		for (int i = 0; i < out.getCount(); i++) {
			double value = out.get(i).getValue();

			try {
				wav.writeFrames(new double[][]{{value}}, 1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		wav.close();
	}
}
