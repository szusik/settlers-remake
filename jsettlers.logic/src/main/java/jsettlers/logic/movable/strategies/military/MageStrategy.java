package jsettlers.logic.movable.strategies.military;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import java8.util.function.Function;
import jsettlers.algorithms.terraform.LandscapeEditor;
import jsettlers.common.action.EMoveToType;
import static jsettlers.common.landscape.ELandscapeType.*;

import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.map.shapes.MapCircle;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
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
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.ILogicMovable;

public class MageStrategy extends MovableStrategy {
	public MageStrategy(Movable movable) {
		super(movable);
	}

	private transient final LandscapeEditor terraformHandle = new LandscapeEditor(pt -> getGrid().getLandscapeTypeAt(pt.x, pt.y), (pt, type) -> getGrid().setLandscape(pt.x, pt.y, type));

	private ShortPoint2D spellLocation = null;
	private ESpellType spell = null;

	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
	}

	private CoordinateStream spellRegion() {
		return spellRegion(Constants.SPELL_EFFECT_RADIUS);
	}

	private CoordinateStream spellRegion(int radius) {
		return spellRegion(spellLocation, radius);
	}

	private CoordinateStream spellRegion(ShortPoint2D at, int radius) {
		return new MapCircle(at, radius).stream()
				.filterBounds(getGrid().getWidth(), getGrid().getHeight());
	}

	private CoordinateStream sort(CoordinateStream stream) {
		// TODO distant positions should come last
		return stream;
	}

	private int teamId(int x, int y) {
		IPlayer player = getGrid().getPlayerAt(new ShortPoint2D(x, y));
		return player != null ? player.getTeamId() : -1;
	}

	private int teamId(ILogicMovable movable) {
		return movable.getPlayer().getTeamId();
	}

	@Override
	protected void action() {
		if(!willCastSpell()) return;

		int sound = -1;
		int animation = -1;
		float duration = 2;
		List<ShortPoint2D> effectLocations = new ArrayList<>();

		if(movable.getPlayer().getMannaInformation().useSpell(spell)) {
			switch(spell) {
				case GILDING:
					convertMaterial(EMaterialType.IRON, EMaterialType.GOLD, ESpellType.GILDING_MAX_IRON, effectLocations);

					sound = 95;
					animation = 121;
					break;
				case CONVERT_FOOD:
					convertMaterial(EMaterialType.FISH, EMaterialType.MEAT, ESpellType.CONVERT_FOOD_MAX_FISH, effectLocations);

					sound = 95;
					animation = 121;
					break;
				case DEFEATISM:
					sort(spellRegion()).map((x, y) -> getGrid().getMovableAt(x, y))
							.filter(lm -> lm!=null&&lm.isAlive()&&lm.getMovableType().isSoldier())
							.filter(lm -> teamId(lm) != teamId(movable))
							.limit(ESpellType.DEFEATISM_MAX_SOLDIERS)
							.forEach(movable -> movable.addEffect(EEffectType.DEFEATISM));
					effectLocations.add(spellLocation);
					animation = 116;
					break;
				case INCREASE_MORALE:
					sort(spellRegion()).map((x, y) -> getGrid().getMovableAt(x, y))
							.filter(lm -> lm!=null&&lm.isAlive()&&lm.getMovableType().isSoldier())
							.filter(lm -> teamId(lm) == teamId(movable))
							.limit(ESpellType.INCREASE_MORALE_MAX_SOLDIERS)
							.forEach(movable -> movable.addEffect(EEffectType.INCREASED_MORALE));
					effectLocations.add(spellLocation);
					animation = 115;
					break;
				case SEND_FOES:
					ShortPoint2D priestPos = movable.getPosition();

					Queue<ILogicMovable> sendEnemies = new ArrayDeque<>(ESpellType.SEND_FOES_MAX_SOLDIERS);
					sort(spellRegion(priestPos, Constants.SPELL_EFFECT_RADIUS))
							.map((x, y) -> getGrid().getMovableAt(x, y))
							.filter(lm -> lm!=null&&lm.isAlive())
							.filter(lm -> teamId(lm) != teamId(movable))
							.filter(lm -> ESpellType.SENDABLE_MOVABLE_TYPES.contains(lm.getMovableType()))
							.limit(ESpellType.SEND_FOES_MAX_SOLDIERS)
							.forEach(sendEnemies::add);


					sort(spellRegion(spellLocation, Constants.SPELL_EFFECT_RADIUS))
							.filter((x, y) -> !getGrid().isBlockedOrProtected(x, y))
							.forEach((x, y) -> {
								ILogicMovable send = sendEnemies.poll();
								if(send != null) {
									ShortPoint2D movPos = send.getPosition();
									send.setPosition(new ShortPoint2D(x, y));
									effectLocations.add(movPos);
								}
							});
					animation = 121;
					break;
				case CURSE_BOWMAN:
					sort(spellRegion()).map((x, y) -> getGrid().getMovableAt(x, y))
							.filter(lm -> lm!=null&&lm.isAlive()&&lm.getMovableType().isBowman())
							.filter(lm -> teamId(lm) != teamId(movable))
							.limit(ESpellType.CURSE_BOWMAN_MAX_BOWMAN)
							.forEach(movable -> movable.convertTo(EMovableType.PIONEER));
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
					sort(spellRegion()).map((x, y) -> getGrid().getMovableAt(x, y))
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
					convertLandscape(ESpellType.IRRIGATE_RADIUS, DESERT_TYPES::contains, GRASS, FLATTENED_DESERTS::contains, FLATTENED);

					effectLocations.add(spellLocation);
					animation = 125;
					break;
				case DESERTIFICATION:
					convertLandscape(ESpellType.DESERTIFICATION_RADIUS, ELandscapeType::isGrass, DESERT, (type) -> type == FLATTENED, FLATTENED_DESERT);

					effectLocations.add(spellLocation);
					animation = 127;
					break;
				case DRAIN_MOOR:
					convertLandscape(ESpellType.DRAIN_MOOR_RADIUS, MOOR_TYPES::contains, GRASS, null, null);

					effectLocations.add(spellLocation);
					animation = 127;
					break;
				case GREEN_THUMB:
					sort(spellRegion()).map((x, y) -> getGrid().getMovableAt(x, y))
							.filter(lm -> lm!=null&&lm.isAlive())
							.filter(lm -> lm.getMovableType() == EMovableType.FARMER ||
									lm.getMovableType() == EMovableType.WINEGROWER ||
									lm.getMovableType() == EMovableType.FORESTER
							).limit(ESpellType.GREEN_THUMB_MAX_SETTLERS).forEach(lm -> lm.addEffect(EEffectType.GREEN_THUMB));
					effectLocations.add(spellLocation);
					break;
				case ROMAN_EYE:
					getGrid().addEyeMapObject(spellLocation, ESpellType.ROMAN_EYE_RADIUS, ESpellType.ROMAN_EYE_TIME, movable.getPlayer());
					effectLocations.add(spellLocation);
					sound = 80;
					animation = 126;
					break;
				case BURN_FOREST:
					MutableInt remainingTrees = new MutableInt(ESpellType.BURN_FOREST_MAX_TREE_COUNT);
					sort(spellRegion()).forEach((x, y) -> {
						if(remainingTrees.value > 0 && getGrid().executeSearchType(movable, new ShortPoint2D(x, y), ESearchType.BURNABLE_TREE)) {
							remainingTrees.value--;
						}
					});
					break;
			}

			if(animation != -1) {
				for (ShortPoint2D point : effectLocations) {
					getGrid().addSelfDeletingMapObject(point, sound, animation, duration, movable.getPlayer());
				}
			}

		} else {
			movable.getPlayer().showMessage(SimpleMessage.castFailed(spellLocation, "spell_failed"));
		}

		playAction(EMovableAction.ACTION1, 1);
		spellLocation = null;
		spell = null;
	}

	private void convertLandscape(int radius, Function<ELandscapeType, Boolean> fromCond, ELandscapeType to, Function<ELandscapeType, Boolean> fromFlatCond, ELandscapeType toFlat) {
		List<ShortPoint2D> flattenedRegion = null;

		CoordinateStream affectedRegion = spellRegion(radius)
				.filter((x, y) -> fromCond.apply(getGrid().getLandscapeTypeAt(x, y)));

		if(fromFlatCond != null) {
			flattenedRegion = affectedRegion.filter((x, y) -> fromFlatCond.apply(getGrid().getLandscapeTypeAt(x, y))).toList();
		}

		terraformHandle.fill(to, affectedRegion.toList());

		if(fromFlatCond != null) {
			terraformHandle.fill(toFlat, flattenedRegion);
		}
	}

	private void convertMaterial(EMaterialType from, EMaterialType to, int limit, List<ShortPoint2D> effects) {
		final MutableInt materialCount = new MutableInt(0);
		AbstractMovableGrid grid = getGrid();
		sort(spellRegion()).forEach((x, y) -> {
			ShortPoint2D pos = new ShortPoint2D(x, y);
			boolean tookSomething = false;

			while(grid.takeMaterial(pos, from) && materialCount.value <= limit) {
				materialCount.value++;
				tookSomething = true;
			}
			if(tookSomething) effects.add(pos);
		});

		sort(spellRegion()).forEach((x, y) -> {
			ShortPoint2D pos = new ShortPoint2D(x, y);

			while(materialCount.value > 0 && grid.dropMaterial(pos, to, true, false)) {
				materialCount.value--;
			}
		});
	}

	private boolean willCastSpell() {
		return spell != null && (!spell.forcePresence() || movable.getPosition().getOnGridDistTo(spellLocation) <= Constants.MAGE_CAST_DISTANCE);
	}

	@Override
	protected boolean checkPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		return !willCastSpell();
	}

	@Override
	protected boolean canBeControlledByPlayer() {
		return true;
	}

	@Override
	protected void stopOrStartWorking(boolean stop) {
		if(stop) {
			movable.moveTo(movable.getPosition(), EMoveToType.FORCED);
		}
	}

	public void castSpellAt(ESpellType spell, ShortPoint2D at) {
		this.spell = spell;
		this.spellLocation = at;
	}
}
