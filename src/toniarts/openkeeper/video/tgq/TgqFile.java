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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.IResourceReader;
import toniarts.openkeeper.tools.convert.ResourceReader;

/**
 * Parses a DK II movie file<br>
 * A little endian file, containing audio and video, just one stream of each. Audio and video frames
 * are ~interspersedly positioned in the file.<br>
 * The format is actually called Texture Quantized Image (TQI), but named here for the extension the
 * DK II movie files have.<br>
 *
 * Source: http://wiki.multimedia.cx/index.php?title=Electronic_Arts_Formats
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class TgqFile implements AutoCloseable {

    private final ResourceReader file;
    private EAAudioHeader audioHeader;
    private Integer width;
    private Integer height;
    private int numberOfAudioStreamChunks;
    private int audioFrameIndex = 0;
    private int videoFrameIndex = 0;
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

    private static final Logger LOGGER = Logger.getLogger(TgqFile.class.getName());

    public static void main(final String[] args) throws IOException {

        if (args.length != 2) {
            throw new RuntimeException("Please provide a TGQ movie file as first parameter and frame output folder as the second parameter!");
        }

        // Take a movie file as parameter
        if (!new File(args[0]).exists()) {
            throw new RuntimeException("Movie file doesn't exist!");
        }

        new File(args[1]).mkdirs();

        // Create the video parser
        try (TgqFile tgq = new TgqFile(new File(args[0])) {
            @Override
            protected void addVideoFrame(TgqFrame frame) {
                File outputfile = new File(args[1].concat("Frame").concat(frame.getFrameIndex() + "").concat(".png"));
                try {
                    ImageIO.write(frame.getImage(), "png", outputfile);
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to save the movie frame!", ex);
                }
            }

            @Override
            protected void addAudioFrame(EAAudioFrame frame) {
                // Not interested
            }

            @Override
            protected void onAudioHeader(EAAudioHeader audioHeader) {
                // Not interested
            }
        }) {
            while (tgq.readFrame()) {
                // Read the frames
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse frame!", e);
        }
    }

    public TgqFile(File file) throws FileNotFoundException {
        this.file = new ResourceReader(file);
    }

    @Override
    public void close() throws Exception {
        file.close();
    }

    public boolean readFrame() throws IOException {
        boolean gotFrame = false;

        if (file.isEof()) {
            return false; // EOF
        }

        // Read the next FourCC
        long pos = file.getFilePointer();
        String tag = file.readString(4);
        int frameSize = file.readInteger();

        // See what kind of frame we are dealing with here
        switch (tag) {
            case SHEN_TAG:
            case SCHl_TAG: {

                // Audio header, set some context parameters
                readAudioHeader(pos, frameSize);
                onAudioHeader(audioHeader);

                gotFrame = true;
                break;
            }
            case SCEN_TAG:
            case SCCl_TAG: {

                // Number of audio data tags
                numberOfAudioStreamChunks = file.readUnsignedInteger();

                gotFrame = true;
                break;
            }
            case SDEN_TAG:
            case SCDl_TAG: {

                // Audio data itself
                byte[] data = file.read(frameSize - 8);
                addAudioFrame(new EAAudioFrame(audioHeader, data, audioFrameIndex));

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
                byte[] data = file.read(frameSize - 8);
                TgqFrame frame = new TgqFrame(data, videoFrameIndex);
                addVideoFrame(frame);

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
                LOGGER.log(Level.WARNING, "Unkown tag {0}!", tag);
                break;
            }
        }

        // Make sure we leave the file in sensible location
        if (file.getFilePointer() != pos + frameSize) {
            LOGGER.log(Level.WARNING, "Invalid file position! Was {0}, should be {1}!", new Object[]{file.getFilePointer(), pos + frameSize});
            file.seek(pos + frameSize);
        }

        return gotFrame;
    }

    /**
     * Called when audio header is got
     *
     * @param audioHeader the header
     */
    protected abstract void onAudioHeader(final EAAudioHeader audioHeader);

    /**
     * A video frame has been decoded
     *
     * @param frame the decoded frame
     */
    protected abstract void addVideoFrame(final TgqFrame frame);

    /**
     * A audio frame has been decoded
     *
     * @param frame the decoded frame
     */
    protected abstract void addAudioFrame(final EAAudioFrame frame);

    private void readAudioHeader(long pos, int frameSize) throws IOException {

        // Support only PT patch
        String headerTag = file.readString(2);
        if (!PT_PATCH_TAG.equals(headerTag)) {
            throw new RuntimeException(PT_PATCH_TAG + " was expected in audio header! But " + headerTag + " found!");
        }
        audioHeader = new EAAudioHeader();

        // Set PT defaults
        audioHeader.setBitsPerSample(16);
        audioHeader.setCompression(EAAudioHeader.Compression.EA_XA_ADPCM);
        audioHeader.setNumberOfChannels(1);
        audioHeader.setSampleRate(22050);

        int platformIdentifier = file.readShort();
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
        boolean inSubStream;
        while (file.getFilePointer() < (pos + frameSize)) {
            short tag = file.readUnsignedByte();

            switch (tag) {
                case 0xFC:
                case 0xFE:
                case 0xFD: {

                    // Start of a sub stream
                    inSubStream = true;

                    // Loop through the sub stream
                    while (inSubStream && file.getFilePointer() < (pos + frameSize)) {
                        short subTag = file.readUnsignedByte();
                        switch (subTag) {
                            case 0x82: {
                                audioHeader.setNumberOfChannels(getValue(file));
                                break;
                            }
                            case 0x83: {
                                switch (getValue(file)) {
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
                                audioHeader.setNumberOfSamplesInStream(getValue(file));
                                break;
                            }
                            case 0x8A: {
                                inSubStream = false;
                                getValue(file); // Specs say this has no data but I beg to differ
                                break;
                            }
                            case 0xFF: {
                                return; // The end
                            }
                            default: {
                                LOGGER.log(Level.INFO, "Did not process sub stream tag {0}!", subTag);
                                getValue(file);
                            }
                        }
                    }
                    break;
                }
                case 0xFF: {
                    return; // The end
                }
                default: {
                    LOGGER.log(Level.INFO, "Did not process tag {0}!", tag);
                    getValue(file);
                }
            }
        }
    }

    /**
     * Gets a value from the file, first reading the length (a byte that signifies the number of
     * bytes to read for the value) of the value, and the value itself
     *
     * @param file file to read from
     * @return value value from the file
     * @throws IOException
     */
    private static int getValue(final IResourceReader file) throws IOException {
        int value = 0;
        short length = file.readUnsignedByte();
        byte[] bytes = file.read(length);

        // Always big endian
        for (byte b : bytes) {
            value <<= 8;
            value |= b & 0xff; // Unsigned
        }
        return value;
    }
}
