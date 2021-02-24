package org.almostrealism.audio;

public class VadEnergyOptions {
	private double vad_energy_threshold;
	private double vad_energy_mean_scale;
	private int vad_frames_context;
	private double vad_proportion_threshold;

	public VadEnergyOptions() {
		vad_energy_threshold = 5.0;
		vad_energy_mean_scale = 0.5;
		vad_frames_context = 0;
		vad_proportion_threshold = 0.6;
	}

	public double getVad_energy_threshold() {
		return vad_energy_threshold;
	}

	public void setVad_energy_threshold(double vad_energy_threshold) {
		this.vad_energy_threshold = vad_energy_threshold;
	}

	public double getVad_energy_mean_scale() {
		return vad_energy_mean_scale;
	}

	public void setVad_energy_mean_scale(double vad_energy_mean_scale) {
		this.vad_energy_mean_scale = vad_energy_mean_scale;
	}

	public int getVad_frames_context() {
		return vad_frames_context;
	}

	public void setVad_frames_context(int vad_frames_context) {
		this.vad_frames_context = vad_frames_context;
	}

	public double getVad_proportion_threshold() {
		return vad_proportion_threshold;
	}

	public void setVad_proportion_threshold(double vad_proportion_threshold) {
		this.vad_proportion_threshold = vad_proportion_threshold;
	}
}
