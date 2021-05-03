package org.almostrealism.audio.computations;

import org.almostrealism.hardware.Hardware;
import org.jocl.cl_command_queue;
import org.jocl.cl_mem;

public class NativeFFT {
	public void transform(cl_mem output, cl_mem input, cl_mem config,
						int outputOffset, int inputOffset, int configOffset,
						int outputSize, int inputSize, int configSize) {
		transform(Hardware.getLocalHardware().getQueue(), output, input, config,
					outputOffset, inputOffset, configOffset,
					outputSize, inputSize, configSize);
	}

	public void transform(cl_command_queue command_queue,
						  cl_mem output, cl_mem input, cl_mem config,
						  int outputOffset, int inputOffset, int configOffset,
						  int outputSize, int inputSize, int configSize) {
		transform(command_queue.getNativePointer(),
				output.getNativePointer(),
				input.getNativePointer(),
				config.getNativePointer(),
				outputOffset, inputOffset, configOffset,
				outputSize, inputSize, configSize);
	}

	public native void transform(long command_queue,
								 long output, long input, long config,
								 int outputOffset, int inputOffset, int configOffset,
								 int outputSize, int inputSize, int configSize);
}
