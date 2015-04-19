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

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.video.tgq.EAAudioFrame;
import toniarts.openkeeper.video.tgq.TgqFile;
import toniarts.openkeeper.video.tgq.TgqFrame;

/**
 * "Mediaplayer" for TGQ files<br>
 * Kinda like an interface between the actual canvas and the decoder<br>
 * Not rewindable etc.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class TgqPlayer {

    private File file;
    private static final int FPS = 15;
    private static final int FRAME_INTERVAL = (int) Math.floor(1000 / FPS); // In milliseconds
    private static final float FRAME_BUFFER_SIZE = 3; // In seconds, there is no fancy counter etc.
    private static final int MAX_FRAME_COUNT_IN_BUFFER = (int) (FPS * FRAME_BUFFER_SIZE);
    public ConcurrentLinkedQueue<EAAudioFrame> audioFrames = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<TgqFrame> videoFrames = new ConcurrentLinkedQueue<>();
    private Thread decoderThread;
    private Thread playerThread;
    private boolean stopped = true;
    private static final Logger logger = Logger.getLogger(TgqPlayer.class.getName());

    public TgqPlayer(File file) {
        this.file = file;
    }

    /**
     * Start playing the movie
     */
    public void play() {
        stopped = false;

        // Create and start the decoder thread
        TgqDecoder decoder = new TgqDecoder(file);
        decoderThread = new Thread(decoder, "TgqDecoder");
        decoderThread.start();

        // Create the player
        Player player = new Player(decoderThread);
        playerThread = new Thread(player, "TgqPlayer");
        playerThread.start();
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

            // Kill the player as well
            if (playerThread != null && playerThread.isAlive()) {
                playerThread.interrupt();
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
     * Player thread, does the actual playing logic
     */
    private class Player implements Runnable {

        private final Thread decoderThread;

        private Player(Thread decoderThread) {
            this.decoderThread = decoderThread;
        }

        @Override
        public void run() {
            try {

                // Wait until the hard working decoder has filled the buffer
                while (decoderThread.isAlive() && videoFrames.size() < MAX_FRAME_COUNT_IN_BUFFER) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        logger.log(Level.WARNING, "Playing start delay interrupted!", ex);
                        return;
                    }
                }

                // Start the actual playing process
                Long lastFrame = null;
                while (!Thread.interrupted() && (!videoFrames.isEmpty() || decoderThread.isAlive())) {
                    if (videoFrames.isEmpty()) {

                        // Shieeet, probably out of sync
                        logger.log(Level.WARNING, "Decoder still alive but frame queue is empty! Buffer too small?");
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ex) {
                            return;
                        }
                        continue;
                    }

                    // First frame
                    if (lastFrame == null) {
                        lastFrame = System.currentTimeMillis();
                        nextFrame();
                        continue;
                    }

                    // Sleep until the next frame
                    long sleepTime = (lastFrame + FRAME_INTERVAL) - System.currentTimeMillis();
                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException ex) {

                            // Interrupted
                            return;
                        }
                    } else {
                        logger.log(Level.WARNING, "Sleep time between frames is negative! Frame processing takes too long!");
                    }
                    lastFrame = System.currentTimeMillis();
                    nextFrame();
                }

            } finally {

                // Stop
                stop();
            }
        }

        private void nextFrame() {
            onNewVideoFrame(videoFrames.poll());
        }
    }

    /**
     * Decoder, decodes the movie frames, keeps the buffers filled
     */
    private class TgqDecoder implements Runnable {

        private final File file;

        private TgqDecoder(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            try (TgqFile tgqFile = new TgqFile(file) {
                @Override
                protected void addVideoFrame(TgqFrame frame) {
                    videoFrames.add(frame);
                }

                @Override
                protected void addAudioFrame(EAAudioFrame frame) {
                    audioFrames.add(frame);
                }
            }) {
                while (!Thread.interrupted() && tgqFile.readFrame()) {

                    // Read the frames, but not too fast
                    if (videoFrames.size() >= MAX_FRAME_COUNT_IN_BUFFER) {
                        Thread.sleep(FRAME_INTERVAL);
                    }
                }
            } catch (InterruptedException ex) {
                // No biggie
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Failed to decode the frames on file " + file + "!", ex);
            }
        }
    }
}
