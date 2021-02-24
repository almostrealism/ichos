package com.almostrealism.audio.feature;

import io.almostrealism.code.OperationAdapter;
import io.almostrealism.relation.Evaluable;
import io.almostrealism.relation.Producer;
import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.algebra.computations.ScalarBankMultiply;
import org.almostrealism.util.CodeFeatures;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class FeatureWindowFunction implements CodeFeatures {
	private final ScalarBank win;
	Evaluable<ScalarBank> window;

	public FeatureWindowFunction(FrameExtractionSettings opts) {
		int frameLength = opts.getWindowSize();
		assert frameLength > 0;

		win = new ScalarBank(frameLength);

		double a = 2.0 * Math.PI / (frameLength - 1);

		for (int i = 0; i < frameLength; i++) {
			double v = 0.5 - 0.5 * Math.cos(a * (double) i);
			if ("hanning".equals(opts.getWindowType())) {
				win.set(i, v);
			} else if ("sine".equals(opts.getWindowType())) {
				// when you are checking ws wikipedia, please
				// note that 0.5 * a = PI/(frameLength-1)
				win.set(i, Math.sin(0.5 * a * (double) i));
			} else if ("hamming".equals(opts.getWindowType())) {
				win.set(i, 0.54 - 0.46 * Math.cos(a * (double) i));
			} else if ("povey".equals(opts.getWindowType())) {  // like hamming but goes to zero at edges.
				win.set(i, Math.pow(v, 0.85));
			} else if ("rectangular".equals(opts.getWindowType())) {
				win.set(i, 1.0);
			} else if ("blackman".equals(opts.getWindowType())) {
				win.set(i, opts.getBlackmanCoeff().getValue() - 0.5 * Math.cos(a * (double) i) +
						(0.5 - opts.getBlackmanCoeff().getValue()) * Math.cos(2 * a * (double) i));
			} else {
				throw new IllegalArgumentException("Invalid window type " + opts.getWindowType());
			}
		}

		this.window = new ScalarBankMultiply(win.getCount(), scalars(win), v(win.getCount() * 2, 0)).get();
		((OperationAdapter) window).compile();
	}

	public UnaryOperator<ScalarBank> getWindow() {
		return window::evaluate;
	}

	public Producer<ScalarBank> getWindow(Supplier<Evaluable<? extends ScalarBank>> input) {
		return new ScalarBankMultiply(win.getCount(), scalars(win), input);
	}
}
