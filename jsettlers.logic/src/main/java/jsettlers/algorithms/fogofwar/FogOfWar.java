/*******************************************************************************
 * Copyright (c) 2015 - 2017
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
package jsettlers.algorithms.fogofwar;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import jsettlers.common.CommonConstants;
import jsettlers.common.map.IGraphicsBackgroundListener;
import jsettlers.common.position.ShortPoint2D;
import go.graphics.FramerateComputer;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.MainGrid;
import jsettlers.logic.movable.MovableManager;

/**
 * This class holds the fog of war for a given map and team.
 * 
 * @author Andreas Eberle
 */
public final class FogOfWar implements Serializable {
	private static final long serialVersionUID = 1877994785778678510L;
	/**
	 * Longest distance any unit may look
	 */
	public static final byte MAX_VIEW_DISTANCE = 65;
	public static final int PADDING = 10;

	public final byte team;

	public final short width;
	public final short height;
	public byte[][] sight;
	public short[][][] visibleRefs;
	public transient FowDimThread dimThread;
	public transient FoWRefThread refThread;

	public transient CircleDrawer circleDrawer;
	private transient IGraphicsBackgroundListener backgroundListener;
	public transient boolean enabled;
	public transient boolean canceled;

	public FogOfWar(short width, short height, byte teamId) {
		this.width = width;
		this.height = height;
		this.team = teamId;
		this.sight = new byte[width][height];
		this.visibleRefs = new short[width][height][0];

		readObject(null);
	}

	public void start() {
		instance = this;
		refThread.start();
		dimThread.start();
	}

	public static void queueResizeCircle(ShortPoint2D at, short from, short to) {
		BuildingFoWTask foWTask = new BuildingFoWTask();
		foWTask.from = from;
		foWTask.to = to;
		foWTask.at = at;
		instance.refThread.nextTasks.add(foWTask);
	}

	public static FogOfWar instance;

	public void setBackgroundListener(IGraphicsBackgroundListener backgroundListener) {
		if (backgroundListener != null) {
			this.backgroundListener = backgroundListener;
		} else {
			this.backgroundListener = new MainGrid.NullBackgroundListener();
		}
	}

	private void readObject(ObjectInputStream ois) {
		if(ois != null) {
			try {
				ois.defaultReadObject();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		refThread = new FoWRefThread();
		dimThread = new FowDimThread();
		circleDrawer = new CircleDrawer();
		enabled = Constants.FOG_OF_WAR_DEFAULT_ENABLED;
		canceled = false;
		backgroundListener = new MainGrid.NullBackgroundListener();
	}

	public static class BuildingFoWTask implements FoWTask {
		ShortPoint2D at;
		short from;
		short to;
	}

	public static class ShowHideFoWTask implements FoWTask {
		boolean addRef;
	}

	public static class WaitFoWTask implements FoWTask {

	}

	/**
	 * Gets the visible status of a map pint
	 *
	 * @param x
	 *            The x coordinate of the point in 0..(mapWidth - 1)
	 * @param y
	 *            The y coordinate of the point in 0..(mapHeight - 1)
	 * @return The status from 0 to visible.
	 */
	public final byte getVisibleStatus(int x, int y) {
		return enabled ? sight[x][y] : CommonConstants.FOG_OF_WAR_VISIBLE;
	}

	public byte[][] getVisibleStatusArray() {
		return sight;
	}

	public final void toggleEnabled() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean enabled) {
		if(this.enabled == enabled) return;

		this.enabled = enabled;

		backgroundListener.fogOfWarEnabledStatusChanged(enabled);
		for(int y = 0; y != height;y++) backgroundListener.backgroundColorLineChangedAt(0, y, width);
	}

	public void showMap() {
		ShowHideFoWTask foWTask = new ShowHideFoWTask();
		foWTask.addRef = true;
		instance.refThread.nextTasks.add(foWTask);
	}

	public void hideMap() {
		ShowHideFoWTask foWTask = new ShowHideFoWTask();
		foWTask.addRef = false;
		instance.refThread.nextTasks.add(foWTask);
	}

	public static final int CIRCLE_REMOVE = 1;
	public static final int CIRCLE_ADD = 2;
	public static final int CIRCLE_DIM = 8;

	public class FoWRefThread extends FoWThread {
		public final ConcurrentLinkedQueue<FoWTask> nextTasks = new ConcurrentLinkedQueue<>();

		FoWRefThread() {
			super("FOW-reference-updater");
			framerate = CommonConstants.FOG_OF_WAR_REF_UPDATE_FRAMERATE;
			nextTasks.add(new WaitFoWTask());
		}

		@Override
		public void init() {
			MovableManager.initFow(team);
			Building.initFow(team);
		}

		@Override
		public void taskProcessor() {
			if (enabled) {
				while(true) {
					FoWTask task = nextTasks.poll();
					if(task == null) return;

					if(!runTask(task)) {
						nextTasks.add(task);
					}

					if(task instanceof WaitFoWTask) return;
				}
			}
		}

		boolean runTask(FoWTask task) {
			if(task instanceof BuildingFoWTask) {
				BuildingFoWTask bFOW = (BuildingFoWTask) task;
				if (bFOW.to > 0) circleDrawer.drawCircleToBuffer(bFOW.at, bFOW.to, CIRCLE_ADD);
				if (bFOW.from > 0) circleDrawer.drawCircleToBuffer(bFOW.at, bFOW.from, CIRCLE_REMOVE);
				circleDrawer.drawCircleToBuffer(bFOW.at, bFOW.to>bFOW.from ? bFOW.to : bFOW.from, CIRCLE_DIM);
				return true;
			} else if(task instanceof ShowHideFoWTask) {
				ShowHideFoWTask shFOW = (ShowHideFoWTask) task;
				circleDrawer.draw(new ShowHideMapIterator(), shFOW.addRef?CIRCLE_ADD|CIRCLE_DIM : CIRCLE_REMOVE|CIRCLE_DIM);
				return true;
			} else if(task instanceof MovableFoWTask) {
				MovableFoWTask mFOW = (MovableFoWTask) task;
				ShortPoint2D currentPos = mFOW.getFoWPosition();
				ShortPoint2D oldPos = mFOW.getOldFoWPosition();

				int vd = mFOW.getViewDistance();
				if(!Objects.equals(oldPos, currentPos)) {
					if(currentPos != null) circleDrawer.drawCircleToBuffer(currentPos, vd, CIRCLE_ADD|CIRCLE_DIM);
					if(oldPos != null) circleDrawer.drawCircleToBuffer(oldPos, vd, CIRCLE_REMOVE|CIRCLE_DIM);
					mFOW.setOldFoWPosition(currentPos);
				}
				return !mFOW.continueFoW();
			} else if(task instanceof WaitFoWTask) {
				return false;
			} else {
				System.err.println("unknown FoWTask: " + task);
				return false;
			}
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public class FowDimThread extends FoWThread {
		FowDimThread() {
			super("FOW-dimmer");
			size = width*height;
			nextUpdate = new BitSet(size);
			update = new BitSet(size);
		}

		public final BitSet nextUpdate;
		public final BitSet update;
		private final int size;

		@Override
		public void taskProcessor() {
			synchronized (nextUpdate) {
				update.or(nextUpdate);
				nextUpdate.clear();
			}

			double sync_factor = fc.getTime();
			if (sync_factor == 0) sync_factor = 1.0 / CommonConstants.FOG_OF_WAR_DIM_FRAMERATE;
			byte dim = (byte) Math.round(sync_factor * CommonConstants.FOG_OF_WAR_DIM * MatchConstants.clock().getGameSpeed());


			int last = 0;
			do {
				int first = update.nextSetBit(last);
				if (first == -1) break;

				last = update.nextClearBit(first);
				if (last == -1) last = size;

				int beginX = first % width;
				int beginY = (first - beginX) / width;

				int endX = last % width;
				int endY = (last - endX) / width;

				for (int y = beginY; y <= endY; y++) {
					int firstUpdate = -1;
					int lastUpdate = -1;

					int x = y == beginY ? beginX : 0;
					int x2 = y == endY ? endX : width;
					for(; x < x2; x++) {
						byte dimTo = dimmedSight(x, y);

						if(dimTo != sight[x][y]) {
							if(lastUpdate + 1 != x) {
								if(firstUpdate != -1) update(y, firstUpdate, lastUpdate);
								firstUpdate = lastUpdate = x;
							} else {
								if(firstUpdate == -1) firstUpdate = x;
								lastUpdate = x;
							}

							sight[x][y] = dim(sight[x][y], dimTo, dim);

							if(sight[x][y] == dimTo) update.clear(y * width + x);
						} else {
							update.set(y * width + x, false);
						}
					}
					if (firstUpdate != -1) {
						update(y, firstUpdate, lastUpdate);
					}
				}
			} while (last < size);
			framerate = (int) (CommonConstants.FOG_OF_WAR_DIM_FRAMERATE*MatchConstants.clock().getGameSpeed());
			if(framerate > CommonConstants.FOG_OF_WAR_DIM_MAX_FRAMERATE) framerate = CommonConstants.FOG_OF_WAR_DIM_MAX_FRAMERATE;
		}

		private void update(int y, int from, int to) {
			backgroundListener.backgroundColorLineChangedAt(from, y, to-from);
		}
	}


	private static byte dim(byte value, byte dimTo, byte dim) {
		if(value >= CommonConstants.FOG_OF_WAR_EXPLORED && dimTo < CommonConstants.FOG_OF_WAR_EXPLORED) dimTo = CommonConstants.FOG_OF_WAR_EXPLORED;
		if(value < CommonConstants.FOG_OF_WAR_EXPLORED && dimTo < value) return value;

		byte dV = (byte) (value-dimTo);
		if(dV < 0) dV = (byte) -dV;

		if(dV < dim) return dimTo;
		if(value < dimTo) return (byte) (value+dim);
		else return (byte) (value-dim);
	}

	final byte dimmedSight(int x, int y) {
		short[] refs = instance.visibleRefs[x][y];
		if(refs.length == 0) return 0;

		byte value = CommonConstants.FOG_OF_WAR_VISIBLE;

		for(int i = 0;i != refs.length;i++) {
			if(refs[i] > 0) break;
			value -= 10;
		}

		return value;
	}

	public abstract class FoWThread extends Thread {
		public int framerate;

		final FramerateComputer fc = new FramerateComputer();

		FoWThread(String name) {
			super(name);
		}

		@Override
		public final void run() {
			init();

			while (!canceled) {
				try {
					taskProcessor();
				} catch(Throwable ex) {
					ex.printStackTrace();
				}
				fc.nextFrame(framerate);
			}
		}

		public void init() {}

		public abstract void taskProcessor();

		public long start;
	}

	public void cancel() {
		canceled = true;
	}

	public int maxIndex(int x, int y) {
		short[] array = instance.visibleRefs[x][y];
		int lastEntry = array.length-1;

		if(array.length == 0) return 0;

		for(int i = lastEntry;i >= 0;i--) {
			if(array[i] > 0) return i+1;
		}
		return 0;
	}

	public interface ViewAreaIterator {
		boolean hasNext();
		int getCurrX();
		int getCurrY();

		byte getRefIndex();
	}

	public final class ShowHideMapIterator implements ViewAreaIterator {
		private int x = 0;
		private int y = 0;

		public ShowHideMapIterator() {
		}

		@Override
		public boolean hasNext() {
			x++;

			if(x == width) {
				x = 0;
				y++;
			}
			return y != height;
		}

		@Override
		public int getCurrX() {
			return x;
		}

		@Override
		public int getCurrY() {
			return y;
		}

		@Override
		public byte getRefIndex() {
			return 0;
		}
	}

	final class CircleDrawer {

		public final CachedViewCircle[] cachedCircles = new CachedViewCircle[MAX_VIEW_DISTANCE];

		/**
		 * Draws a circle to the buffer line. Each point is only brightened and onlydrawn if its x coordinate is in [0, mapWidth - 1] and its computed y coordinate is bigger than 0.
		 */
		final void drawCircleToBuffer(ShortPoint2D at, int viewDistance, int state) {
			CachedViewCircle circle = getCachedCircle(viewDistance);
			CachedViewCircle.CachedViewCircleIterator iterator = circle.iterator(at.x, at.y);
			draw(iterator, state);
		}

		final void draw(ViewAreaIterator iterator, int state) {
			while (iterator.hasNext()) {
				final int x = iterator.getCurrX();
				final int y = iterator.getCurrY();

				if (x >= 0 && x < width && y > 0 && y < height) {
					byte tmpIndex = iterator.getRefIndex();

					if((state&CIRCLE_ADD) > 0) {
						if(instance.visibleRefs[x][y].length <= tmpIndex) { // enlarge ref index array
							short[] tmpRef = instance.visibleRefs[x][y];
							instance.visibleRefs[x][y] = new short[tmpIndex+1];
							System.arraycopy(tmpRef, 0, instance.visibleRefs[x][y], 0, tmpRef.length);
						}

						instance.visibleRefs[x][y][tmpIndex]++;
					}
					if((state&CIRCLE_REMOVE) > 0) {
						instance.visibleRefs[x][y][tmpIndex]--;
						if(instance.visibleRefs[x][y][tmpIndex] == 0 && instance.visibleRefs[x][y].length == tmpIndex+1) { // minimize ref index array size
							int newLength = maxIndex(x, y);

							short[] tmpRef = instance.visibleRefs[x][y];
							instance.visibleRefs[x][y] = new short[newLength];
							System.arraycopy(tmpRef, 0, instance.visibleRefs[x][y], 0, newLength);
						}
					}

					if((state&CIRCLE_DIM) > 0 && sight[x][y] != dimmedSight(x, y)) {
						synchronized (instance.dimThread.nextUpdate) {
							instance.dimThread.nextUpdate.set(y*width+x);
						}
					}
				}
			}
		}

		public CachedViewCircle getCachedCircle(int viewDistance) {
			int radius = Math.min(viewDistance, MAX_VIEW_DISTANCE - 1);
			if (cachedCircles[radius] == null) {
				cachedCircles[radius] = new CachedViewCircle(radius);
			}

			return cachedCircles[radius];
		}
	}
}
