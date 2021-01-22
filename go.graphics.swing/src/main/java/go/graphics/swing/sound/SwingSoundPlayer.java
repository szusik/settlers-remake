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
package go.graphics.swing.sound;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import go.graphics.sound.ForgettingQueue;
import go.graphics.sound.ForgettingQueue.Sound;
import go.graphics.sound.ISoundDataRetriever;
import go.graphics.sound.SoundPlayer;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static javax.sound.sampled.AudioSystem.getAudioInputStream;

public class SwingSoundPlayer implements SoundPlayer {
	private static final int BUFFER_SIZE = 4048 * 4;
	private static final int SOUND_THREADS = 30;

	private final ISoundSettingsProvider soundSettingsProvider;
	private ForgettingQueue<Integer> queue = new ForgettingQueue<>();
	private ISoundDataRetriever soundDataRetriever;
	private SourceDataLine musicLine;
	private float musicVolume;

	public SwingSoundPlayer(ISoundSettingsProvider soundSettingsProvider) {
		this.soundSettingsProvider = soundSettingsProvider;
		ThreadGroup soundGroup = new ThreadGroup("soundplayer");
		for (int i = 0; i < SOUND_THREADS; i++) {
			new Thread(soundGroup, new SoundPlayerTask(), "soundplayer" + i).start();
		}

		this.musicLine = null;
		this.musicVolume = soundSettingsProvider.getMusicVolume();
	}

	@Override
	public void playSound(int soundStart, float leftVolume, float rightVolume) {
		float gameVolume = soundSettingsProvider.getVolume();
		leftVolume *= gameVolume;
		rightVolume *= gameVolume;

		if (leftVolume > 0 || rightVolume > 0) {
			queue.offer(soundStart, leftVolume, rightVolume);
		}
	}

	private byte[] transformData(short[] data) {
		byte[] buffer = new byte[data.length * 4];
		for (int i = 0; i < data.length; i++) {
			buffer[4 * i] = buffer[4 * i + 2] = (byte) data[i];
			buffer[4 * i + 1] = buffer[4 * i + 3] = (byte) (data[i] >> 8);
		}

		return buffer;
	}

	private byte[] transformData(short[] data, float l, float r) {
		byte[] buffer = new byte[data.length * 4];
		for (int i = 0; i < data.length; i++) {
			int ld = (int) (data[i] * l);
			buffer[4 * i] = (byte) ld;
			buffer[4 * i + 1] = (byte) (ld >> 8);
			int rd = (int) (data[i] * r);
			buffer[4 * i + 2] = (byte) rd;
			buffer[4 * i + 3] = (byte) (rd >> 8);
		}

		return buffer;
	}

	private class SoundPlayerTask implements Runnable {

		@Override
		public void run() {
			AudioFormat format = new AudioFormat(22050, 16, 2, true, false);

			Line.Info info = new Line.Info(SourceDataLine.class);

			try {
				SourceDataLine dataLine = (SourceDataLine) AudioSystem
						.getMixer(null).getLine(info);
				dataLine.open(format, BUFFER_SIZE);

				while (true) {
					try {
						// start sound playing
						dataLine.start();

						Sound<Integer> sound = queue.take();

						byte[] buffer;
						if (dataLine.isControlSupported(FloatControl.Type.VOLUME) && dataLine.isControlSupported(FloatControl.Type.BALANCE)) {
							buffer = transformData(soundDataRetriever.getSoundData(sound.getData()));
							FloatControl volumeControl = (FloatControl) dataLine.getControl(FloatControl.Type.VOLUME);
							volumeControl.setValue(sound.getVolume() * volumeControl.getMaximum());
							((FloatControl) dataLine.getControl(FloatControl.Type.BALANCE)).setValue(sound.getBalance());
						} else {
							buffer = transformData(soundDataRetriever.getSoundData(sound.getData()), sound.getLvolume(), sound.getRvolume());
						}

						dataLine.write(buffer, 0, buffer.length);

						// stop playing
						dataLine.drain();
						dataLine.stop();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			} catch (InterruptedException e) {
				// exit
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setSoundDataRetriever(ISoundDataRetriever soundDataRetriever) {
		this.soundDataRetriever = soundDataRetriever;
	}

	/**
	 * Music
	 *
	 */
	public void playMusic(final File musicFile) {
		try (final AudioInputStream in = getAudioInputStream(musicFile)) {

			final AudioFormat outFormat = getMusicOutFormat(in.getFormat());
			final DataLine.Info info = new DataLine.Info(SourceDataLine.class, outFormat);

			try (final SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {

				if (line != null) {
					this.musicLine = line;

					line.open(outFormat);

					if (this.musicVolume == 0f && soundSettingsProvider.getMusicVolume() != 0f) {
						this.musicVolume = 0.3f;
					}
					setMusicVolume(this.musicVolume, false);

					line.start();
					streamMusic(getAudioInputStream(outFormat, in), line);
					line.drain();
					line.stop();
				}
			}
		} catch (UnsupportedAudioFileException
				| LineUnavailableException
				| IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private AudioFormat getMusicOutFormat(final AudioFormat inFormat) {
		final int ch = inFormat.getChannels();
		final float rate = inFormat.getSampleRate();
		return new AudioFormat(PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
	}

	private void streamMusic(final AudioInputStream in, final SourceDataLine line)
			throws IOException {
		final byte[] buffer = new byte[65536];
		for (int n = 0; n != -1; n = in.read(buffer, 0, buffer.length)) {
			line.write(buffer, 0, n);
		}
	}

	public void stopMusic() {
		if (this.musicLine != null) {
			musicLine.stop();
			musicLine.close();
		}
	}

	public void setMusicVolume(float volume, boolean relative) {
		if (this.musicLine != null) {
			try {
				FloatControl gainControl = (FloatControl) this.musicLine.getControl(FloatControl.Type.MASTER_GAIN);
				BooleanControl muteControl = (BooleanControl) this.musicLine.getControl(BooleanControl.Type.MUTE);

				if (relative) {
					this.musicVolume += volume;
				} else {
					this.musicVolume = volume;
				}

				if (this.musicVolume <= 0f) {
					muteControl.setValue(true);
					this.musicVolume = 0f;
				} else {
					if (this.musicVolume > 1f) {
						this.musicVolume = 1f;
					}
					muteControl.setValue(false);
					gainControl.setValue((float) (Math.log(this.musicVolume) / Math.log(10.0) * 20.0));
				}
			} catch (Exception ex) {
				System.out.println("unable to set the volume to the provided source");
			}
		}
	}

	public boolean isMusicPlayAll() {
		return soundSettingsProvider.isMusicPlayAll();
	}
}
