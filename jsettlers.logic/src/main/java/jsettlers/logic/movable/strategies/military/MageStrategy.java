package jsettlers.logic.movable.strategies.military;

import java.util.ArrayList;
import java.util.List;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.landscape.ELandscapeType;
import static jsettlers.common.landscape.ELandscapeType.*;

import jsettlers.common.map.shapes.MapCircle;
import jsettlers.common.map.shapes.MapNeighboursArea;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
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

	private boolean castSpell() {
		if(spellLocation == null || (spell.forcePresence() && spellLocation.getOnGridDistTo(movable.getPosition()) > Constants.MAGE_CAST_DISTANCE)) return true;

		int sound = -1;
		int animation = -1;
		float duration = 2;
		List<ShortPoint2D> effectLocations = new ArrayList<>();

		if(movable.getPlayer().getMannaInformation().useSpell(spell)) {
			switch(spell) {
				case GILDING:
					CoordinateStream possibleLocations = spellRegion(Constants.SPELL_EFFECT_RADIUS).filter((x, y) -> !getGrid().isBlocked(x, y));
					final MutableInt remainingPlace = new MutableInt(0);

					possibleLocations.filter((x, y) -> getGrid().canTakeMaterial(new ShortPoint2D(x, y), EMaterialType.IRON))
							.limit(ESpellType.GILDING_MAX_IRON).forEach((x, y) -> {
								ShortPoint2D point = new ShortPoint2D(x, y);
								getGrid().takeMaterial(point, EMaterialType.IRON);
								effectLocations.add(point);
								remainingPlace.value++;
							});

					for(ShortPoint2D point : possibleLocations.toList()) {
						while(remainingPlace.value > 0 && getGrid().dropMaterial(point, EMaterialType.GOLD, true, false)) {
							remainingPlace.value--;
						}
					}

					sound = 95;
					animation = 121;
					if(remainingPlace.value > 0) System.err.println("Couldn't place " + remainingPlace + "gold");
					break;
				case DEFEATISM:
					spellRegion(Constants.SPELL_EFFECT_RADIUS).map((x, y) -> getGrid().getMovableAt(x, y))
							.filter(lm -> lm!=null&&lm.isAlive()&&lm.isAttackable())
							.filter(lm -> teamId(lm) != teamId(movable))
							.limit(ESpellType.DEFEATISM_MAX_SOLDIERS)
							.forEach(movable -> movable.addEffect(EEffectType.DEFEATISM));
					effectLocations.add(spellLocation);
					animation = 116;
					break;
				case GIFTS:
					spellRegion(ESpellType.GIFTS_RADIUS).filter((x, y) -> !getGrid().isBlockedOrProtected(x, y))
							.filter((x, y) -> teamId(x, y) == -1 || teamId(x, y) == teamId(movable))
							.limit(MatchConstants.random().nextInt(ESpellType.GIFTS_MAX_STACKS+1))
							.forEach((x, y) -> {
								ShortPoint2D at = new ShortPoint2D(x, y);
								//TODO only give useful stuff
								EMaterialType type = EMaterialType.values()[MatchConstants.random().nextInt(EMaterialType.values().length)];
								int size = MatchConstants.random().nextInt(9);
								for(int i = 0; i != size; i++) getGrid().dropMaterial(at, type, true, false);
								effectLocations.add(at);
							});
					duration = 1;
					sound = 78;
					animation = 114;
					break;
				case CURSE_MOUNTAIN:
					spellRegion(ESpellType.CURSE_MOUNTAIN_RADIUS)
							.filter((x, y) -> teamId(x, y) == -1 || teamId(x, y) != teamId(movable))
							.forEach((x, y) -> getGrid().tryCursingLocation(new ShortPoint2D(x, y)));
					effectLocations.add(spellLocation);
					sound = 100;
					animation = 120;
					break;
				case DEFECT:
					spellRegion(Constants.SPELL_EFFECT_RADIUS).map((x, y) -> getGrid().getMovableAt(x, y))
							.filter(lm -> lm!=null&&lm.isAlive()&&lm.isAttackable())
							.filter(lm -> teamId(lm) != teamId(movable))
							.limit(ESpellType.DEFECT_MAX_ENEMIES)
							.forEach(lm -> {
								lm.defectTo(movable.getPlayer());
								effectLocations.add(lm.getPosition());
							});
					sound = 95;
					animation = 119;
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
									MapNeighboursArea.stream(x, y)
											.filter((tx, ty) -> !DRY_GRASS_NEIGHBORS.contains(getGrid().getLandscapeTypeAt(tx, ty)))
											.isEmpty())
							.forEach((x, y) -> getGrid().changeTerrainTo(x, y, DRY_GRASS));

					// if there aren't any fields that can't be next to grass, set it to grass
					affectedRegion.filter((x, y) -> getGrid().getLandscapeTypeAt(x, y) == DRY_GRASS)
							.filter((x, y) ->
									MapNeighboursArea.stream(x, y)
											.filter((tx, ty) -> !GRASS_NEIGHBORS.contains(getGrid().getLandscapeTypeAt(tx, ty)))
											.isEmpty())
							.forEach((x, y) -> getGrid().changeTerrainTo(x, y, GRASS));

					for(ShortPoint2D point : flattenedRegion) {
						if(MapNeighboursArea.stream(point.x, point.y).filter((x, y) -> !FLATTENED_NEIGHBORS.contains(getGrid().getLandscapeTypeAt(x, y))).isEmpty()) {
							getGrid().changeTerrainTo(point.x, point.y, FLATTENED);
						}
					}
					effectLocations.add(spellLocation);
					animation = 125;
					break;
				case GREEN_THUMB:
					spellRegion(Constants.SPELL_EFFECT_RADIUS).map((x, y) -> getGrid().getMovableAt(x, y))
							.filter(lm -> lm!=null&&lm.isAlive())
							.filter(lm -> lm.getMovableType() == EMovableType.FARMER &&
									lm.getMovableType() == EMovableType.WINEGROWER &&
									lm.getMovableType() == EMovableType.FORESTER
							).limit(1).forEach(lm -> lm.addEffect(EEffectType.GREEN_THUMB));
					effectLocations.add(spellLocation);
					break;
				case ROMAN_EYE:
					getGrid().addEyeMapObject(spellLocation, ESpellType.ROMAN_EYE_RADIUS, ESpellType.ROMAN_EYE_TIME, movable.getPlayer());
					effectLocations.add(spellLocation);
					sound = 80;
					animation = 126;
					break;
			}

			for(ShortPoint2D point : effectLocations) {
				getGrid().addSelfDeletingMapObject(point, sound, animation, duration, movable.getPlayer());
			}

		} else {
			movable.getPlayer().showMessage(SimpleMessage.castFailed(spellLocation, "spell_failed"));
		}

		playAction(EMovableAction.ACTION1, 1);

		boolean abortPath = spellAbortPath;
		abortCasting();

		return !abortPath;
	}

	@Override
	protected boolean checkPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		if(spellAbortPath && !pathTarget.equals(spellLocation)) {
			abortCasting();
		}

		return castSpell();
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
			movable.moveTo(movable.getPosition(), EMoveToType.FORCED);
		}
	}

	public void castSpell(ShortPoint2D at, ESpellType spell) {
		spellAbortPath = movable.getAction() != EMovableAction.WALKING;
		spellLocation = new ShortPoint2D(at.x, at.y);
		this.spell = spell;

		if(!castSpell()) return;

		if(movable.getAction() != EMovableAction.WALKING) {
			movable.moveTo(spellLocation, EMoveToType.FORCED);
		}
	}
}
