package org.almostrealism.audio.pattern.test;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.pattern.PatternAudio;
import org.almostrealism.audio.pattern.PatternElement;
import org.almostrealism.audio.pattern.PatternNote;
import org.almostrealism.collect.PackedCollection;
import org.junit.Test;

public class PatternAudioTest {
	@Test
	public void push() {
		PackedCollection in = new PackedCollection(5);
		Scalar s = new Scalar(in, 0);
		s.setLeft(4);
		s.setRight(6);

		PatternElement e = new PatternElement(new PatternNote(in), 0.5);
		PatternAudio audio = new PatternAudio(60, 1.0, 1.0, 10);
		audio.push(e);

		Scalar out = new Scalar(audio.getData(), 5);
		System.out.println(out);
	}
}
