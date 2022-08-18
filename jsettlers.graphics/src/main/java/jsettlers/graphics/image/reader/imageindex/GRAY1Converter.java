package jsettlers.graphics.image.reader.imageindex;

public class GRAY1Converter extends AlphaBitImageConverter {

	protected GRAY1Converter() {
		super(0);
	}

	@Override
	protected short readPixel(byte[] bfr, int offset, boolean alpha) {
		return (short) (alpha?0x8:0);
	}
}
