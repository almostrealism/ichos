package org.almostrealism.audio.feature;

import org.almostrealism.audio.WavFile;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.Tensor;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

public class FeatureExtractor {
	private static final ThreadLocal<FeatureComputer> computers = new ThreadLocal<>();

	public static void main(String args[]) {
		ExecutorService executor = Executors.newFixedThreadPool(8);

		IntStream.range(0, 10).mapToObj(i -> (Runnable) () -> {
			try {
				main(
						Collections.singletonList(WavFile.openWavFile(
								new File("/Users/michael/CLionProjects/kaldi/test-16khz.wav"))),
						FeatureExtractor::print);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).forEach(executor::submit);
	}

	public static int main(List<WavFile> files, Consumer<Tensor<Scalar>> output) throws IOException {
		FeatureComputer mfcc = computers.get();

		if (mfcc == null) {
			FeatureSettings settings = new FeatureSettings();
			mfcc = new FeatureComputer(settings);
			computers.set(mfcc);
		}

		double vtlnWarp = 1.0;
		Scalar minDuration = new Scalar(0.0);

		int index = 0;
		int uttCount = 0, successCount = 0;
		for (WavFile f : files) {
			uttCount++;
			String utt = String.valueOf(index++);

			if (f.getDuration() < minDuration.getValue()) {
				System.out.println(utt + " is too short (" +
						f.getDuration() + " sec): producing no output.");
				continue;
			}

			int[][] wave = new int[f.getNumChannels()][(int) f.getFramesRemaining()];
			f.readFrames(wave, 0, (int) f.getFramesRemaining());

			int channelCount = f.getNumChannels();

			assert channelCount > 0;
			int channel = 0;

			ScalarBank waveform = WavFile.channel(wave, channel);
			Tensor<Scalar> features = new Tensor<>();

			try {
				mfcc.computeFeatures(waveform, new Scalar(f.getSampleRate()), vtlnWarp, features);
			} catch (Exception e) {
				System.out.println("Failed to compute features for utterance " + utt);
				e.printStackTrace();
				continue;
			}

			output.accept(features);

			if (uttCount % 10 == 0)
				System.out.println("Processed " + uttCount + " utterances");
			System.out.println("Processed features for key " + utt);
			successCount++;
		}

		System.out.println(" Done " + successCount + " out of " + uttCount + " utterances.");
		return successCount != 0 ? 0 : 1;
	}

	public static void print(Tensor t) { System.out.println(t.toHTML()); }
}
