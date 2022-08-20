package jsettlers.graphics.image.reader.imageindex;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ShortBuffer;

public abstract class AlphaBitImageConverter implements ImageConverter {

	private final int dataPerPixel;

	protected AlphaBitImageConverter(int dataPerPixel) {
		this.dataPerPixel = dataPerPixel;
	}

	@Override
	public final void convert(DataInputStream dis, int count, ShortBuffer output) throws IOException {
		int blocks = count / 8;

		short[] blockData = new short[8];
		for(int i = 0; i < blocks; i++) {
			readSingleBlock(dis, blockData);
			output.put(blockData);
		}

		int remaining = count % 8;
		short[] restBlock = new short[remaining];
		readSingleBlock(dis, restBlock);
		output.put(restBlock);
		output.rewind();
	}

	private void readSingleBlock(DataInputStream dis, short[] data) throws IOException {
		int blockSize = data.length*dataPerPixel+1;
		byte[] bfr = dis.readNBytes(blockSize);
		if(blockSize != bfr.length) {
			throw new IOException("Invalid block data!");
		}

		byte alphaBits = bfr[bfr.length-1];

		for(int i = 0; i < data.length; i++) {
			boolean alphaBit = (alphaBits & (1 << data.length-i-1)) > 0;

			data[i] = readPixel(bfr, i*dataPerPixel, alphaBit);
		}
	}

	protected abstract short readPixel(byte[] bfr, int offset, boolean alpha);

	protected final int cnv8to4(byte c8bit) {
		int unsigned = c8bit&0xFF;
		return (int) (unsigned/255f*15f);
	}
}
