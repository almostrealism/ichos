package org.almostrealism.audio;

import org.almostrealism.audio.feature.computations.FeatureExtractor;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.Tensor;
import uk.co.labbookpages.WavFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceActivityDetector {
	public static void main(String args[]) throws IOException {
		int threads = 3;
		int count = 100 * threads;

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		for (int i = 0; i < count; i++) {
			pool.submit(() -> main(Collections.singletonList(WavFile.openWavFile(
					new File("/Users/michael/CLionProjects/kaldi/test.wav")))));
		};
	}

	static int main(List<WavFile> files) throws IOException {
		List<Tensor<Scalar>> features = new ArrayList<>();
		int stat = FeatureExtractor.main(files, features::add);
		if (stat != 0) return stat;

		try {
			boolean omit_unvoiced_utts = false;
			VadEnergyOptions opts = new VadEnergyOptions();

			int num_done = 0, num_err = 0;
			int num_unvoiced = 0;
			double tot_length = 0.0, tot_decision = 0.0;

			int index = 0;

			for (Tensor<Scalar> feat : features) {
				String utt = String.valueOf(index++);

				if (feat.length() == 0) {
					System.out.println("Empty feature matrix for utterance " + utt);
					num_err++;
					continue;
				}

				ScalarBank vad_result = new ScalarBank(feat.length());

				computeVadEnergy(opts, feat, vad_result);

				double sum = vad_result.sum().getValue();
				if (sum == 0.0) {
					System.out.println("No frames were judged voiced for utterance " + utt);
					num_unvoiced++;
				} else {
					num_done++;
				}
				tot_decision += vad_result.sum().getValue();
				tot_length += vad_result.getCount();

				if (!(omit_unvoiced_utts && sum == 0)) {
					// vad_writer.Write(utt, vad_result);
					// TODO  Output the result
				}
			}

			System.out.println("Applied energy based voice activity detection; "
					+ "processed " + num_done + " utterances successfully; "
					+ num_err + " had empty features, and " + num_unvoiced
					+ " were completely unvoiced.");
			System.out.println("Proportion of voiced frames was "
					+ (tot_decision / tot_length) + " over "
					+ tot_length + " frames.");
			return (num_done != 0 ? 0 : 1);
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	static void computeVadEnergy(VadEnergyOptions opts, Tensor<Scalar> feats,
						  ScalarBank output_voiced) {
		int T = feats.length();
		// output_voiced->Resize(T);

		if (T == 0) {
			System.out.println("Empty features");
			return;
		}

		ScalarBank log_energy = new ScalarBank(T);
		log_energy.copyColFromMat(feats, 0); // column zero is log-energy.

		double energy_threshold = opts.getVad_energy_threshold();
		if (opts.getVad_energy_mean_scale() != 0.0) {
			assert(opts.getVad_energy_mean_scale() > 0.0);
			energy_threshold += opts.getVad_energy_mean_scale() * log_energy.sum().getValue() / T;
		}

		assert(opts.getVad_frames_context() >= 0);
		assert(opts.getVad_proportion_threshold() > 0.0 &&
				opts.getVad_proportion_threshold() < 1.0);

		for (int t = 0; t < T; t++) {
    		// const BaseFloat *log_energy_data = log_energy.Data();
			int num_count = 0, den_count = 0, context = opts.getVad_frames_context();

			for (int t2 = t - context; t2 <= t + context; t2++) {
				if (t2 >= 0 && t2 < T) {
					den_count++;

					if (log_energy.get(t2).getValue() > energy_threshold)
						num_count++;
				}
			}

			if (num_count >= den_count * opts.getVad_proportion_threshold()) {
				output_voiced.set(t, 1.0);
			} else {
				output_voiced.set(t, 0.0);
			}
		}
	}
}
