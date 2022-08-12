package jsettlers.graphics.image.reader.custom.graphics;

import jsettlers.common.images.AnimationSequence;
import jsettlers.graphics.image.Image;
import jsettlers.graphics.image.reader.DatFileReader;
import jsettlers.graphics.image.reader.EmptyDatFile;
import jsettlers.graphics.image.reader.WrappedAnimation;
import jsettlers.graphics.image.sequence.ArraySequence;
import jsettlers.graphics.image.sequence.Sequence;
import jsettlers.graphics.image.sequence.SequenceList;
import jsettlers.graphics.map.draw.ImageProvider;

public class CustomEgyptianBuildingsDatFile extends EmptyDatFile {
	private final DatFileReader fallback;
	private final ImageProvider imageProvider;

	CustomEgyptianBuildingsDatFile(DatFileReader fallback, ImageProvider imageProvider) {
		this.fallback = fallback;
		this.imageProvider = imageProvider;
	}

	@Override
	public SequenceList<Image> getSettlers() {
		return new SequenceList<>() {

			private SequenceList<Image> fallbackSequence = fallback.getSettlers();

			@Override
			public int size() {
				return Math.max(30, fallbackSequence.size());
			}

			@Override
			public Sequence<Image> get(int index) {

				if (index == 1) {
					return new WrappedAnimation(imageProvider, new AnimationSequence("egyptian_sawmill_sawmill", 0, 2));
				}
				if (index < fallbackSequence.size()) {
					Sequence<Image> sequence = fallbackSequence.get(index);

					if (sequence != null) {
						return sequence;
					}
				}
				return ArraySequence.getNullSequence();
			}
		};
	}
}
