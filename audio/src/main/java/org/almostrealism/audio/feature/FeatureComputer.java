package org.almostrealism.audio.feature;

import org.almostrealism.algebra.computations.jni.NativePowerSpectrum512;
import org.almostrealism.audio.computations.ComplexFFT;
import org.almostrealism.audio.util.TensorRow;
import io.almostrealism.relation.Evaluable;
import org.almostrealism.algebra.PairBank;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.Tensor;
import org.almostrealism.algebra.computations.Dither;
import org.almostrealism.algebra.computations.PowerSpectrum;
import org.almostrealism.algebra.computations.Preemphasize;
import org.almostrealism.algebra.computations.ScalarBankAdd;
import org.almostrealism.algebra.computations.ScalarBankPad;
import org.almostrealism.algebra.computations.ScalarBankSum;
import org.almostrealism.audio.computations.SplitRadixFFT;
import org.almostrealism.hardware.Hardware;
import org.almostrealism.util.CodeFeatures;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class FeatureComputer implements CodeFeatures {
	private static final double epsilon = 0.00000001;

	private final FeatureSettings settings;
	private final FeatureWindowFunction featureWindowFunction;

	private final ComplexFFT fft;

	private final Evaluable<? extends ScalarBank> processWindow;
	private final Evaluable<? extends ScalarBank> preemphasizeAndWindowFunctionAndPad;

	private final Evaluable<ScalarBank> powerSpectrum;

	private ScalarBank lifterCoeffs;
	private final Tensor<Scalar> dctMatrix;  // matrix we left-multiply by to perform DCT.
	private Scalar logEnergyFloor;
	private final Map<Double, MelBanks> allMelBanks;  // BaseFloat is VTLN coefficient.

	private final ScalarBank melEnergies;

	public FeatureComputer(FeatureSettings settings) {
		this.settings = settings;
		this.featureWindowFunction = new FeatureWindowFunction(settings.getFrameExtractionSettings());
		this.allMelBanks = new HashMap<>();

		int binCount = this.settings.getMelBanksSettings().getNumBins();
		this.melEnergies = new ScalarBank(binCount);

		if (this.settings.getNumCeps() > binCount)
			System.err.println("num-ceps cannot be larger than num-mel-bins."
					+ " It should be smaller or equal. You provided num-ceps: "
					+ settings.getNumCeps() + "  and num-mel-bins: "
					+ binCount);

		dctMatrix = new Tensor<>();
		computeDctMatrix(dctMatrix, binCount, binCount);

		// Note that we include zeroth dct in either case.  If using the
		// energy we replace this with the energy.  This means a different
		// ordering of features than HTK.
		dctMatrix.trim(this.settings.getNumCeps(), binCount);
		if (this.settings.getCepstralLifter().getValue() != 0.0) {
			lifterCoeffs = new ScalarBank(this.settings.getNumCeps());
			computeLifterCoeffs(this.settings.getCepstralLifter().getValue(), lifterCoeffs);
		}
		if (this.settings.getEnergyFloor().getValue() > 0.0)
			logEnergyFloor = new Scalar(Math.log(this.settings.getEnergyFloor().getValue()));

		int paddedWindowSize = this.settings.getFrameExtractionSettings().getPaddedWindowSize();
		fft = new ComplexFFT(paddedWindowSize, true, v(2 * paddedWindowSize, 0));

		int count = settings.getFrameExtractionSettings().getWindowSize();
		Supplier<Evaluable<? extends ScalarBank>> processWindow = v(2 * count, 0);

		processWindow = new Dither(count, processWindow, p(settings.getFrameExtractionSettings().getDither()));

		if (settings.getFrameExtractionSettings().isRemoveDcOffset()) {
			processWindow = new ScalarBankAdd(count, processWindow, new ScalarBankSum(count, processWindow).divide(count).multiply(-1));
		}

		Supplier<Evaluable<? extends ScalarBank>> preemphasizeAndWindowFunctionAndPad = v(2 * count, 0);

		if (settings.getFrameExtractionSettings().getPreemphCoeff().getValue() != 0.0) {
			preemphasizeAndWindowFunctionAndPad = new Preemphasize(settings.getFrameExtractionSettings().getWindowSize(),
					preemphasizeAndWindowFunctionAndPad,
					v(settings.getFrameExtractionSettings().getPreemphCoeff()));
		}

		preemphasizeAndWindowFunctionAndPad = featureWindowFunction.getWindow(preemphasizeAndWindowFunctionAndPad);
		preemphasizeAndWindowFunctionAndPad = new ScalarBankPad(settings.getFrameExtractionSettings().getWindowSize(),
								settings.getFrameExtractionSettings().getPaddedWindowSize(),
								preemphasizeAndWindowFunctionAndPad);

		this.processWindow = processWindow.get();
		this.preemphasizeAndWindowFunctionAndPad = preemphasizeAndWindowFunctionAndPad.get();

		if (paddedWindowSize == 512 && Hardware.getLocalHardware().isNativeSupported()) {
			this.powerSpectrum = new NativePowerSpectrum512().get();
		} else {
			if (Hardware.getLocalHardware().isNativeSupported())
				System.out.println("WARN: Unable to use NativePowerSpectrum with window " + paddedWindowSize);
			this.powerSpectrum = new PowerSpectrum(paddedWindowSize, v(2 * paddedWindowSize, 0)).get();
		}

		// We'll definitely need the filterbanks info for VTLN warping factor 1.0.
		// [note: this call caches it.]
		getMelBanks(1.0);
	}

	protected void computeDctMatrix(Tensor<Scalar> M, int K, int N) {
//		MatrixIndexT K = M->NumRows();
//		MatrixIndexT N = M->NumCols();

		assert K > 0;
		assert N > 0;
		Scalar normalizer = new Scalar(Math.sqrt(1.0 / N));  // normalizer for
															 // X.0.
		for (int j = 0; j < N; j++) M.insert(normalizer, 0, j);
		normalizer = new Scalar(Math.sqrt(2.0 / N));  // normalizer for other
													  // elements.
		for (int k = 1; k < K; k++)
			for (int n = 0; n < N; n++)
				M.insert(new Scalar(normalizer.getValue() * Math.cos(Math.PI /N * (n + 0.5) * k)), k, n);
	}

	protected void computeLifterCoeffs(double Q, ScalarBank coeffs) {
		// Compute liftering coefficients (scaling on cepstral coeffs)
		// coeffs are numbered slightly differently from HTK: the zeroth
		// index is C0, which is not affected.
		for (int i = 0; i < coeffs.getCount(); i++)
			coeffs.set(i, 1.0 + 0.5 * Q * Math.sin(Math.PI * i / Q));
	}

	public void computeFeatures(ScalarBank wave,
								Scalar sampleFreq,
								Tensor<Scalar> output) {
		computeFeatures(wave, sampleFreq, 1.0, output);
	}

	public void computeFeatures(ScalarBank wave,
								Scalar sampleFreq,
								double vtlnWarp,
								Tensor<Scalar> output) {
		Scalar newSampleFreq = settings.getFrameExtractionSettings().getSampFreq();
		if (sampleFreq.getValue() == newSampleFreq.getValue()) {
			compute(wave, vtlnWarp, output);
		} else {
			if (newSampleFreq.getValue() < sampleFreq.getValue() &&
					!settings.getFrameExtractionSettings().isAllowDownsample())
				System.err.println("Waveform and config sample Frequency mismatch: "
						+ sampleFreq + " .vs " + newSampleFreq
						+ " (use --allow-downsample=true to allow "
						+ " downsampling the waveform).");
			else if (newSampleFreq.getValue() > sampleFreq.getValue() &&
					!settings.getFrameExtractionSettings().isAllowUpsample())
				System.err.println("Waveform and config sample Frequency mismatch: "
						+ sampleFreq + " .vs " + newSampleFreq
						+ " (use --allow-upsample=true option to allow "
						+ " upsampling the waveform).");
			// Resample the waveform.
			ScalarBank resampledWave = new ScalarBank(wave.getCount());
			Resampler.resampleWaveform(sampleFreq, wave,
					newSampleFreq, resampledWave);
			compute(resampledWave, vtlnWarp, output);
		}
	}

	protected void compute(ScalarBank wave, double vtlnWarp, Tensor<Scalar> output) {
		int rowsOut = numFrames(wave.getCount(), settings.getFrameExtractionSettings(), false);

		if (rowsOut == 0) {
			return;
		}

		boolean useRawLogEnergy = settings.isNeedRawLogEnergy();
		for (int r = 0; r < rowsOut; r++) {  // r is frame index.
			ScalarBank window = new ScalarBank(settings.getFrameExtractionSettings().getPaddedWindowSize());  // windowed waveform.
			Scalar rawLogEnergy = new Scalar(0.0);
			window = extractWindow(0, wave, r, settings.getFrameExtractionSettings(),
					featureWindowFunction, window,
					useRawLogEnergy ? rawLogEnergy : null);

			TensorRow outputRow = new TensorRow(output, r);
			long start = System.currentTimeMillis();
			compute(rawLogEnergy, vtlnWarp, window, outputRow);
			System.out.println("-----> " + (System.currentTimeMillis() - start) + " total");
		}
	}

	protected void compute(Scalar signalRawLogEnergy,
						double vtlnWarp,
						ScalarBank realSignalFrame,
						TensorRow<Scalar> feature) {
		assert realSignalFrame.getCount() == settings.getFrameExtractionSettings().getPaddedWindowSize();

		MelBanks melBanks = getMelBanks(vtlnWarp);

		if (settings.isUseEnergy() && !settings.isRawEnergy())
			signalRawLogEnergy.setValue(Math.log(Math.max(Resampler.vecVec(realSignalFrame, realSignalFrame).getValue(), epsilon)));

		long start = System.currentTimeMillis();

		PairBank signalFrame;

		if (fft != null) {  // Compute FFT using the split-radix algorithm.
			signalFrame = fft.evaluate(toPairBank(realSignalFrame));
		} else {
			throw new UnsupportedOperationException();
		}

		System.out.println("--> FFT: " + (System.currentTimeMillis() - start));

		// Convert the FFT into a power spectrum.
		start = System.currentTimeMillis();
		ScalarBank powerSpectrum = this.powerSpectrum.evaluate(signalFrame).range(0, signalFrame.getCount() / 2 + 1);
		System.out.println("--> computePowerSpectrum: " + (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		melBanks.compute(powerSpectrum, melEnergies);
		System.out.println("--> melBanks: " + (System.currentTimeMillis() - start));

		// avoid log of zero (which should be prevented anyway by dithering).
		start = System.currentTimeMillis();
		melEnergies.applyFloor(FeatureComputer.epsilon);
		melEnergies.applyLog();  // take the log.
		System.out.println("--> applyLog: " + (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		feature.setZero();  // in case there were NaNs.
		feature.addMatVec(dctMatrix, melEnergies);
		System.out.println("--> dctMatrix: " + (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		if (settings.getCepstralLifter().getValue() != 0.0)
			feature.mulElements(lifterCoeffs);
		System.out.println("--> lifterCoeffs: " + (System.currentTimeMillis() - start));

		// TODO 12
		if (settings.isUseEnergy()) {
			if (settings.getEnergyFloor().getValue() > 0.0 &&
					signalRawLogEnergy.getValue() < logEnergyFloor.getValue())
				signalRawLogEnergy = logEnergyFloor;
			feature.set(0, signalRawLogEnergy);
		}

		if (settings.isHtkCompat()) {
			double energy = feature.get(0).getValue();
			for (int i = 0; i < settings.getNumCeps() - 1; i++)
				feature.set(i, feature.get(i + 1));

			if (!settings.isUseEnergy())
				energy *= SplitRadixFFT.SQRT_2;  // scale on C0 (actually removing a scale
			// we previously added that's part of one common definition of
			// the cosine transform.)
			feature.set(settings.getNumCeps() - 1, new Scalar(energy));
		}
	}

	private int numFrames(long numSamples, FrameExtractionSettings opts) {
		return numFrames(numSamples, opts, true);
	}

	private int numFrames(long numSamples, FrameExtractionSettings opts, boolean flush) {
		long frameShift = opts.getWindowShift();
		long frameLength = opts.getWindowSize();
		if (opts.isSnipEdges()) {
			// with --snip-edges=true (the default), we use a HTK-like approach to
			// determining the number of frames-- all frames have to fit completely into
			// the waveform, and the first frame begins at sample zero.
			if (numSamples < frameLength)
				return 0;
			else
				return (int) (1 + (numSamples - frameLength) / frameShift);
			// You can understand the expression above as follows: 'numSamples -
			// frameLength' is how much room we have to shift the frame within the
			// waveform; 'frameShift' is how much we shift it each time; and the ratio
			// is how many times we can shift it (integer arithmetic rounds down).
		} else {
			// if --snip-edges=false, the number of frames is determined by rounding the
			// (file-length / frame-shift) to the nearest integer.  The point of this
			// formula is to make the number of frames an obvious and predictable
			// function of the frame shift and signal length, which makes many
			// segmentation-related questions simpler.
			//
			// Because integer division in C++ rounds toward zero, we add (half the
			// frame-shift minus epsilon) before dividing, to have the effect of
			// rounding towards the closest integer.
			int numFrames = (int) ((numSamples + frameShift / 2) / frameShift);

			if (flush)
				return numFrames;

			// note: 'end' always means the last plus one, i.e. one past the last.
			long endSampleOfLastFrame = firstSampleOfFrame(numFrames - 1, opts) + frameLength;

			// the following code is optimized more for clarity than efficiency.
			// If flush == false, we can't output frames that extend past the end
			// of the signal.
			while (numFrames > 0 && endSampleOfLastFrame > numSamples) {
				numFrames--;
				endSampleOfLastFrame -= frameShift;
			}
			return numFrames;
		}
	}

	long firstSampleOfFrame(int frame, FrameExtractionSettings opts) {
		long frameShift = opts.getWindowShift();

		if (opts.isSnipEdges()) {
			return frame * frameShift;
		} else {
			long midpointOfFrame = frameShift * frame + frameShift / 2;
			return midpointOfFrame - opts.getWindowSize() / 2;
		}
	}

	// ExtractWindow extracts a windowed frame of waveform with a power-of-two,
	// padded size.  It does mean subtraction, pre-emphasis and dithering as
	// requested.
	ScalarBank extractWindow(long sampleOffset, ScalarBank wave,
					   int f,  // with 0 <= f < NumFrames(feats, opts)
					   FrameExtractionSettings opts,
					   FeatureWindowFunction windowFunction,
					   ScalarBank window,
					   Scalar logEnergyPreWindow) {
		assert sampleOffset >= 0 && wave.getCount() != 0;
		int frameLength = opts.getWindowSize();
		int frameLengthPadded = opts.getPaddedWindowSize();
		long numSamples = sampleOffset + wave.getCount(),
				startSample = firstSampleOfFrame(f, opts);

		if (opts.isSnipEdges()) {
			long endSample = startSample + frameLength;
			assert startSample >= sampleOffset &&
					endSample <= numSamples;
		} else {
			assert sampleOffset == 0 || startSample >= sampleOffset;
		}

		// waveStart and waveEnd are start and end indexes into 'wave', for the
		// piece of wave that we're trying to extract.
		int waveStart = (int) (startSample - sampleOffset);
		int waveEnd = waveStart + frameLength;
		if (waveStart >= 0 && waveEnd <= wave.getCount()) {
			// the normal case-- no edge effects to consider.
//			window->Range(0, frameLength).CopyFromVec(
//					wave.Range(waveStart, frameLength));
			for (int i = 0; i < frameLength; i++) {
				window.set(i, wave.get(waveStart + i));
			}
		} else {
			// Deal with any end effects by reflection, if needed.  This code will only
			// be reached for about two frames per utterance, so we don't concern
			// ourselves excessively with efficiency.
			int waveDim = wave.getCount();
			for (int s = 0; s < frameLength; s++) {
				int sInWave = s + waveStart;
				while (sInWave < 0 || sInWave >= waveDim) {
					// reflect around the beginning or end of the wave.
					// e.g. -1 -> 0, -2 -> 1.
					// dim -> dim - 1, dim + 1 -> dim - 2.
					// the code supports repeated reflections, although this
					// would only be needed in pathological cases.
					if (sInWave < 0) sInWave = -sInWave - 1;
					else sInWave = 2 * waveDim - 1 - sInWave;
				}

				window.set(s, wave.get(sInWave));
			}
		}

		ScalarBank frame = window;
		if (frameLengthPadded > frameLength) frame = frame.range(0, frameLength);

		return processWindow(opts, frame, logEnergyPreWindow);
	}

	ScalarBank processWindow(FrameExtractionSettings opts,
					   ScalarBank window,
					   Scalar logEnergyPreWindow) {
		long start = System.currentTimeMillis();

		int frameLength = opts.getWindowSize();
		assert window.getCount() == frameLength;

		window = processWindow.evaluate(window);

		if (logEnergyPreWindow != null) {
			double energy = Math.max(Resampler.vecVec(window, window).getValue(), epsilon);
			logEnergyPreWindow.setValue(Math.log(energy));
		}

		window = preemphasizeAndWindowFunctionAndPad.evaluate(window);

		System.out.println("--> processWindow: " + (System.currentTimeMillis() - start));

		return window;
	}

	private MelBanks getMelBanks(double vtlnWarp) {
		MelBanks melBanks;
		MelBanks val = allMelBanks.get(vtlnWarp);
		if (val == null) {
			melBanks = new MelBanks(settings.getMelBanksSettings(),
					settings.getFrameExtractionSettings(),
					new Scalar(vtlnWarp));
			allMelBanks.put(vtlnWarp, melBanks);
		} else {
			return val;
		}

		return melBanks;
	}

	static PairBank toPairBank(ScalarBank real) {
		PairBank p = new PairBank(real.getCount());
		IntStream.range(0, real.getCount()).forEach(i -> p.set(i, real.get(i).getValue(), 0.0));
		return p;
	}
}
