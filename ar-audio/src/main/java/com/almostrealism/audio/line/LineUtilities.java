package com.almostrealism.audio.line;

import com.almostrealism.audio.JavaAudioSample;
import org.almostrealism.algebra.Scalar;
import org.almostrealism.audio.OutputLine;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class LineUtilities {
	protected static AudioFormat lastFormat;
	
	/**
	 * Returns a SourceDataOutputLine for the most recent format requested.
	 */
	public static OutputLine getLine() { return getLine(lastFormat); }
	
	/**
	 * Returns a SourceDataOutputLine for the specified format.
	 */
	public static OutputLine getLine(AudioFormat format) {
		SourceDataLine line;
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		if (!AudioSystem.isLineSupported(info)) {
			System.out.println("Not supported");
			return null;
		}
		
		lastFormat = format;
		
		try {
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(format);
			line.start();
		} catch (LineUnavailableException ex) {
			System.out.println("Unavailable (" + ex.getMessage() + ")");
			return null;
		}
		
		return new SourceDataOutputLine(line);
	}
	
	/**
	 * Converts the specified sample so that it can be played using a line
	 * configured with the current default format.
	 * 
	 * @param s  Sample to convert.
	 * @return  Converted sample.
	 */
	public static JavaAudioSample convert(JavaAudioSample s) {
		return convert(s, lastFormat);
	}
	
	/**
	 * Adjusts the specified sample so that it can be played using a line
	 * configured with the specified format.
	 * 
	 * @param s  Sample to convert.
	 * @param f  Format to convert to.
	 * @return  Converted sample.
	 */
	public static JavaAudioSample convert(JavaAudioSample s, AudioFormat f) {
		return s;
	}

	/**
	 * Converts the specified long value to the bytes of one frame,
	 * depending on the frame size of the specified {@link AudioFormat}.
	 */
	public static byte[] toFrame(Scalar frame, AudioFormat format) {
		int frameSize = format.getFrameSize();

		byte frameBytes[] = null;

		double frameAsDouble = frame.getValue();

		if (frameSize == 1) {
			frameBytes = new byte[1];
			frameBytes[0] = (byte) (Byte.MAX_VALUE * frameAsDouble);
		} else {
			throw new IllegalArgumentException("Frame size " + frameSize + " is not supported");
		}

		return frameBytes;
	}
	
	public static AudioFormat getAudioFormat() {
		return lastFormat;
	}
	
	/**
	 * Initializes the default audio format using that data read from the specified stream.
	 * This method buffers the stream for you.
	 * 
	 * @param instream  Stream to read initial audio data from.
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 * @return  The format of the data that was read.
	 */
	public static AudioFormat initDefaultAudioFormat(InputStream instream) throws UnsupportedAudioFileException, IOException {
		instream = new BufferedInputStream(instream);
		AudioInputStream in = AudioSystem.getAudioInputStream(instream);
		return lastFormat = in.getFormat();
	}
}
