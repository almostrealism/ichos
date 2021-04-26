package org.almostrealism.audio.computations.test;

import org.almostrealism.algebra.PairTable;
import org.almostrealism.audio.computations.ComplexFFT;
import org.almostrealism.algebra.Pair;
import org.almostrealism.algebra.PairBank;
import org.almostrealism.hardware.HardwareException;
import org.almostrealism.hardware.MemoryBank;
import org.almostrealism.util.TestFeatures;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class ComplexFFTTest implements TestFeatures {
	private static final int kernelSize = 20;
	private static final double[] input = {0.000000, -0.089694, -0.280677, -0.421681, -0.277944, 0.183299, 0.779925, 1.492257, 2.033665, 2.213228, 1.998645, 1.732736, 1.755729, 2.330768, 3.179083, 3.811341, 4.228307, 4.494624, 3.448980, 0.535508, -3.192014, -6.464241, -9.660633, -11.870137, -12.257075, -10.083065, -8.045455, -4.603135, -0.532577, 3.794308, 4.777180, 5.455580, 5.729632, 5.393667, 3.769879, 3.934175, 4.380951, 7.003248, 10.376730, 13.005665, 15.040275, 12.980432, 5.341743, -1.645482, -8.627213, -13.180408, -16.953100, -17.481390, -9.732295, -0.444518, 1.510766, 6.490928, 7.832850, 7.396569, 2.571124, 1.496327, 2.665652, 4.429607, 9.063531, 17.917639, 24.311132, 37.514820, 36.952362, 27.326279, 20.740438, 8.601480, -3.380449, -20.606853, -28.571341, -22.921618, -24.719198, -21.836292, -17.454727, -12.633554, -11.102814, -20.621502, -28.473419, -37.577557, -35.861874, -23.781889, -13.277066, 1.947855, 29.446297, 42.838684, 51.519161, 50.941532, 44.229107, 32.351135, 9.555557, 0.596025, -7.344855, -8.663194, -7.243068, -15.335967, -21.631544, -28.126217, -41.364418, -57.524673, -69.631790, -59.741020, -53.687119, -53.567333, -43.342369, -39.076870, -29.006899, -30.016510, -29.987375, -21.600697, -14.063282, -9.710466, -8.214617, -0.967300, 1.189335, -8.222613, -14.077708, -21.943466, -27.824106, -32.801533, -44.585953, -46.179382, -42.762730, -40.863991, -38.051144, -33.810478, -33.059345, -30.390207, -28.663109, -30.171106, -28.744534, -19.441828, -14.483845, -22.889154, -20.794006, -15.613181, -12.120833, -14.843236, -6.767931, 1.616426, 16.706846, 15.290432, 9.605330, -1.993879, -8.744300, -13.683979, -22.533998, -11.474499, -5.359107, 12.903996, 32.931976, 42.552532, 23.966291, -8.406090, -33.082829, -46.919640, -68.057632, -78.725670, -64.046738, -33.457127, -6.573945, 24.117315, 47.569683, 76.146477, 108.199280, 127.095795, 162.548370, 187.245392, 216.069275, 222.978058, 214.914551, 175.972961, 129.245300, 83.015808, 39.200306, 2.243525, -42.674610, -77.733612, -117.108589, -141.748215, -168.730850, -191.134811, -194.229797, -171.304764, -129.176086, -77.482918, -37.887508, 4.321923, 34.325359, 61.226192, 101.842171, 103.896683, 108.497002, 132.372009, 146.290634, 133.670898, 115.971786, 103.758583, 67.221786, 50.665073, 26.559351, -7.044951, -28.763475, -51.137005, -54.580227, -64.829315, -66.664742, -58.691158, -49.866135, -29.297029, 1.442142, 24.445948, 45.571285, 38.126846, 35.261189, 19.013771, 16.739914, 23.098196, 9.099855, 19.352211, 31.732033, 34.548450, 39.344940, 19.245165, -12.853366, -23.298367, -24.821587, -23.972502, -23.872808, -5.144417, 19.426111, 54.801228, 58.653454, 87.056793, 93.831299, 101.757530, 85.873451, 50.772511, 35.727711, 21.055124, 5.471799, -21.249924, -11.244349, -2.686260, -0.168226, 27.138620, 26.016756, 4.357766, -18.443705, -31.854504, -53.227962, -74.176094, -66.263214, -42.422714, -25.728436, 3.610842, 21.348194, 22.354284, 14.636805, -2.709909, -22.141933, -31.111454, -44.464634, -50.911484, -45.740395, -16.262383, 5.851552, 13.491541, 21.458876, 29.923298, 22.273422, 8.997065, -27.618488, -51.002804, -58.167297, -57.805027, -53.652855, -54.157639, -43.159687, -17.532181, -28.352955, -46.681423, -55.923389, -66.620384, -78.730263, -86.779556, -71.816719, -49.652863, -36.427315, -24.225313, -20.959066, -12.820323, -9.290688, -11.707084, -20.743563, -15.246432, -6.260724, -2.238197, 2.715221, 5.910264, 10.386369, 6.092572, 3.823456, 3.183392, 2.456050, 0.549079, -4.235939, -18.004429, -14.991520, -15.222369, -13.054528, -13.116466, -10.080865, -4.359385, -5.517323, -9.628589, -5.014955, 1.043007, 24.548920, 54.366493, 87.995033, 113.396027, 122.949516, 119.693893, 103.506416, 73.847000, 42.920517, 9.609437, -11.270100, -23.386442, -20.233187, -8.510818, -5.867599, -8.542984, -20.304579, -38.250759, -53.561779, -62.959824, -58.777096, -43.290810, -20.263786, 3.998118, 25.730253, 39.583717, 38.985458, 30.633295, 20.577339, 13.818870, 5.237952, 0.143665, 2.020593, 9.682196, 12.543064, 11.813222, 6.748987, 2.530689, -5.080540, -13.187437, -17.094307, -17.221382, -13.651767, -8.170530, -1.827398, 4.912029, 7.607140, 9.490978, 8.219586, 5.435180, 3.388952, 1.705922, -1.667652, -1.173065, -2.340821, -2.186936, -0.492692, -1.403425, -1.753728, -0.964662, -1.821616, -3.023450, -1.507610, -1.053387, -0.696807, -0.651406, 0.091801, 0.728388, 0.574529, 0.913812, 0.954890, 0.989393, 1.127310, 1.111984, 0.986025, 0.607977, 0.392609, 0.173413, -0.008165, -0.066832, -0.028274, -0.012140, -0.001233, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000};
	private static final double[] output = {-133.884186, -31.736633, -446.001129, -9.111568, -24.099682, 1750.342041, 2451.848145, -1881.893066, -2167.784180, -105.304535, -422.750397, -662.842651, 471.996460, 2732.858398, 569.826172, -2193.066895, -348.157684, 520.551880, -399.347992, 686.904297, 930.355591, -889.509033, -594.551025, 364.142273, -451.652466, 657.411560, 2462.798828, -748.470032, -2485.253418, -1493.711548, -416.319427, 2861.627930, 2774.184814, -2307.367676, -3372.284912, 1132.769287, 1824.246704, 1472.019531, 3814.235352, -2515.299561, -6009.905273, -1839.094360, 1568.052002, 2994.589355, 1418.408691, -1593.472900, -4371.332031, 887.660889, 4368.125000, 1916.656250, -387.350098, -2546.356934, -2229.859863, 785.774841, 1834.767578, 524.465088, -382.799927, -133.440048, -369.966431, -579.535217, 331.367615, 1081.488770, 327.780731, -657.184204, -282.759033, -257.207947, -99.653435, 609.858154, 235.842422, -374.387390, 89.235123, 14.085526, -351.357391, 168.022827, 438.612946, -59.329639, 177.547638, -198.781952, -782.268066, -276.090637, 568.854736, 534.842773, -559.888550, 154.071960, 1409.523926, -490.048187, -1304.154785, -244.536057, 767.271851, 467.261902, -746.148499, -1510.079956, 90.316345, 1550.397217, -1376.937256, -611.282715, 1566.732544, 1030.409058, -296.115417, 149.445923, 216.565277, -668.675537, 381.829346, 101.727425, -216.537140, -82.101166, -540.860657, -436.515625, 264.708191, 639.043945, 170.700256, 274.503052, 214.296387, -870.265381, -469.207520, 293.081116, 62.306602, 409.524109, 325.314087, -600.122192, -513.811096, 528.807129, 509.258667, -113.515289, -103.596085, -142.587982, -82.625137, -9.698013, 11.172920, 93.706604, 48.001167, -51.305893, -1.834709, 8.136209, 32.545631, -20.072802, -41.945183, -94.834518, -66.399719, 78.644073, 30.340233, 48.911156, 51.544628, -91.287971, -68.763504, 89.109924, 45.973911, -65.348709, -33.920517, 21.872648, 23.208521, 62.212311, 19.192257, -78.555664, 26.310825, 37.256279, -57.501236, -51.911766, 36.572266, 22.115446, -30.372635, -6.119808, -26.185930, -1.460459, 29.220932, 50.005596, 35.929169, 1.082756, 1.016846, -82.824409, -52.089447, 46.115234, 63.386120, 0.866148, -85.745888, -57.051064, 50.452900, 91.141159, -36.414413, -45.099663, 62.408569, 32.826225, -32.370811, -29.576706, -17.083651, 24.958443, 137.652969, -25.468369, -206.978256, -63.082096, 129.888428, 100.615120, -38.219082, -62.728142, -3.568626, 20.619516, 25.477781, -31.779938, -66.952652, -21.533535, -35.321533, 111.546112, 149.514862, -73.998718, -191.314224, 18.300228, 177.190582, 98.061684, -20.588324, -139.700043, -35.337040, -7.833330, -87.943939, 89.081482, 170.402496, -15.960983, -126.850868, -97.347473, 4.401964, 141.600769, 62.878460, -66.607025, -15.371712, 2.230053, 9.355022, 0.824379, -64.668251, -61.532654, 82.030647, 147.251129, -64.523651, -214.524170, -36.038368, 225.916138, 49.681610, -97.061440, 99.773445, 120.159050, -47.821861, -186.874146, 63.207882, 0.096413, -195.723267, -19.354271, 10.893063, 79.255615, 59.818481, 110.641068, 82.531372, -108.112366, -67.212860, 34.262104, 36.169765, -53.467377, 5.136837, 74.408134, 23.468842, -105.429077, -62.682560, 19.562691, 45.873356, 25.224091, -52.350372, -69.606705, -28.283966, 45.586151, 0.671761, 62.772751, 77.639771, -11.131409, -51.386345, -107.091301, -19.344303, 145.473846, 23.683838, -94.903191, 8.753830, 82.714058, 13.939056, -85.536110, -38.216858, 62.195442, 29.642853, -16.047005, 13.144943, -2.991676, -15.102381, -12.736145, 17.536884, 20.798576, -13.433525, -67.876450, -54.949398, 64.498917, 64.968338, 22.098083, -24.789757, -49.408997, 27.864899, 86.468277, 36.243561, -109.366760, -10.010861, 16.935652, -81.250969, -44.532555, 37.853645, 53.458290, -86.947586, 1.573849, 146.775726, 70.424561, -101.409851, -122.274582, 41.719769, 127.701569, 37.407043, -106.546524, -73.507721, 37.264954, 43.215885, 8.347712, -22.582603, 6.190788, 50.045670, -25.047581, -45.757580, 4.306274, -7.475874, -23.701965, -15.696618, 61.585518, 63.611153, -17.998346, -42.295105, -13.754295, 23.856754, -2.777515, -14.739571, 1.649298, 3.188175, 4.350828, 15.972334, -0.131638, -21.242607, -52.048378, -42.442139, 61.118858, 56.176163, 12.865520, -29.783003, -35.635933, 19.141434, 50.345387, 42.673828, -51.913849, -83.648911, 9.991264, 73.454514, 15.071007, -45.055157, -22.445576, 31.694036, 21.799942, -29.427971, -34.370361, 8.332317, 35.327503, 11.121460, -21.292599, -16.041058, 6.671516, -3.165352, -2.594765, 6.139370, 11.747066, 21.708582, -7.689281, -59.547844, -9.547945, 67.178314, 43.140396, -38.718246, -64.752274, 6.607124, 76.177505, 14.794254, -37.973637, 32.199596, -14.011780, -44.925797, 13.893921, 29.423996, -50.813736, -41.408707, 43.042145, -5.871956, -56.031998, -47.191162, 102.574409, 77.285553, -3.288116, -2.759880, 1.756287, 85.451759, -35.091370, -101.171341, -72.396561, -4.708305, 47.223690, -1.007919, 24.616066, -17.485130, -4.779449, 63.939636, 31.631348, -24.075562, -49.611328, -23.761963, 6.276764, 35.785278, 35.436584, -31.821228, -52.216431, 30.599213, 71.759354, 11.921692, -105.811447, -83.928284, 69.481842, 71.923248, 6.339775, -21.639008, -11.616699, 15.200592, 0.764786, -13.602722, -6.962975, 30.909546, 8.675655, -45.401428, -45.946438, 6.381611, 65.414780, 9.684677, -36.039047, 5.028572, 28.129120, -4.947510, -34.358566, -6.557892, 52.594574, 50.146500, -58.426331, -67.524918, 2.207092, 23.851257, 39.973068, -9.344971, -36.799286, 11.637207, 44.512512, 7.773666, -19.775757, 13.319824, -13.474854, -36.139893, -1.967285, 43.987610, 18.574768, -40.976135, -46.759399, -12.028809, 59.916870, 64.863892, -15.646973, -44.533325, -43.199829, -10.856445, 22.126770, -18.621582, 26.581543, 59.561371, 3.551147, -39.930542, -35.730225, 10.682495, 46.969299, 28.695877, -40.826904, -40.297943, 4.998810, 25.300995, 2.693573, -35.055389, 5.226898, 25.929779, 1.982666, 3.635010, 11.691040, -22.290161, -5.037964, 51.763306, -3.435303, -37.033081, 5.959385, 36.527710, -29.844849, -41.501453, 6.066345, 23.190826, -3.133098};

	private static final ThreadLocal<PairBank> inputs = new ThreadLocal<>();
	private static final ThreadLocal<PairTable> inputTables = new ThreadLocal<>();
	private static final ThreadLocal<ComplexFFT> fft = new ThreadLocal<>();

	private static long totalTime;
	private static long totalComputations;

	private PairBank input() {
		PairBank in = ComplexFFTTest.inputs.get();

		if (in == null) {
			PairBank bank = new PairBank(input.length);
			IntStream.range(0, input.length).forEach(i -> bank.set(i, input[i], 0.0));
			ComplexFFTTest.inputs.set(bank);
			return bank;
		}

		return in;
	}

	private PairTable inputTable() {
		PairTable in = ComplexFFTTest.inputTables.get();

		if (in == null) {
			in = new PairTable(input.length, kernelSize);

			for (int t = 0; t < kernelSize; t++) {
				PairBank bank = new PairBank(input.length);
				IntStream.range(0, input.length).forEach(i -> bank.set(i, input[i], 0.0));
				in.set(t, bank);
			}

			ComplexFFTTest.inputTables.set(in);
		}

		return in;
	}

	private ComplexFFT complexFft() {
		ComplexFFT fft = ComplexFFTTest.fft.get();

		if (fft == null) {
			fft = new ComplexFFT(input.length, true, v(input.length, 0));
			ComplexFFTTest.fft.set(fft);
		}

		return fft;
	}

	private synchronized void updateTime(long time) {
		totalTime += time;
		totalComputations++;
	}

	protected Runnable compute(boolean print, boolean verify) {
		return () -> {
			try {
				PairBank input = input();

				long start = System.currentTimeMillis();
				PairBank p = complexFft().evaluate(input);
				updateTime(System.currentTimeMillis() - start);

				if (print) {
					IntStream.range(0, p.getCount())
							.mapToObj(p::get)
							.map(Pair::toString)
							.forEach(System.out::println);
				}

				if (verify) {
					assert IntStream.range(0, output.length).map(i ->
							i % 2 == 0 ? Math.abs(p.get(i / 2).r() - output[i]) > 0.001 ? 1 : 0 :
									Math.abs(p.get(i / 2).i() - output[i]) > 0.001 ? 1 : 0).sum() < 3;
				}
			} catch (HardwareException e) {
				e.printStackTrace();
				System.out.println(e.getProgram());
				Assert.fail();
			}
		};
	}

	private Runnable compute(int index) {
		return compute(index < 1, index < 10);
	}

	@Test
	public void fft() {
		compute(0).run();
	}

	@Test
	public void fftThreads() throws InterruptedException {
		int threads = 50;
		int count = 10 * threads;

		ExecutorService service = Executors.newFixedThreadPool(threads);
		IntStream.range(0, count)
				.mapToObj(this::compute)
				.forEach(service::execute);

		while (true) {
			Thread.sleep(5000);
			if (totalComputations >= count) {
				System.out.println("FFT required " +
						totalTime / totalComputations +
						" msec on average");
				return;
			}
		}
	}

	protected Runnable kernelCompute(boolean print, boolean verify) {
		return () -> {
			PairTable input = inputTable();
			PairTable table = new PairTable(input.getWidth(), input.getCount());

			long start = System.currentTimeMillis();
			complexFft().kernelEvaluate(table, new MemoryBank[] { input });
			updateTime(System.currentTimeMillis() - start);

			for (int t = 0; t < table.getCount(); t++) {
				PairBank p = table.get(t);

				if (print) {
					IntStream.range(0, p.getCount())
							.mapToObj(p::get)
							.map(Pair::toString)
							.forEach(System.out::println);
				}

				if (verify) {
					assert IntStream.range(0, output.length).map(i ->
							i % 2 == 0 ? Math.abs(p.get(i / 2).r() - output[i]) > 0.001 ? 1 : 0 :
									Math.abs(p.get(i / 2).i() - output[i]) > 0.001 ? 1 : 0).sum() < 3;
				}
			}
		};
	}

	private Runnable kernelCompute(int index) {
		return kernelCompute(index < 1, index < 0);
	}

	@Test
	public void fftKernelThreads() throws InterruptedException {
		int threads = 50;
		int count = 10 * threads;

		ExecutorService service = Executors.newFixedThreadPool(threads);
		IntStream.range(0, count)
				.mapToObj(this::kernelCompute)
				.forEach(service::execute);

		while (true) {
			Thread.sleep(5000);
			if (totalComputations >= count) {
				System.out.println("FFT required " +
						totalTime / totalComputations +
						" msec on average");
				return;
			}
		}
	}
}
