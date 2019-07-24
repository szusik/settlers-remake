package jsettlers.logic.movable.strategies.military;

import java.util.List;

import jsettlers.common.map.shapes.MapCircle;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.ESpellType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.utils.coordinates.CoordinateStream;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableStrategy;

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

	@Override
	protected boolean checkPathStepPreconditions(ShortPoint2D pathTarget, int step) {
		if(spellAbortPath && !pathTarget.equals(spellLocation)) {
			abortCasting();
		}

		if(spellLocation == null || spellLocation.getOnGridDistTo(movable.getPosition()) > Constants.MAGE_CAST_DISTANCE) return true;

		if(movable.getPlayer().getMannaInformation().useSpell(spell)) {
			switch(spell) {
				case GILDING:
					List<ShortPoint2D> possibleLocations = spellRegion(Constants.SPELL_EFFECT_RADIUS).filter((x, y) -> !getGrid().isBlocked(x, y)).toList();
					int remainingTake = ESpellType.GILDING_MAX_IRON;
					int remainingPlace = 0;

					for(ShortPoint2D point : possibleLocations) {
						while(remainingTake > 0 && getGrid().canTakeMaterial(point, EMaterialType.IRON)) {
							getGrid().takeMaterial(point, EMaterialType.IRON);
							remainingTake--;
							remainingPlace++;
						}
					}

					for(ShortPoint2D point : possibleLocations) {
						while(remainingPlace > 0 && getGrid().dropMaterial(point, EMaterialType.GOLD, true, false)) {
							remainingPlace--;
						}
					}

					//TODO play sound 95 and play animation 1:121
					if(remainingPlace > 0) System.err.println("Couldn`t place " + remainingPlace + "gold");
					break;
				case DEFEATISM:
					spellRegion(Constants.SPELL_EFFECT_RADIUS).map((x, y) -> getGrid().getMovableAt(x, y))
							.filter(lm -> lm!=null&&lm.isAlive()&&lm.isAttackable())
							.filter(lm -> lm.getPlayer().getTeamId()!=movable.getPlayer().getTeamId())
							.limit(ESpellType.DEFEATISM_MAX_SOLDIERS)
							.forEach(movable -> movable.addEffect(EEffectType.DEFEATISM));

					//TODO play sound and play animation 1:119
					break;
				case GIFTS:
					spellRegion(ESpellType.GIFTS_RADIUS).filter((x, y) -> !getGrid().isBlockedOrProtected(x, y))
							.filter((x, y) -> getGrid().getPlayerAt(new ShortPoint2D(x, y)) == movable.getPlayer())
							.limit(MatchConstants.random().nextInt(0, ESpellType.GIFTS_MAX_STACKS))
							.forEach((x, y) -> {
								ShortPoint2D at = new ShortPoint2D(x, y);
								//TODO only give useful stuff
								EMaterialType type = EMaterialType.values()[MatchConstants.random().nextInt(EMaterialType.values().length)];
								int size = MatchConstants.random().nextInt(9);
								for(int i = 0; i != size; i++) getGrid().dropMaterial(at, type, true, false);
							});
					//TODO play sound and play animation 1:114
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
