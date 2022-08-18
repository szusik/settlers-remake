package jsettlers.graphics.image.reader.imageindex;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ShortBuffer;

public interface ImageConverter {
	void convert(DataInputStream dis, int count, ShortBuffer output) throws IOException;
}
