package jsettlers.logic.movable.strategies.military;

import java.util.List;

import jsettlers.common.map.shapes.MapCircle;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.menu.messages.EMessageType;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESpellType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.utils.coordinates.ICoordinateConsumer;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableStrategy;

public class MageStrategy extends MovableStrategy {
	public MageStrategy(Movable movable) {
		super(movable);
	}


	private boolean spellAbortPath = false;
	private ShortPoint2D spellLocation = null;
	private ESpellType spell = null;

	@Override
	protected boolean checkPathStepPreconditions(ShortPoint2D pathTarget, int step) {
		if(spellAbortPath && !pathTarget.equals(spellLocation)) {
			abortCasting();
		}

		if(spellLocation != null && spellLocation.getOnGridDistTo(movable.getPosition()) <= Constants.MAGE_CAST_DISTANCE) {

			if(movable.getPlayer().getMannaInformation().useSpell(spell)) {
				switch(spell) {
					case GILDING:
						int remainingTake = ESpellType.GILDING_MAX_IRON;
						int remainingPlace = 0;

						List<ShortPoint2D> possibleLocations = new MapCircle(spellLocation, Constants.SPELL_EFFECT_RADIUS).stream()
								.filterBounds(getGrid().getWidth(), getGrid().getHeight())
								.filter((x, y) -> !getGrid().isBlocked(x, y)).toList();

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

						//TODO play sound 95
						if(remainingPlace > 0) System.err.println("Couldn`t place " + remainingPlace + "gold");
						break;
					default:
						System.err.println("unimplemented spell: " + spell);
				}
			} else {
				movable.getPlayer().showMessage(SimpleMessage.castFailed(spellLocation, "spell_failed"));
			}

			boolean abortPath = spellAbortPath;
			abortCasting();

			return !abortPath;
		}
		return true;
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
