package go.graphics.android;

import android.media.MediaMetadataRetriever;
import android.media.SoundPool;

import java.io.File;

import go.graphics.sound.SoundHandle;

public class AndroidSoundHandle implements SoundHandle {

	private final SoundPool pool;
	private final int audioId;
	private int streamId = -1;
	private float volume;
	private int length;

	public AndroidSoundHandle(SoundPool pool, File source) {
		this.pool = pool;

		MediaMetadataRetriever mdr  = new MediaMetadataRetriever();
		mdr.setDataSource(source.getPath());
		String lengthString = mdr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		mdr.release();

		// something went wrong
		if(lengthString == null) {
			length = -1;
			audioId = -1;
			return;
		}

		length = Integer.parseInt(lengthString);

		audioId = pool.load(source.getPath(), 0);
	}

	@Override
	public void start() {
		if(streamId != -1) {
			pool.resume(streamId);
		} else {
			streamId = pool.play(audioId, volume, volume, 0, 1, 1);
		}
	}

	@Override
	public void pause() {
		pool.pause(streamId);
	}

	@Override
	public void stop() {
		pool.stop(streamId);
		streamId = -1;
	}

	@Override
	public void dismiss() {
		pool.unload(audioId);
	}

	@Override
	public void setVolume(float volume) {
		this.volume = volume;
		if(streamId != -1) pool.setVolume(streamId, volume, volume);
	}

	@Override
	public int getPlaybackDuration() {
		return length;
	}
}
