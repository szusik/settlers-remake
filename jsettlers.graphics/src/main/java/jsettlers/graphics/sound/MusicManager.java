package jsettlers.graphics.sound;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import java8.util.Lists;
import java8.util.Lists2;
import jsettlers.common.player.ECivilisation;
import go.graphics.sound.SoundPlayer;

/**
 *
 * music management of different s3 versions, provides CIVILISATION specific music management
 *
 * TODO: PC only so far, add ingame gui config, validate music files and integrate exception handling
 *
 * @author MarviMarv
 */
public final class MusicManager implements Runnable {

	private final static String[][] ULTIMATE_EDITION_MUSIC_SET = { { "02", "03", "12" }, { "06", "07", "14" }, { "04", "05", "13" }, { "08", "09", "10" }, { "11" } };
	private final static String[][] HISTORY_EDITION_MUSIC_SET = { { "02", "03", "04" }, { "05", "06", "07" }, { "08", "09", "10" }, { "13", "14", "15" }, { "11", "12" } };

	private static File lookupPath = null;

	public static void setLookupPath(final File lookupPath) {
		MusicManager.lookupPath = lookupPath;
	}

	private Thread musicThread = null;
	private final SoundPlayer soundPlayer;
	private final ECivilisation civilisation;
	private boolean paused = true;

	public MusicManager(SoundPlayer soundPlayer, ECivilisation civilisation) {
		this.soundPlayer = soundPlayer;
		this.civilisation = civilisation;
	}

	public boolean isRunning() {
		return !paused;
	}

	public void startMusic() {
		if(!isRunning()) {
			paused = false;
			musicThread = new Thread(this);
			musicThread.setName("MusicThread");
			musicThread.setDaemon(true);
			musicThread.start();
		}
	}

	public void stopMusic() {
		if (isRunning()) {
			paused = true;
			soundPlayer.stopMusic();
		}
	}

	public void setMusicVolume(float volume, boolean relative) {
		if (MusicManager.lookupPath != null && isRunning()) {
			soundPlayer.setMusicVolume(volume, relative);
		}
	}

	private List<File> assembleMusicSet(final ECivilisation civilisation, final boolean playAll) {
		// there are no music files available
		if(lookupPath == null || !lookupPath.exists()) return Lists2.of();


		List<File> list = new ArrayList<>();
		// just take all mp3 and ogg files
		if(playAll) {
			list.addAll(Arrays.asList(lookupPath.listFiles((file, name) -> name.endsWith(".mp3") || name.endsWith(".ogg"))));
		} else if(lookupPath.getName().equals("Theme")) { // history edition
			for(String fileName : HISTORY_EDITION_MUSIC_SET[civilisation.ordinal]) {
				File file = new File(lookupPath, "Track" + fileName + ".mp3");
				if(file.exists()) list.add(file);
			}
		} else {
			// ultimate edition
			for(String fileName : ULTIMATE_EDITION_MUSIC_SET[civilisation.ordinal]) {
				File file = new File(lookupPath, "Track" + fileName + ".ogg");
				if(file.exists()) list.add(file);
			}
		}

		// this music folder is custom made
		if(list.isEmpty()) {
			list.addAll(Arrays.asList(lookupPath.listFiles((file, name) -> name.matches(civilisation.name() + ".\\d*.(mp3|ogg)"))));
		}

		// shuffle list so we don't get bored :)
		Collections.shuffle(list);

		return list;
	}

	@Override
	public void run() {
		int trackIndex = 0;
		List<File> musicSet = this.assembleMusicSet(civilisation, soundPlayer.isMusicPlayAll());

		while (!paused) {
			this.soundPlayer.playMusic(musicSet.get(trackIndex));
			trackIndex++;

			if (trackIndex == musicSet.size()) {
				trackIndex = 0;
			}
		}
	}
}
