/*******************************************************************************
 * Copyright (c) 2015, 2016
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
 *******************************************************************************/
package jsettlers.common.buildings;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import jsettlers.common.buildings.jobs.IBuildingJob;
import jsettlers.common.buildings.loader.BuildingFile;
import jsettlers.common.buildings.stacks.ConstructionStack;
import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.images.ImageLink;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.RelativePoint;

import static jsettlers.common.player.ECivilisation.REPLACE_ME;

/**
 * This interface defines the main building type.
 * 
 * @author Michael Zangl
 * @author Andreas Eberle
 */
public enum EBuildingType {
	STONECUTTER,
	FORESTER,
	LUMBERJACK,
	SAWMILL,

	SULFURMINE,
	COALMINE,
	IRONMINE,
	GOLDMINE,
	GOLDMELT,
	IRONMELT,
	TOOLSMITH,
	WEAPONSMITH,

	FARM,
	PIG_FARM,
	/**
	 * Needs to implement {@link IBuilding.IMill}
	 */
	MILL,
	WATERWORKS,
	SLAUGHTERHOUSE,
	BAKER,
	FISHER,
	WINEGROWER,
	CHARCOAL_BURNER,
	DONKEY_FARM,

	SMALL_LIVINGHOUSE,
	MEDIUM_LIVINGHOUSE,
	BIG_LIVINGHOUSE,

	LOOKOUT_TOWER,
	TOWER,
	BIG_TOWER,
	CASTLE,
	HOSPITAL,
	BARRACK,

	DOCKYARD,
	HARBOR,
	STOCK,

	TEMPLE,
	BIG_TEMPLE,

	MARKET_PLACE;

	/**
	 * A copy of {@link #values()}. Do not modify this array. This is intended for quicker access to this value.
	 */
	public static final EBuildingType[] VALUES = EBuildingType.values();

	/**
	 * The number of buildings in the {@link #VALUES} array.
	 */
	public static final int NUMBER_OF_BUILDINGS = VALUES.length;
	public static final EnumSet<EBuildingType> MILITARY_BUILDINGS = EnumSet.of(TOWER, BIG_TOWER, CASTLE);

	/**
	 * The ordinal of this type. Yields more performance than using {@link #ordinal()}
	 */
	public final int ordinal;

	private final Map<ECivilisation, BuildingVariant> buildingVariants = new EnumMap<>(ECivilisation.class);

	/**
	 * Constructs an enum object.
	 */
	EBuildingType() {
		this.ordinal = ordinal();

		for(ECivilisation civilisation : ECivilisation.VALUES) {
			try {
				buildingVariants.put(civilisation, new BuildingVariant(this, civilisation));
			} catch(Throwable t) {}
		}
	}

	public BuildingVariant getVariant(ECivilisation civilisation) {
		return buildingVariants.get(civilisation);
	}

	/**
	 * Gets the job a worker for this building should start with.
	 * 
	 * @return That {@link IBuildingJob}
	 */
	public final IBuildingJob getStartJob() {
		return buildingVariants.get(REPLACE_ME).getStartJob();
	}

	/**
	 * Gets the type of worker required for the building.
	 * 
	 * @return The worker or <code>null</code> if no worker is required.
	 */
	public final EMovableType getWorkerType() {
		return buildingVariants.get(REPLACE_ME).getWorkerType();
	}

	/**
	 * Gets the position of the door for this building.
	 * 
	 * @return The door.
	 */
	public final RelativePoint getDoorTile() {
		return buildingVariants.get(REPLACE_ME).getDoorTile();
	}

	/**
	 * Gets a list of blocked positions.
	 * 
	 * @return The list of blocked positions.
	 */
	public final RelativePoint[] getBlockedTiles() {
		return buildingVariants.get(REPLACE_ME).getBlockedTiles();
	}

	/**
	 * Gets the tiles that are protected by this building. On thse tiles, no other buildings may be build.
	 * 
	 * @return The tiles as array.
	 */
	public final RelativePoint[] getProtectedTiles() {
		return buildingVariants.get(REPLACE_ME).getProtectedTiles();
	}

	/**
	 * Gets the images needed to display this building. They are rendered in the order provided.
	 * 
	 * @return The images
	 */
	public final ImageLink[] getImages() {
		return buildingVariants.get(REPLACE_ME).getImages();
	}

	/**
	 * Gets the images needed to display this building while it si build. They are rendered in the order provided.
	 * 
	 * @return The images
	 */
	public final ImageLink[] getBuildImages() {
		return buildingVariants.get(REPLACE_ME).getBuildImages();
	}

	/**
	 * Gets the gui image that is displayed in the building selection dialog.
	 * 
	 * @return The image. It may be <code>null</code>
	 */
	public final ImageLink getGuiImage() {
		return buildingVariants.get(REPLACE_ME).getGuiImage();
	}

	/**
	 * Gets the working radius of the building. If it is 0, the building does not support a working radius.
	 *
	 * @return The radius.
	 */
	public final short getWorkRadius() {
		return buildingVariants.get(REPLACE_ME).getWorkRadius();
	}

	/**
	 * Gets the default work center for the building type.
	 * 
	 * @return The default work center position.
	 */
	public final RelativePoint getDefaultWorkcenter() {
		return buildingVariants.get(REPLACE_ME).getDefaultWorkcenter();
	}

	/**
	 * Gets the position of the flag for this building. The flag type is determined by the building itself.
	 * 
	 * @return The flag position.
	 */
	public final RelativePoint getFlag() {
		return buildingVariants.get(REPLACE_ME).getFlag();
	}

	/**
	 * Gets the positions where the bricklayers should stand to build the house.
	 * 
	 * @return The positions.
	 * @see RelativeBricklayer
	 */
	public final RelativeBricklayer[] getBricklayers() {
		return buildingVariants.get(REPLACE_ME).getBricklayers();
	}

	/**
	 * Gets the positions of the build marks (sticks) for this building.
	 * 
	 * @return The positions of the marks.
	 */
	public final RelativePoint[] getBuildMarks() {
		return buildingVariants.get(REPLACE_ME).getBuildMarks();
	}

	/**
	 * Gets the ground types this building can be placed on.
	 * 
	 * @return The ground types.
	 */
	public final Set<ELandscapeType> getGroundTypes() {
		return buildingVariants.get(REPLACE_ME).getGroundTypes();
	}

	/**
	 * Queries a building job with the given name that needs to be accessible from the start job.
	 * 
	 * @param jobname
	 *            The name of the job.
	 * @return The job if found.
	 * @throws IllegalArgumentException
	 *             If the name was not found.
	 */
	public final IBuildingJob getJobByName(String jobname) {
		return buildingVariants.get(REPLACE_ME).getJobByName(jobname);
	}

	/**
	 * Gets the materials required to build this building and where to place them.
	 * 
	 * @return The array of material stacks.
	 */
	public ConstructionStack[] getConstructionStacks() {
		return buildingVariants.get(REPLACE_ME).getConstructionStacks();
	}

	/**
	 * Get the amount of material required to build this house. Usually the number of stone + planks.
	 * 
	 * @return The number of materials required to construct the building.
	 */
	public final byte getNumberOfConstructionMaterials() {
		return buildingVariants.get(REPLACE_ME).getNumberOfConstructionMaterials();
	}

	/**
	 * Gets the request stacks required to operate this building.
	 * 
	 * @return The request stacks.
	 */
	public RelativeStack[] getRequestStacks() {
		return buildingVariants.get(REPLACE_ME).getRequestStacks();
	}

	/**
	 * Gets the positions where the building should offer materials.
	 * 
	 * @return The offer positions.
	 */
	public RelativeStack[] getOfferStacks() {
		return buildingVariants.get(REPLACE_ME).getOfferStacks();
	}

	/**
	 * Checks if this building is a mine.
	 * 
	 * @return <code>true</code> iff this building is a mine.
	 */
	public boolean isMine() {
		return buildingVariants.get(REPLACE_ME).isMine();
	}

	public boolean needsFlattenedGround() {
		return buildingVariants.get(REPLACE_ME).needsFlattenedGround();
	}

	/**
	 * Checks if this building is a military building.
	 * 
	 * @return <code>true</code> iff this is a military building.
	 */
	public boolean isMilitaryBuilding() {
		return MILITARY_BUILDINGS.contains(this);
	}
}
