package jsettlers.logic.movable.strategies.military;

import java.util.List;

import jsettlers.common.landscape.ELandscapeType;
import static jsettlers.common.landscape.ELandscapeType.*;

import jsettlers.common.map.shapes.MapCircle;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.ESpellType;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.utils.coordinates.CoordinateStream;
import jsettlers.common.utils.mutables.MutableInt;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableStrategy;
import jsettlers.logic.movable.interfaces.ILogicMovable;

public class MageStrategy extends MovableStrategy {
	public MageStrategy(Movable movable) {
		super(movable);
	}


	private boolean spellAbortPath = false;
	private ShortPoint2D spellLocation = null;
	private ESpellType spell = null;

	private CoordinateStream spellRegion(int radius) {
		return new MapCircle(spellLocation, radius).stream()
				.filterBounds(getGrid().getWidth(), getGrid().getHeight());
	}

	private int teamId(int x, int y) {
		IPlayer player = getGrid().getPlayerAt(new ShortPoint2D(x, y));
		return player != null ? player.getTeamId() : -1;
	}

	private int teamId(ILogicMovable movable) {
		return movable.getPlayer().getTeamId();
	}

	private void replace(CoordinateStream at, ELandscapeType from, ELandscapeType to) {
		at.filter((x, y) -> getGrid().getLandscapeTypeAt(x, y) == from).forEach((x, y) -> getGrid().changeTerrainTo(x, y, to));
	}

	@Override
	protected boolean checkPathStepPreconditions(ShortPoint2D pathTarget, int step) {
		if(spellAbortPath && !pathTarget.equals(spellLocation)) {
			abortCasting();
		}

		if(spellLocation == null || spellLocation.getOnGridDistTo(movable.getPosition()) > Constants.MAGE_CAST_DISTANCE) return true;

		if(movable.getPlayer().getMannaInformation().useSpell(spell)) {
			switch(spell) {
				case GILDING:
					CoordinateStream possibleLocations = spellRegion(Constants.SPELL_EFFECT_RADIUS).filter((x, y) -> !getGrid().isBlocked(x, y));
					final MutableInt remainingPlace = new MutableInt(0);

					possibleLocations.filter((x, y) -> getGrid().canTakeMaterial(new ShortPoint2D(x, y), EMaterialType.IRON))
							.limit(ESpellType.GILDING_MAX_IRON).forEach((x, y) -> {
								getGrid().takeMaterial(new ShortPoint2D(x, y), EMaterialType.IRON);
								remainingPlace.value++;
							});

					for(ShortPoint2D point : possibleLocations.toList()) {
						while(remainingPlace.value > 0 && getGrid().dropMaterial(point, EMaterialType.GOLD, true, false)) {
							remainingPlace.value--;
						}
					}

					//TODO play sound 95 and play animation 1:121
					if(remainingPlace.value > 0) System.err.println("Couldn't place " + remainingPlace + "gold");
					break;
				case DEFEATISM:
					spellRegion(Constants.SPELL_EFFECT_RADIUS).map((x, y) -> getGrid().getMovableAt(x, y))
							.filter(lm -> lm!=null&&lm.isAlive()&&lm.isAttackable())
							.filter(lm -> teamId(lm) != teamId(movable))
							.limit(ESpellType.DEFEATISM_MAX_SOLDIERS)
							.forEach(movable -> movable.addEffect(EEffectType.DEFEATISM));

					//TODO play sound and play animation 1:116
					break;
				case GIFTS:
					spellRegion(ESpellType.GIFTS_RADIUS).filter((x, y) -> !getGrid().isBlockedOrProtected(x, y))
							.filter((x, y) -> teamId(x, y) == teamId(movable))
							.limit(MatchConstants.random().nextInt(ESpellType.GIFTS_MAX_STACKS+1))
							.forEach((x, y) -> {
								ShortPoint2D at = new ShortPoint2D(x, y);
								//TODO only give useful stuff
								EMaterialType type = EMaterialType.values()[MatchConstants.random().nextInt(EMaterialType.values().length)];
								int size = MatchConstants.random().nextInt(9);
								for(int i = 0; i != size; i++) getGrid().dropMaterial(at, type, true, false);
							});
					//TODO play sound and play animation 1:114
					break;
				case CURSE_MOUNTAIN:
					spellRegion(ESpellType.CURSE_MOUNTAIN_RADIUS)
							.filter((x, y) -> teamId(x, y) != teamId(movable))
							.forEach((x, y) -> getGrid().tryCursingLocation(new ShortPoint2D(x, y)));
					//TODO play sound and play animation 1:120
					break;
				case DEFECT:
					spellRegion(Constants.SPELL_EFFECT_RADIUS).map((x, y) -> getGrid().getMovableAt(x, y))
							.filter(lm -> lm!=null&&lm.isAlive()&&lm.isAttackable())
							.filter(lm -> teamId(lm) != teamId(movable))
							.limit(ESpellType.DEFECT_MAX_ENEMIES)
							.forEach(lm -> lm.defectTo(movable.getPlayer()));
					//TODO play sound and play animation 1:119
					break;
				case IRRIGATE:
					CoordinateStream affectedRegion = spellRegion(ESpellType.IRRIGATE_RADIUS);
					List<ShortPoint2D> flattenedRegion = affectedRegion.filter((x, y) -> FLATTENED_DESERTS.contains(getGrid().getLandscapeTypeAt(x, y))).toList();
					//all desert fields can always be replaced with dry grass
					replace(affectedRegion, DESERTBORDEROUTER, DRY_GRASS);
					replace(affectedRegion, DESERTBORDER, DRY_GRASS);
					replace(affectedRegion, SHARP_FLATTENED_DESERT, DESERT);
					replace(affectedRegion, FLATTENED_DESERT, DESERT);

					affectedRegion.filter((x, y) -> getGrid().getLandscapeTypeAt(x, y) == DESERT)
							.filter((x, y) ->
									EDirection.neighborStream(new ShortPoint2D(x, y))
											.filter((tx, ty) -> !DRY_GRASS_NEIGHBORS.contains(getGrid().getLandscapeTypeAt(tx, ty)))
											.isEmpty())
							.forEach((x, y) -> getGrid().changeTerrainTo(x, y, DRY_GRASS));

					// if there aren't any fields that can't be next to grass, set it to grass
					affectedRegion.filter((x, y) -> getGrid().getLandscapeTypeAt(x, y) == DRY_GRASS)
							.filter((x, y) ->
									EDirection.neighborStream(new ShortPoint2D(x, y))
											.filter((tx, ty) -> !GRASS_NEIGHBORS.contains(getGrid().getLandscapeTypeAt(tx, ty)))
											.isEmpty())
							.forEach((x, y) -> getGrid().changeTerrainTo(x, y, GRASS));

					for(ShortPoint2D point : flattenedRegion) {
						if(EDirection.neighborStream(point).filter((x, y) -> !FLATTENED_NEIGHBORS.contains(getGrid().getLandscapeTypeAt(x, y))).isEmpty()) {
							getGrid().changeTerrainTo(point.x, point.y, FLATTENED);
						}
					}
					//TODO play sound and play animation 1:125
					break;
				default:
					System.err.println("unimplemented spell: " + spell);
					 break;
			}
		} else {
			movable.getPlayer().showMessage(SimpleMessage.castFailed(spellLocation, "spell_failed"));
		}

		boolean abortPath = spellAbortPath;
		abortCasting();

		return !abortPath;
	}

	@Override
	protected boolean canBeControlledByPlayer() {
		return true;
	}

	private void abortCasting() {
		spellLocation = null;
		spell = null;
		spellAbortPath = false;
	}

	@Override
	protected void stopOrStartWorking(boolean stop) {
		if(stop) {
			movable.moveTo(movable.getPosition());
		}
	}

	public void castSpell(ShortPoint2D at, ESpellType spell) {
		spellLocation = new ShortPoint2D(at.x, at.y);
		this.spell = spell;

		if(movable.getAction() != EMovableAction.WALKING) {
			spellAbortPath = true;
			movable.moveTo(spellLocation);
		}
	}
}
