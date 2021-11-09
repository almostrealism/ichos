package org.almostrealism.audio.sources.test;

import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.CellFeatures;
import org.almostrealism.audio.CellList;
import org.almostrealism.audio.DynamicAudioCell;
import org.almostrealism.audio.OutputLine;
import org.almostrealism.audio.computations.DefaultEnvelopeComputation;
import org.almostrealism.audio.data.PolymorphicAudioData;
import org.almostrealism.audio.sources.SineWaveCell;
import org.almostrealism.audio.sources.ValueSequenceCell;
import org.almostrealism.audio.sources.ValueSequencePush;
import org.almostrealism.audio.sources.ValueSequenceTick;
import org.almostrealism.graph.Receptor;
import org.almostrealism.hardware.AcceleratedOperation;
import org.almostrealism.hardware.DynamicAcceleratedOperation;
import org.almostrealism.heredity.ScaleFactor;
import org.almostrealism.time.Frequency;
import org.almostrealism.time.TemporalRunner;
import org.almostrealism.util.TestFeatures;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.stream.IntStream;


public class SequenceTest implements CellFeatures, TestFeatures {
	@Test
	public void valueSequencePush() {
		PolymorphicAudioData data = new PolymorphicAudioData();
		Scalar out = new Scalar();
		ValueSequencePush push = new ValueSequencePush(data, v(4), out, v(1.0), v(2.0));
		data.setWavePosition(3);

		DynamicAcceleratedOperation op = (DynamicAcceleratedOperation) push.get();
		System.out.println(op.getFunctionDefinition());
		op.run();
		assertEquals(2.0, out);
	}

	@Test
	public void valueSequenceTick() {
		PolymorphicAudioData data = new PolymorphicAudioData();
		ValueSequenceTick tick = new ValueSequenceTick(data, v(4), v(1.0), v(2.0));
		data.setWaveLength(1.0);

		DynamicAcceleratedOperation op = (DynamicAcceleratedOperation) tick.get();
		System.out.println(op.getFunctionDefinition());
		op.run();
		assertEquals(1.0, data.wavePosition());
	}

	@Test
	public void valueSequenceCell() {
		ValueSequenceCell cell = new ValueSequenceCell(i -> v(i + 1), v(0.1), 2);
		cell.setReceptor(loggingReceptor());

		cell.setup().get().run();
		Runnable push = cell.push(v(0.0)).get();
		Runnable tick = cell.tick().get();

		IntStream.range(0, OutputLine.sampleRate / 10).forEach(i -> {
			push.run();
			tick.run();
		});
	}

	@Test
	public void valueSequenceCsv() {
		CellList cells = seq(i -> v(i + 1), v(0.1), 2).csv(i -> new File("value-sequence-test.csv"));

		TemporalRunner runner = new TemporalRunner(cells, OutputLine.sampleRate / 10);
		runner.get().run();
		cells.reset();
	}

	@Test
	public void valueSequenceAssign() {
		Scalar out = new Scalar();

		CellList cells = seq(i -> v(i + 1), v(0.1), 2);
		cells.get(0).setReceptor(a(p(out)));

		TemporalRunner runner = new TemporalRunner(cells, OutputLine.sampleRate / 10);
		runner.get().run();
		assertEquals(2.0, out);
		cells.reset();
	}

	protected SineWaveCell cell(int freq) {
		SineWaveCell cell = new SineWaveCell();
		cell.setFreq(freq);
		cell.setNoteLength(6000);
		cell.setAmplitude(0.4);
		cell.setEnvelope(DefaultEnvelopeComputation::new);
		return cell;
	}

	@Test
	public void valueSequenceWithDynamicCell() {
		SineWaveCell cell1 = cell(196);
		SineWaveCell cell2 = cell(261);

		Scalar out = new Scalar();

		ValueSequenceCell seq = (ValueSequenceCell) seq(i -> v(0.25 + i * 0.5), v(2), 2).get(0);
		seq.setReceptor(a(p(out)));

		CellList cells = new CellList();
		cells.addRoot(seq);
		cells = new CellList(cells);
		cells.addRoot(new DynamicAudioCell(v(1).multiply(p(out)), Arrays.asList(data -> cell1, data -> cell2)));
		cells = cells.o(i -> new File("seq-dynamic-test.wav"));

		TemporalRunner runner = new TemporalRunner(cells, 4 * OutputLine.sampleRate);
		runner.get().run();
		cells.reset();

		System.out.println(out);
	}

	@Test
	public void notes() {
		CellList cells = w(new Frequency(196), new Frequency(196));
		((SineWaveCell) cells.get(0)).setNoteLength(2000);
		((SineWaveCell) cells.get(1)).setNoteLength(2000);

		cells = cells.gr(2000, 8,
				i -> i % 2 == 0 ? 0 : 1)
				.o(i -> new File("notes-seq-test.wav"));
		cells.sec(2).get().run();
	}

	@Test
	public void mix() {
		int count = 32;

		CellList cells = cells(
					silence().and(w(v(bpm(128).l(1)), "src/test/resources/BD 909 Color 06.wav"))
						.gr(bpm(128).l(count), count, i -> 1),
					silence().and(w(v(bpm(128).l(1)), "src/test/resources/Snare Perc DD.wav"))
						.gr(bpm(128).l(count), count, i -> i % 2 == 0 ? 0 : 1),
					silence().and(w(v(bpm(128).l(0.5)), v(bpm(128).l(1)), "src/test/resources/GT_HAT_31.wav"))
						.gr(bpm(128).l(count), count * 2, i -> i % 2 == 0 ? 0 : 1))
				.f(i -> i == 0 ? new ScaleFactor(0.5) : new ScaleFactor(0.1))
				.sum().o(i -> new File("mix-test.wav"));

		cells.sec(bpm(128).l(count)).get().run();
	}

	protected Receptor<Scalar> loggingReceptor() {
		return protein -> () -> () -> System.out.println(protein.get().evaluate());
	}

}
