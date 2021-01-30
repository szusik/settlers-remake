package go.graphics.swing.sound;

import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import go.graphics.sound.SoundHandle;

public class SwingSoundHandle implements SoundHandle {

	private final Clip clip;

	public SwingSoundHandle(final Clip clip) {
		this.clip = clip;
	}

	private boolean paused = false;

	@Override
	public void start() {
		if(!paused) {
			clip.setFramePosition(0);
			paused = false;
		}
		clip.start();
	}

	@Override
	public void pause() {
		clip.stop();
		paused = true;
	}

	@Override
	public void stop() {
		clip.stop();
	}

	@Override
	public void dismiss() {
		clip.close();
	}

	@Override
	public void setVolume(float volume) {
		FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		control.setValue(volume);
	}

	@Override
	public int getPlaybackDuration() {
		return (int) (clip.getMicrosecondLength()/1000);
	}
}
