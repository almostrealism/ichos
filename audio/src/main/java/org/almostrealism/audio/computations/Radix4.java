/*
 * Copyright 2021 Michael Murray
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

package org.almostrealism.audio.computations;

import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.PairBank;
import org.almostrealism.algebra.PairProducer;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.algebra.ScalarProducer;
import org.almostrealism.algebra.computations.ComplexFromAngle;
import org.almostrealism.algebra.computations.PairFromPairBank;
import org.almostrealism.algebra.computations.PairFromScalars;
import org.almostrealism.util.CodeFeatures;

public class Radix4 implements RadixComputationFactory, CodeFeatures {
	private int part;
	private boolean pos;

	public Radix4(int part, boolean pos) {
		this.part = part;
		this.pos = pos;
	}

	@Override
	public PairProducer build(Scalar angle, Scalar k, Scalar n, Producer<PairBank> bank, int length) {
		Producer<Scalar> angleProducer = v(angle);
		Producer<Scalar> kProducer = v(k);
		Producer<Scalar> nProducer = v(n);

		ScalarProducer halfN = scalarsMultiply(nProducer, v(0.5));
		ScalarProducer quarterN = scalarsMultiply(nProducer, v(0.25));
		ScalarProducer tripleQuarterN = scalarsMultiply(nProducer, v(0.75));

		ScalarProducer kPlusTripleQuarterN = scalarAdd(kProducer, tripleQuarterN);
		ScalarProducer kPlusHalfN = scalarAdd(kProducer, halfN);
		ScalarProducer kPlusQuarterN = scalarAdd(kProducer, quarterN);

		PairProducer a = new PairFromPairBank(bank, kProducer);
		PairProducer b = new PairFromPairBank(bank, kPlusQuarterN);
		PairProducer c = new PairFromPairBank(bank, kPlusHalfN);
		PairProducer d = new PairFromPairBank(bank, kPlusTripleQuarterN);

		PairProducer bMinusD = pairSubtract(b, d);
		PairProducer aMinusC = pairSubtract(a, c);

		PairProducer imaginaryTimesSub;

		if (pos) {
			imaginaryTimesSub = new PairFromScalars(bMinusD.r().minus(), bMinusD.l());
		} else {
			imaginaryTimesSub = new PairFromScalars(bMinusD.r(), bMinusD.l().minus());
		}

		ScalarProducer angleK = scalarsMultiply(angleProducer, kProducer);
		ScalarProducer angleK3 = scalarsMultiply(angleK, v(3));
		PairProducer omega = new ComplexFromAngle(angleK);
		PairProducer omegaToPowerOf3 = new ComplexFromAngle(angleK3);

		if (part == 0) {
			return pairSubtract(aMinusC, imaginaryTimesSub).multiplyComplex(omega);
		} else {
			return pairAdd(aMinusC, imaginaryTimesSub).multiplyComplex(omegaToPowerOf3);
		}
	}
}
