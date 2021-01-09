package jsettlers.common.music;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;

/**
 *
 * supports mp3, gradle takes care of dependencies TODO add volume and generic audio stream
 *
 * @author MarviMarv
 */

public class MusicThread implements Runnable {

	/**
	 * The lookup path for the music files.
	 */
	private static File lookupPath;

	private AdvancedPlayer player;
	private final Thread musicThread;

	private final String musicFilePath;
	private final String musicFileType;
	private final String[] musicTracks;

	private boolean canceled;

	public MusicThread(final String musicFilePath, final String musicFileType, final String[] musicTracks) {
		canceled = false;
		player = null;

		this.musicFilePath = musicFilePath;
		this.musicFileType = musicFileType;
		this.musicTracks = musicTracks;

		musicThread = new Thread(this);
		musicThread.setName("MusicThread");
		musicThread.setDaemon(true);
	}

	@Override
	public void run() {
		int trackIndex = 0;

		while (!canceled) {

			try {
				if (musicFileType == "mp3") {
					player = new AdvancedPlayer(new FileInputStream(musicFilePath + musicTracks[trackIndex] + "." + musicFileType));
					player.play();
				}

				trackIndex++;
			} catch (JavaLayerException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			if (trackIndex == musicTracks.length) {
				trackIndex = 0;
			}
		}
	}

	public void start() {
		musicThread.start();
	}

	public void cancel() {
		canceled = true;

		if (player != null) {
			player.close();
			player = null;
		}

		musicThread.interrupt();
	}
}