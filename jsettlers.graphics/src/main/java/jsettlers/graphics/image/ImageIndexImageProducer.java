package jsettlers.graphics.image;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import jsettlers.graphics.image.reader.translator.ImageDataProducer;

public class ImageIndexImageProducer implements ImageDataProducer {

	private final int textureNumber;

	public ImageIndexImageProducer(int textureNumber) {
		this.textureNumber = textureNumber;
	}

	@Override
	public ShortBuffer produceData() {
		try(DataInputStream dis = new DataInputStream(ImageIndexFile.getResource("images_" + textureNumber))) {
			int width = dis.readShort();
			int height = dis.readShort();

			ShortBuffer output = ByteBuffer.allocateDirect(width*height*2)
					.order(ByteOrder.nativeOrder())
					.asShortBuffer();

			while(output.hasRemaining()) {
				output.put(dis.readShort());
			}
			output.rewind();
			return output;
		} catch (IOException e) {
			throw new IllegalStateException("failed to load imageIndexImage", e);
		}
	}
}
