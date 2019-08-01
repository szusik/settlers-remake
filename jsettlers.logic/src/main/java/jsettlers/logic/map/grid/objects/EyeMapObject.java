package jsettlers.logic.map.grid.objects;

import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.objects.SoundableSelfDeletingObject;

public class EyeMapObject extends SoundableSelfDeletingObject {

	private IMapObjectsManagerGrid grid;
	private boolean drawn = false;
	private ShortPoint2D at;
	private short distance;

	public EyeMapObject(IMapObjectsManagerGrid grid, ShortPoint2D pos, float duration, short distance, IPlayer player) {
		super(pos, EMapObjectType.EYE, duration, player);
		this.distance = distance;
		this.grid = grid;
		this.at = pos;
	}

	@Override
	protected void changeState() {
		if(!drawn) {
			grid.drawFogOfWar(at, (short)0, distance);
			drawn = true;
		} else {
			grid.drawFogOfWar(at, distance, (short)0);
		}
	}
}
