package jsettlers.graphics.image.reader.imageindex;

public class GRAY8Converter extends AlphaBitImageConverter {
	protected GRAY8Converter() {
		super(1);
	}

	@Override
	protected short readPixel(byte[] bfr, int offset, boolean alpha) {
		int value = cnv8to4(bfr[offset]);
		return (short) (value<<12 | value<<8 | value << 4 | (alpha?0b1111:0));
	}
}
