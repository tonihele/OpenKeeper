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
package toniarts.openkeeper.video.tgq;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.Utils;

/**
 * Parses a DK II movie file<br>
 * A little endian file, containing audio and video, just one stream of each.
 * Audio and video frames are ~interspersedly positioned in the file.<br>
 * The format is actually called Texture Quantized Image (TQI), but named here
 * for the extension the DK II movie files have.<br>
 *
 * Source: http://wiki.multimedia.cx/index.php?title=Electronic_Arts_Formats
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TgqFile implements AutoCloseable {

    private final RandomAccessFile file;
    private EAAudioHeader audioHeader;
    private Integer width;
    private Integer height;
    private int numberOfAudioStreamChunks;
    private int audioFrameIndex = 0;
    private int videoFrameIndex = 0;
    public ConcurrentLinkedQueue<EAAudioFrame> audioFrames = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<TgqFrame> videoFrames = new ConcurrentLinkedQueue<>();
    private final static String TQG_TAG = "pIQT";
    private final static String SCHl_TAG = "SCHl";
    private final static String SHEN_TAG = "SHEN";
    private final static String SCCl_TAG = "SCCl";
    private final static String SCEN_TAG = "SCEN";
    private final static String SCDl_TAG = "SCDl";
    private final static String SDEN_TAG = "SDEN";
    private final static String SCEl_TAG = "SCEl";
    private final static String SEEN_TAG = "SEEN";
    private final static String PT_PATCH_TAG = "PT";
    private static final Logger logger = Logger.getLogger(TgqFile.class.getName());

    public static void main(String[] args) throws IOException {

        // Take a movie file as parameter
        if (args.length != 1 || !new File(args[0]).exists()) {
            throw new RuntimeException("Please provide a TGQ movie file as first parameter!");
        }

        // Create the video parser
        try (TgqFile tgq = new TgqFile(new File(args[0]))) {
            TgqFrame frame = null;
//            while ((frame = tgq.readFrame()) != null) {
            while (tgq.readFrame()) {
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse frame!", e);
        }
    }

    public TgqFile(File file) throws FileNotFoundException {
        this.file = new RandomAccessFile(file, "r");
    }

    @Override
    public void close() throws Exception {
        file.close();
    }

    public boolean readFrame() throws IOException {
        boolean gotFrame = false;

        if (file.getFilePointer() == file.length()) {
            return false; // EOF
        }

        // Read the next FourCC
        long pos = file.getFilePointer();
        byte[] bytes = new byte[4];
        file.read(bytes);
        int frameSize = Utils.readInteger(file);

        // See what kind of frame we are dealing with here
        switch (Utils.bytesToString(bytes)) {
            case SHEN_TAG:
            case SCHl_TAG: {

                // Audio header, set some context parameters
                readAudioHeader(pos, frameSize);

                gotFrame = true;
                break;
            }
            case SCEN_TAG:
            case SCCl_TAG: {

                // Number of audio data tags
                numberOfAudioStreamChunks = Utils.readUnsignedInteger(file);

                gotFrame = true;
                break;
            }
            case SDEN_TAG:
            case SCDl_TAG: {

                // Audio data itself
                byte[] data = new byte[frameSize - 8];
                file.read(data);
                audioFrames.add(new EAAudioFrame(audioHeader, data, audioFrameIndex));

                gotFrame = true;
                audioFrameIndex++;
                break;
            }
            case SEEN_TAG:
            case SCEl_TAG: {

                // End of audio stream, nothing really to do

                gotFrame = true;
                break;
            }
            case TQG_TAG: {

                // Video frame
                byte[] data = new byte[frameSize - 8];
                file.read(data);
                TgqFrame frame = new TgqFrame(data, videoFrameIndex);
                videoFrames.add(frame);

                // See if we have the data
                if (width == null || height == null) {
                    width = frame.getWidth();
                    height = frame.getHeight();
                }

                gotFrame = true;
                videoFrameIndex++;
                break;
            }
            default: {
                logger.log(Level.WARNING, "Unkown tag {0}!", Utils.bytesToString(bytes));
                break;
            }
        }

        // Make sure we leave the file in sensible location
        if (file.getFilePointer() != pos + frameSize) {
            logger.log(Level.WARNING, "Invalid file position! Was {0}, should be {1}!", new Object[]{file.getFilePointer(), pos + frameSize});
            file.seek(pos + frameSize);
        }

        return gotFrame;
    }

    private void readAudioHeader(long pos, int frameSize) throws IOException {

        // Support only PT patch
        byte[] bytes = new byte[2];
        file.read(bytes);
        if (!PT_PATCH_TAG.equals(Utils.bytesToString(bytes))) {
            throw new RuntimeException(PT_PATCH_TAG + " was expected in audio header! But " + Utils.bytesToString(bytes) + " found!");
        }
        audioHeader = new EAAudioHeader();

        // Set PT defaults
        audioHeader.setBitsPerSample(16);
        audioHeader.setCompression(EAAudioHeader.Compression.EA_XA_ADPCM);
        audioHeader.setNumberOfChannels(1);
        audioHeader.setSampleRate(22050);

        int platformIdentifier = Utils.readShort(file);
        switch (platformIdentifier) {
            case 0x00: {
                audioHeader.setPlatform(EAAudioHeader.Platform.PC);
                break;
            }
            case 0x03: {
                audioHeader.setPlatform(EAAudioHeader.Platform.MACINTOSH);
                break;
            }
            case 0x05: {
                audioHeader.setPlatform(EAAudioHeader.Platform.PLAYSTATION_2);
                break;
            }
            case 0x06: {
                audioHeader.setPlatform(EAAudioHeader.Platform.GAME_CUBE);
                break;
            }
            case 0x07: {
                audioHeader.setPlatform(EAAudioHeader.Platform.XBOX);
                break;
            }
            case 0x09: {
                audioHeader.setPlatform(EAAudioHeader.Platform.XENON_XBOX_360);
                break;
            }
            case 0x0A: {
                audioHeader.setPlatform(EAAudioHeader.Platform.PSP);
                break;
            }
            default: {
                audioHeader.setPlatform(EAAudioHeader.Platform.UNKNOWN);
            }
        }

        // Tag data
        while (file.getFilePointer() < (pos + frameSize)) {
            short tag = (short) file.readUnsignedByte();
            if (tag == 0xFF) {
                return;
            } else if (tag == 0x8A || tag == 0xFC || tag == 0xFD || tag == 0xFE) {

                // No data
                continue;
            }
            short length = (short) file.readUnsignedByte();

            // See the tag
            switch (tag) {
                case 0x82: {
                    audioHeader.setNumberOfChannels(getValue(length));
                    break;
                }
                case 0x83: {
                    switch (getValue(length)) {
                        case 0x00: {
                            audioHeader.setCompression(EAAudioHeader.Compression.PCM_16_I_LE); // We don't know the bits really
                            break;
                        }
                        case 0x07: {
                            audioHeader.setCompression(EAAudioHeader.Compression.EA_XA_ADPCM);
                            break;
                        }
                        case 0x09: {
                            audioHeader.setCompression(EAAudioHeader.Compression.UNKNOWN);
                            break;
                        }
                    }
                    break;
                }
                case 0x85: {
                    audioHeader.setNumberOfSamplesInStream(getValue(length));
                    break;
                }
                default: {
                    logger.log(Level.INFO, "Did not process tag {0}!", tag);
                    file.skipBytes(length);
                    break;
                }
            }
        }
    }

    /**
     * Gets a value of given length (in bytes)
     *
     * @param length length in bytes
     * @return value from given length
     * @throws IOException
     */
    private int getValue(final short length) throws IOException {
        int value = 0;
        byte[] bytes = new byte[length];
        file.read(bytes);

        // Always big endian
        for (byte b : bytes) {
            value <<= 8;
            value |= b & 0xff; // Unsigned
        }
        return value;
    }
}
