package jsettlers.common.movable;

import jsettlers.common.images.ImageLink;

public enum ESpellType {
	EYE(10, "original_14_GUI_246"),
	IRRIGATE(10, "original_14_GUI_249"),
	GREEN_THUMB(15, "original_14_GUI_252"),
	DEFEATISM(15, "original_14_GUI_255"),
	GIFTS(20, "original_14_GUI_258"),
	GILDING(20, "original_14_GUI_261"),
	LESS_RESOURCES(25, "original_14_GUI_264"),
	DEFECT(40, "original_14_GUI_267");

	private short manna;
	private ImageLink imageLink;

	ESpellType(int manna, String imageLink) {
		this.manna = (short) manna;
		this.imageLink = ImageLink.fromName(imageLink, 0);
	}

	public ImageLink getImageLink() {
		return imageLink;
	}

	public short getBaseCost() {
		return manna;
	}

	public short getMannaCost(int count) {
		return (short)(manna*(count/10f+1));
	}

	public static final int GILDING_MAX_IRON = 40;
	public static final int DEFEATISM_MAX_SOLDIERS = 20;

	public static final int GIFTS_MAX_STACKS = 5;
	public static final int GIFTS_RADIUS = 3;
}
