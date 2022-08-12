/*
 * Copyright (c) 2015 - 2018
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package jsettlers.textures.generation;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ShortBuffer;

/**
 * This class writes texture files
 *
 * @author michael
 */
public class TextureFile {

	public static void write(File file, ShortBuffer data, int width, int height, int imageWidth, int imageHeight) throws IOException {
		try(DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(file)))) {
			out.writeShort(imageWidth);
			out.writeShort(imageHeight);

			if(data.remaining() != width*height) {
				throw new IllegalStateException("illegal amount of data: actual: " + data.remaining() + ", expected: " + width*height);
			}

			for(int y = 0; y < imageHeight; y++) {
				for (int x = 0; x < imageWidth; x++) {
					int rx = (int) (x*width/(float)imageWidth);
					int ry = (int) (y*height/(float)imageHeight);
					out.writeShort(data.get(rx + ry*width));
				}
			}
		}
	}
}
