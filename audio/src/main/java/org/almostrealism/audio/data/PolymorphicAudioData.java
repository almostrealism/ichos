package org.almostrealism.audio.data;

import org.almostrealism.algebra.ScalarBank;
import org.almostrealism.audio.sources.SineWaveCellData;
import org.almostrealism.audio.sources.WavCellData;

public class PolymorphicAudioData extends ScalarBank implements SineWaveCellData, WavCellData {
	public PolymorphicAudioData() {
		super(7);
	}
}
