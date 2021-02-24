package org.almostrealism.audio.computations;

import io.almostrealism.code.OperationAdapter;
import io.almostrealism.relation.Evaluable;
import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.Pair;
import org.almostrealism.algebra.PairBank;
import org.almostrealism.algebra.PairProducer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarProducer;
import org.almostrealism.algebra.computations.ComplexFromAngle;
import org.almostrealism.algebra.computations.PairBankInterleave;
import org.almostrealism.algebra.computations.PairFromPairBank;
import org.almostrealism.algebra.computations.PairFromScalars;
import org.almostrealism.hardware.DynamicAcceleratedOperation;
import org.almostrealism.util.CodeFeatures;

import java.util.HashMap;
import java.util.Map;

public class SplitRadixFFT implements Evaluable<PairBank>, CodeFeatures {
	public static final double SQRT_2 = Math.sqrt(2.0);

	private final boolean forward;

	private final Evaluable<Pair> radix4Part1Pos;
	private final Evaluable<Pair> radix4Part1Neg;
	private final Evaluable<Pair> radix4Part2Pos;
	private final Evaluable<Pair> radix4Part2Neg;
	private final Evaluable<Pair> radix2A;
	private final Evaluable<Pair> radix2B;
	private final Evaluable<Pair> radix2Even;
	private final Evaluable<Pair> radix2Odd;

	private Map<Integer, Evaluable<PairBank>> interleavers;

	public SplitRadixFFT(int bins, boolean forward) {
		this.forward = forward;
		populateInterleavers(bins);

		Producer<Scalar> angle = v(Scalar.class, 0);
		Producer<Scalar> k = v(Scalar.class, 1);
		Producer<Scalar> n = v(Scalar.class, 2);
		Producer<PairBank> bank = v(2, 3);
		ScalarProducer halfN = scalarsMultiply(n, v(0.5));
		ScalarProducer quarterN = scalarsMultiply(n, v(0.25));
		ScalarProducer tripleQuarterN = scalarsMultiply(n, v(0.75));

		ScalarProducer kPlusTripleQuarterN = scalarAdd(k, tripleQuarterN);
		ScalarProducer kPlusHalfN = scalarAdd(k, halfN);
		ScalarProducer kPlusQuarterN = scalarAdd(k, quarterN);

		PairProducer a = new PairFromPairBank(bank, k);
		PairProducer b = new PairFromPairBank(bank, kPlusQuarterN);
		PairProducer c = new PairFromPairBank(bank, kPlusHalfN);
		PairProducer d = new PairFromPairBank(bank, kPlusTripleQuarterN);

		radix2A = pairAdd(a, c).get();
		radix2B = pairAdd(b, d).get();

		PairProducer bMinusD = pairSubtract(b, d);
		PairProducer aMinusC = pairSubtract(a, c);
		PairProducer imaginaryTimesSubPos = new PairFromScalars(bMinusD.r().minus(), bMinusD.l());
		PairProducer imaginaryTimesSubNeg = new PairFromScalars(bMinusD.r(), bMinusD.l().minus());

		ScalarProducer angleK = scalarsMultiply(angle, k);
		ScalarProducer angleK3 = scalarsMultiply(angleK, v(3));
		PairProducer omega = new ComplexFromAngle(angleK);
		PairProducer omegaToPowerOf3 = new ComplexFromAngle(angleK3);

		radix4Part1Pos = pairSubtract(aMinusC, imaginaryTimesSubPos).multiplyComplex(omega).get();
		radix4Part1Neg = pairSubtract(aMinusC, imaginaryTimesSubNeg).multiplyComplex(omega).get();
		radix4Part2Pos = pairAdd(aMinusC, imaginaryTimesSubPos).multiplyComplex(omegaToPowerOf3).get();
		radix4Part2Neg = pairAdd(aMinusC, imaginaryTimesSubNeg).multiplyComplex(omegaToPowerOf3).get();

		radix2Even = pairAdd(a, c).get();
		radix2Odd = pairSubtract(a, c).multiplyComplex(omega).get();

		((OperationAdapter) radix4Part1Pos).compile();
		((OperationAdapter) radix4Part1Neg).compile();
		((OperationAdapter) radix4Part2Pos).compile();
		((OperationAdapter) radix4Part2Neg).compile();
		((OperationAdapter) radix2A).compile();
		((OperationAdapter) radix2B).compile();
		((OperationAdapter) radix2Even).compile();
		((OperationAdapter) radix2Odd).compile();
	}

	private void populateInterleavers(int bins) {
		interleavers = new HashMap<>();

		while (bins > 0) {
			Evaluable<PairBank> inter = new PairBankInterleave(bins, v(2, 0), v(2, 1)).get();
			((OperationAdapter) inter).compile();

			interleavers.put(bins, inter);
			bins = bins / 2;
		}
	}

	@Override
	public PairBank evaluate(Object... args) {
		return transform((PairBank) args[0], forward);
	}

	protected PairBank transform(PairBank values, boolean forward) {
		return calculateTransform(values, !forward, !forward);
	}

	private PairBank calculateTransform(PairBank input, boolean inverseTransform, boolean isFirstSplit) {
		int powerOfTwo = 31 - Integer.numberOfLeadingZeros(input.getCount());

		if (1 << powerOfTwo != input.getCount()) {
			throw new IllegalArgumentException("Length of the <input> must be a power of 2 (i.e. 2^N, N = 1, 2, ...), actual: " + input.getCount());
		}

		if (input.getCount() >= 4) {
			int halfN = input.getCount() / 2;
			int quarterN = halfN / 2;

			PairBank radix2 = new PairBank(halfN);
			PairBank radix4Part1 = new PairBank(quarterN);
			PairBank radix4Part2 = new PairBank(quarterN);
			Scalar angle = new Scalar(2 * Math.PI / input.getCount());

			Evaluable<Pair> radix4Part1P, radix4Part2P;

			if (!inverseTransform) {
				angle.setValue(angle.getValue() * -1);
				radix4Part1P = radix4Part1Pos;
				radix4Part2P = radix4Part2Pos;
			} else {
				radix4Part1P = radix4Part1Neg;
				radix4Part2P = radix4Part2Neg;
			}

			Scalar ks = new Scalar(0);
			Scalar ns = new Scalar(input.getCount());
			for (int k = 0; k < quarterN; k++) {
				ks.setValue(k);

				//radix-2 part
				radix2.set(k, radix2A.evaluate(angle, ks, ns, input));
				radix2.set(k + quarterN, radix2B.evaluate(angle, ks, ns, input));

				//radix-4 part
				radix4Part1.set(k, radix4Part1P.evaluate(angle, ks, ns, input));
				radix4Part2.set(k, radix4Part2P.evaluate(angle, ks, ns, input));
			}

			PairBank radix2FFT = calculateTransform(radix2, inverseTransform, false);
			PairBank radix4Part1FFT = calculateTransform(radix4Part1, inverseTransform, false);
			PairBank radix4Part2FFT = calculateTransform(radix4Part2, inverseTransform, false);

			PairBank transformed = new PairBank(input.getCount());

			for (int k = 0; k < quarterN; k++) {
				int doubleK = 2 * k;
				int quadrupleK = 2 * doubleK;

				if (inverseTransform && isFirstSplit) {
					transformed.set(doubleK, radix2FFT.get(k).divide(input.getCount()));
					transformed.set(quadrupleK + 1, radix4Part1FFT.get(k).divide(input.getCount()));
					transformed.set(doubleK + halfN, radix2FFT.get(k + quarterN).divide(input.getCount()));
					transformed.set(quadrupleK + 3, radix4Part2FFT.get(k).divide(input.getCount()));
				} else {
					transformed.set(doubleK, radix2FFT.get(k));
					transformed.set(quadrupleK + 1, radix4Part1FFT.get(k));
					transformed.set(doubleK + halfN, radix2FFT.get(k + quarterN));
					transformed.set(quadrupleK + 3, radix4Part2FFT.get(k));
				}
			}

			return transformed;
		}

		if (input.getCount() >= 2) {
			return calculateRadix2Transform(input, inverseTransform, isFirstSplit);
		}

		return input;
	}

	private PairBank calculateRadix2Transform(PairBank input, boolean inverseTransform, boolean isFirstSplit) {
		Scalar ns = new Scalar(input.getCount());

		if (input.getCount() >= 2) {
			int halfN = input.getCount() / 2;
			PairBank even = new PairBank(halfN);
			PairBank odd = new PairBank(halfN);
			Scalar angle = new Scalar(2 * Math.PI / input.getCount());

			if (!inverseTransform) {
				angle.setValue(angle.getValue() * -1);
			}

			Scalar ks = new Scalar();
			for (int k = 0; k < halfN; k++) {
				// int kPlusHalfN = k + halfN;
				// double angleK = angle * k;
				// Pair omega = new Pair(Math.cos(angleK), Math.sin(angleK));
				// even.set(k, input.get(k).add(input.get(kPlusHalfN)));
				// odd.set(k, input.get(k).subtract(input.get(kPlusHalfN)).multiplyComplex(omega));
				even.set(k, radix2Even.evaluate(angle, ks, ns, input));
				odd.set(k, radix2Odd.evaluate(angle, ks, ns, input));
			}

			PairBank evenFFT = calculateRadix2Transform(even, inverseTransform, false);
			PairBank oddFFT = calculateRadix2Transform(odd, inverseTransform, false);

			PairBank transformed = inverseTransform && isFirstSplit ? new PairBank(input.getCount()) : null;

			for (int k = 0; k < halfN; k++) {
				int doubleK = k * 2;
				if (inverseTransform && isFirstSplit) {
					transformed.set(doubleK, evenFFT.get(k).divide(input.getCount()));
					transformed.set(doubleK + 1, oddFFT.get(k).divide(input.getCount()));
				} else {
//					transformed.set(doubleK, evenFFT.get(k));
//					transformed.set(doubleK + 1, oddFFT.get(k));
					transformed = interleavers.get(input.getCount()).evaluate(evenFFT, oddFFT);
				}
			}

			return transformed;
		}

		return input;
	}
}
