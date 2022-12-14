package org.almostrealism.audio.transform;

import org.almostrealism.audio.JavaAudioSample;
import org.almostrealism.audio.SampleTransformer;

public abstract class ResampleTransformer implements SampleTransformer<JavaAudioSample> {
	private double ratio = 1.0;
	
	public void setRatio(double r) { this.ratio = r; }
	public double getRatio() { return this.ratio; }
	
	public void transform(JavaAudioSample s) {
		byte data[][] = s.data;
		byte newData[][] = new byte[(int) (s.data.length * ratio)][s.data[0].length];
		
		for (int i = 0; i < newData.length; i++) {
			double indexd = i / ratio;
			double delta = indexd % 1;
			int index = (int) (indexd - delta);
			int next = index + 1;
			if (next > data.length - 1) next--;
			
			for (int j = 0; j < newData[i].length; j++)
				newData[i][j] = resample(data[index][j], data[next][j], delta);
		}
		
		s.data = newData;
		s.loopStart = 0;
		s.loopEnd = s.data.length;
		
		s.beatLength = (int) (s.beatLength * this.ratio);
		s.marker = (int) (s.marker * this.ratio);
	}
	
	public abstract byte resample(byte a, byte b, double delta);
}
