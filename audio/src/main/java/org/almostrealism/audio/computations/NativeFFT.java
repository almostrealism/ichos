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

import org.almostrealism.hardware.Hardware;
import org.jocl.cl_command_queue;
import org.jocl.cl_mem;

public class NativeFFT {
	public void transform(long output, long input, long config,
						int outputOffset, int inputOffset, int configOffset,
						int outputSize, int inputSize, int configSize) {
		transform(Hardware.getLocalHardware().getQueue(), output, input, config,
					outputOffset, inputOffset, configOffset,
					outputSize, inputSize, configSize);
	}

	public void transform(cl_command_queue command_queue,
						  long output, long input, long config,
						  int outputOffset, int inputOffset, int configOffset,
						  int outputSize, int inputSize, int configSize) {
		transform(command_queue.getNativePointer(),
				output, input, config,
				outputOffset, inputOffset, configOffset,
				outputSize, inputSize, configSize);
	}

	public native void transform(long command_queue,
								 long output, long input, long config,
								 int outputOffset, int inputOffset, int configOffset,
								 int outputSize, int inputSize, int configSize);
}
