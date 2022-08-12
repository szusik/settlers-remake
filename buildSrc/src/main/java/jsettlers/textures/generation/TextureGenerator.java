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

import java.nio.file.Files;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * This class lets you generate a texture that can be understood by the graphics module. It generates the .texture file.
 *
 * @author michael
 */
public final class TextureGenerator {

	private static class ImageData {
		ProvidedImage data = null;
		ProvidedImage torso = null;
		String name;
	}

	private static final Pattern TUPLE_PATTERN = Pattern.compile("^(?<first>[-?\\d+]+)\\s+(?<second>[-?\\d+]+)\\R*$");

	private static final String OFFSET_SUFFIX = ".offset";
	private static final String SIZE_SUFFIX = ".size";

	private final File outDirectory;
	private final TextureIndex textureIndex;

	public TextureGenerator(TextureIndex textureIndex, File outDirectory) {
		this.textureIndex = textureIndex;
		this.outDirectory = outDirectory;
	}

	void processTextures(File resourceDirectory) {
		processTextures(resourceDirectory, resourceDirectory);
	}

	private void processTextures(File resourceDirectory, File directory) {
		File[] files = directory.listFiles();
		if (files == null) {
			return;
		}

		Arrays.stream(files)
				.parallel()
				.filter(File::isDirectory)
				.forEach(subDirectory -> processTextures(resourceDirectory, subDirectory));
		Arrays.stream(files)
				.parallel()
				.filter(File::isFile)
				.filter(file -> file.getName().endsWith(".png"))
				.filter(file -> !file.getName().endsWith(".t.png")) // torso files are added with their corresponding image file
				.forEach(file -> processTexturesFile(resourceDirectory, file));
	}

	private void processTexturesFile(File baseDirectory, File file) {
		ImageData imageData = addIdToTexture(baseDirectory, file);
		storeImageData(imageData);
	}

	private ImageData addIdToTexture(File baseDirectory, File imageFile) {
		String name = calculateName(baseDirectory, imageFile);

		ImageData imageData = new ImageData();
		imageData.name = name;
		imageData.data = getImage(imageFile);

		File torsoFile = new File(imageFile.getPath().replace(".png", ".t.png"));

		if (torsoFile.exists()) {
			imageData.torso = getImage(torsoFile);
		}

		if (imageData.data == null) {
			System.err.println("WARNING: loading image " + name + ": No image file found.");
		}
		return imageData;
	}

	private String calculateName(File baseDirectory, File file) {
		StringBuilder name = new StringBuilder(file.getName().replaceFirst("(\\.t)?\\.png$", ""));
		File currentFile = file.getParentFile();

		while (!baseDirectory.equals(currentFile)) {
			name.insert(0, currentFile.getName() + "/");
			currentFile = currentFile.getParentFile();
		}

		return name.toString();
	}

	private void storeImageData(ImageData imageData) {
		storeImage(imageData.name, imageData.data, imageData.torso);
	}

	private void storeImage(String name, ProvidedImage data, ProvidedImage torso) {
		try {
			if (data != null) {
				int torsoIndex = -1;

				if (torso != null) {
					torsoIndex = storeImage(name + "_torso", torso, -1);
				}

				storeImage(name, data, torsoIndex);
			}
		} catch (Throwable t) {
			System.err.println("WARNING: Problem writing image " + name + ". Problem was: " + t.getMessage());
		}
	}

	private int storeImage(String name, ProvidedImage image, int torsoIndex) throws IOException {
		int texture = textureIndex.getNextTextureIndex();
		addAsNewImage(image, texture);
		int index = textureIndex.registerTexture(name, texture, image.getOffsetX(), image.getOffsetY(), image.getImageWidth(), image.getImageHeight(), torsoIndex);
		System.out.println("Texture file #" + texture + ": add name=" + name + " using values x=" + image.getOffsetX() + ".." + (image.getOffsetX() + image.getImageWidth()) + ", y=" + image
				.getOffsetY() + ".." + (image.getOffsetY() + image.getImageHeight()));
		return index;
	}

	// This is slow.
	private void addAsNewImage(ProvidedImage data, int texture) throws IOException {
		File imageFile = new File(outDirectory, "images_" + texture);
		TextureFile.write(imageFile, data.getData(), data.getTextureWidth(), data.getTextureHeight());
	}

	private ProvidedImage getImage(File imageFile) {
		try {
			BufferedImage image = ImageIO.read(imageFile);
			int[] size = getTupleFile(imageFile, SIZE_SUFFIX)
					.orElse(new int[] {image.getWidth(), image.getHeight()});

			int[] offsets = getTupleFile(imageFile, OFFSET_SUFFIX)
					.orElse(new int[2]);

			return new ProvidedImage(image, offsets, size);
		} catch (Throwable t) {
			System.err.println("WARNING: Problem reading image " + imageFile + ". Problem was: " + t.getMessage());
			return null;
		}
	}
	private Optional<int[]> getTupleFile(File file, String suffix) {
		return getTupleFile(new File(file.getPath() + suffix));
	}

	private Optional<int[]> getTupleFile(File file) {

		if (!file.exists()) {
			return Optional.empty();
		}


		String content;
		try {
			content = Files.readString(file.toPath());
		} catch (IOException e) {
			System.err.println("WARNING: " + file + " could not be read: ");
			e.printStackTrace();
			return Optional.empty();
		}

		Matcher matcher = TUPLE_PATTERN.matcher(content);
		if (!matcher.matches()) {
			System.err.println("WARNING: " + file + " has wrong file format!");
			return Optional.empty();
		}

		int[] values = new int[2];
		values[0] = Integer.parseInt(matcher.group("first"));
		values[1] = Integer.parseInt(matcher.group("second"));
		return Optional.of(values);
	}
}
