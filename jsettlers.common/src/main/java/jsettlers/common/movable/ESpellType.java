package jsettlers.common.movable;

import jsettlers.common.images.ImageLink;

public enum ESpellType {
	EYE(10, false,"original_14_GUI_246"),
	IRRIGATE(10, true, "original_14_GUI_249"),
	GREEN_THUMB(15, true, "original_14_GUI_252"),
	DEFEATISM(15, true, "original_14_GUI_255"),
	GIFTS(20, true, "original_14_GUI_258"),
	GILDING(20, true, "original_14_GUI_261"),
	CURSE_MOUNTAIN(25, true, "original_14_GUI_264"),
	DEFECT(40, true, "original_14_GUI_267");

	private short manna;
	private ImageLink imageLink;
	private boolean forcePresence;

	ESpellType(int manna, boolean forcePresence, String imageLink) {
		this.manna = (short) manna;
		this.forcePresence = forcePresence;
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

	public boolean forcePresence() {
		return forcePresence;
	}

	public static final int GILDING_MAX_IRON = 40;

	public static final int DEFEATISM_MAX_SOLDIERS = 20;

	public static final int GIFTS_MAX_STACKS = 5;
	public static final int GIFTS_RADIUS = 3;

	public static final int CURSE_MOUNTAIN_RADIUS = 5;
	public static final float CURSE_MOUNTAIN_RESOURCE_MOD = 0.5f;

	public static final long DEFECT_MAX_ENEMIES = 10;

	public static final int IRRIGATE_RADIUS = 7;

	public static final short EYE_RADIUS = 10;
	public static final float EYE_TIME = 6;
}
