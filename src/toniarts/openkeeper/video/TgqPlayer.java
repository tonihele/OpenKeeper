/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.video;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import toniarts.openkeeper.video.tgq.EAAudioFrame;
import toniarts.openkeeper.video.tgq.EAAudioHeader;
import toniarts.openkeeper.video.tgq.TgqFile;
import toniarts.openkeeper.video.tgq.TgqFrame;

/**
 * "Media player" for TGQ files<br>
 * Kinda like an interface between the actual canvas and the decoder<br>
 * Not rewindable etc.<br>
 * Very simplistic, the buffer is just filled up once. If the decoder is too
 * slow, the playback will start to stutter. But with a modern computer, it
 * really shouldn't be that slow. Video is synced to audio.<br>
 * Example is taken from JCodec project
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class TgqPlayer {

    private static final Logger logger = System.getLogger(TgqPlayer.class.getName());

    private final Path file;
    private static final int FPS = 25; // The specs say 15 FPS, but with this they are totally in sync, dunno why
    private static final int FRAME_INTERVAL = (int) Math.floor(1000 / FPS); // In milliseconds
    private static final float FRAME_BUFFER_SIZE = 3; // In seconds, there is no fancy counter etc.
    private static final int MAX_FRAME_COUNT_IN_BUFFER = (int) (FPS * FRAME_BUFFER_SIZE);
    private final Queue<EAAudioFrame> audioFrames = new ConcurrentLinkedQueue<>();
    private final Queue<TgqFrame> videoFrames = new ConcurrentLinkedQueue<>();
    private volatile EAAudioHeader audioHeader;
    private Thread decoderThread;
    private Thread videoPlaybackThread;
    private Thread audioPlaybackThread;
    private long clock;
    private volatile boolean bufferingComplete;
    private SourceDataLine line;
    private final Object bufferedEvent = new Object();
    private final Object audioHeaderEvent = new Object();

    /**
     * We need to wait the audio to be cleaned up properly or otherwise other
     * things may fail to init it after us
     */
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private boolean stopped = true;

    public TgqPlayer(Path file) {
        this.file = file;
    }

    /**
     * Start playing the movie
     */
    public void play() {

        // Init the variables
        stopped = false;
        bufferingComplete = false;
        clock = 0;
        line = null;
        audioFrames.clear();
        videoFrames.clear();

        // Create and start the decoder thread
        TgqDecoder decoder = new TgqDecoder(file);
        decoderThread = new Thread(decoder, "TgqDecoder");
        decoderThread.start();

        // The audio player
        AudioPlayer audioPlayer = new AudioPlayer();
        audioPlaybackThread = new Thread(audioPlayer, "TgqAudioPlayback");
        audioPlaybackThread.start();

        // The video player
        VideoPlayer videoPlayer = new VideoPlayer();
        videoPlaybackThread = new Thread(videoPlayer, "TgqVideoPlayback");
        videoPlaybackThread.start();
    }

    /**
     * Stop playing the movie
     */
    public synchronized void stop() {

        try {

            // Kill the decoder
            if (decoderThread != null && decoderThread.isAlive()) {
                decoderThread.interrupt();
            }

            // Kill the video player as well
            if (videoPlaybackThread != null && videoPlaybackThread.isAlive()) {
                videoPlaybackThread.interrupt();
            }

            // Kill the audio player as well
            if (audioPlaybackThread != null && audioPlaybackThread.isAlive()) {
                audioPlaybackThread.interrupt();
            }

            // Wait for the threads to die down
            if (countDownLatch.getCount() != 0) {
                try {
                    countDownLatch.await();
                } catch (InterruptedException ex) {
                    logger.log(Level.WARNING, "Interrupted while waiting for count down latch", ex);
                }
            }
        } finally {

            // And finally notify, just call this once
            if (!stopped) {
                stopped = true;
                onPlayingEnd();
            }
        }
    }

    /**
     * Callback for playing ended, it has either ended on its own, stopped or
     * crashed
     */
    protected abstract void onPlayingEnd();

    /**
     * The movie has advanced to next frame, it really should be shown to the
     * user ASAP
     *
     * @param frame the current frame
     */
    protected abstract void onNewVideoFrame(TgqFrame frame);

    /**
     * This thread handles the audio playback, critical to the process since the
     * video is synced to the audio
     */
    private class AudioPlayer implements Runnable {

        public AudioPlayer() {
        }

        @Override
        public void run() {

            try {

                // Wait for the audio header to init audio
                EAAudioHeader localAudioHeader = audioHeader;
                if (localAudioHeader == null) {
                    synchronized (audioHeaderEvent) {
                        localAudioHeader = audioHeader;
                        if (localAudioHeader == null) {
                            try {
                                audioHeaderEvent.wait();
                                localAudioHeader = audioHeader;
                            } catch (InterruptedException ex) {
                                logger.log(Level.WARNING, "Audio header waiting interrupted!", ex);
                                return;
                            }
                        }
                    }
                }
                initAudio(localAudioHeader);

                // Wait for the start
                if (!bufferingComplete) {
                    synchronized (bufferedEvent) {
                        if (!bufferingComplete) {
                            try {
                                bufferedEvent.wait();
                            } catch (InterruptedException ex) {
                                logger.log(Level.WARNING, "Playing start delay interrupted!", ex);
                                return;
                            }
                        }
                    }
                }

                // Play audio
                EAAudioFrame audioFrame = null;
                while (!Thread.currentThread().isInterrupted()) {
                    if (audioFrame == null) {
                        audioFrame = audioFrames.poll();
                        if (audioFrame == null) {
                            if (!decoderThread.isAlive()) {

                                // No more audio data
                                return;
                            }

                            // We need to wait
                            logger.log(Level.WARNING, "Decoder still alive but frame queue is empty! Buffer too small?");
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException ex) {
                                return;
                            }
                            continue;
                        }
                    }

                    // Feed the stream
                    ByteBuffer buf = audioFrame.getPcm();
                    int written = line.write(buf.array(), buf.arrayOffset() + buf.position(), Math.min(line.available(), buf.remaining()));
                    buf.position(buf.position() + written);
                    if (buf.remaining() == 0) {
                        audioFrame = null;
                    } else {

                        // Sleep a bit to not take all CPU
                        Thread.sleep(10);
                    }
                }

            } catch (InterruptedException e) {
                // Just stopped by the user
            } catch (Exception e) {
                logger.log(Level.ERROR, "Audio player failed!", e);
            } finally {

                // Let the audio die nicely
                if (line != null) {
                    try {

                        // If not interrupted, wait until the audio is played completely
                        if (!Thread.currentThread().isInterrupted()) {
                            line.drain();
                        }
                        line.stop();
                        line.flush();
                        line.close();
                        line = null;
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to release the audio!", e);
                    }
                }

                // Make sure we call stop at the end
                countDownLatch.countDown();
                stop();
            }
        }

        /**
         * Initialize the audio
         *
         * @param audioHeader the audio header
         */
        private void initAudio(EAAudioHeader audioHeader) {
            AudioFormat format = new AudioFormat(audioHeader.getSampleRate(), audioHeader.getBitsPerSample(), audioHeader.getNumberOfChannels(), true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new RuntimeException("Line matching " + info + " not supported.");
            }
            try {
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format, 1024 * 4 * format.getFrameSize());
                line.start();
            } catch (LineUnavailableException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Plays the actual video frames, is synced to audio
     */
    private class VideoPlayer implements Runnable {

        public VideoPlayer() {
        }

        @Override
        public void run() {

            // Wait for the start
            if (!bufferingComplete) {
                synchronized (bufferedEvent) {
                    if (!bufferingComplete) {
                        try {
                            bufferedEvent.wait();
                        } catch (InterruptedException ex) {
                            logger.log(Level.WARNING, "Playing start delay interrupted!", ex);
                            return;
                        }
                    }
                }
            }

            // Advance the video frames
            long lastAudio = 0;
            int lastFrameIndex = -1;
            while (!Thread.interrupted()) {
                if (line != null) {
                    long newAudio = line.getMicrosecondPosition();
                    clock += newAudio - lastAudio;
                    lastAudio = newAudio;

                    int frameIndex = (int) Math.floor(clock / 1000f / FRAME_INTERVAL);
                    if (lastFrameIndex < frameIndex) {

                        // New frame
                        TgqFrame videoFrame = videoFrames.poll();
                        while (videoFrame == null || videoFrame.getFrameIndex() < frameIndex) {
                            videoFrame = videoFrames.poll();
                            if (decoderThread.isAlive() && videoFrame == null) {
                                logger.log(Level.WARNING, "Video decoding is late!");
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException ex) {
                                    return;
                                }
                            } else if (!decoderThread.isAlive()) {

                                // Decoder is dead already and no frames
                                return;
                            }
                        }
                        onNewVideoFrame(videoFrame);
                        int late = frameIndex - (lastFrameIndex == -1 ? 0 : lastFrameIndex);
                        if (late > 1) {

                            logger.log(Level.WARNING, "Video is late {0} frames!", late);
                        }
                        lastFrameIndex = frameIndex;
                    } else {

                        // No frame change, sleep just a tiny bit not to take all the CPU
                        try {
                            Thread.sleep(2, 0);
                        } catch (InterruptedException ex) {
                            return;
                        }
                    }
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Decoder, decodes the movie frames, keeps the buffers filled
     */
    private class TgqDecoder implements Runnable {

        private final Path file;

        private TgqDecoder(Path file) {
            this.file = file;
        }

        @Override
        public void run() {
            try {
                try (TgqFile tgqFile = new TgqFile(file) {
                    @Override
                    protected void addVideoFrame(TgqFrame frame) {
                        videoFrames.add(frame);
                    }

                    @Override
                    protected void addAudioFrame(EAAudioFrame frame) {
                        audioFrames.add(frame);
                    }

                    @Override
                    protected void onAudioHeader(EAAudioHeader audioHeader) {
                        TgqPlayer.this.audioHeader = audioHeader;
                        synchronized (audioHeaderEvent) {
                            audioHeaderEvent.notifyAll();
                        }
                    }
                }) {
                    while (!Thread.interrupted() && tgqFile.readFrame()) {

                        // Read the frames, but not too fast
                        if (audioFrames.size() >= MAX_FRAME_COUNT_IN_BUFFER) {
                            if (!bufferingComplete) {
                                bufferingComplete = true;
                                synchronized (bufferedEvent) {
                                    bufferedEvent.notifyAll();
                                }
                            }
                            Thread.sleep(FRAME_INTERVAL);
                        }
                    }
                } catch (InterruptedException ex) {
                    // No biggie
                } catch (Exception ex) {
                    logger.log(Level.ERROR, "Failed to decode the frames on file " + file + "!", ex);
                    stop();
                }
            } finally {

                // If someone is still waiting
                if (!bufferingComplete) {
                    bufferingComplete = true;
                    synchronized (bufferedEvent) {
                        bufferedEvent.notifyAll();
                    }
                }
            }
        }
    }
}
