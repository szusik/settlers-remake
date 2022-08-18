package jsettlers.graphics.image.reader.imageindex;

public class RGB8Converter extends AlphaBitImageConverter {
	protected RGB8Converter() {
		super(3);
	}

	@Override
	protected short readPixel(byte[] bfr, int offset, boolean alpha) {
		return (short) (cnv8to4(bfr[offset]) << 12 | cnv8to4(bfr[offset+1]) << 8 | cnv8to4(bfr[offset+2]) << 4 | (alpha?0b1111 : 0));
	}
}
