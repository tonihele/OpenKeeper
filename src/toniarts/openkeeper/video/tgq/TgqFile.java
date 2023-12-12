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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.BufferedResourceReader;
import toniarts.openkeeper.tools.convert.IResourceChunkReader;
import toniarts.openkeeper.tools.convert.IResourceReader;

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
    
    private static final Logger LOGGER = System.getLogger(TgqFile.class.getName());

    private final IResourceReader file;
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

    public static void main(final String[] args) throws IOException {

        if (args.length != 2) {
            throw new RuntimeException("Please provide a TGQ movie file as first parameter and frame output folder as the second parameter!");
        }

        // Take a movie file as parameter
        Path file = Paths.get(args[1]);
        if (!Files.exists(file)) {
            throw new RuntimeException("Movie file doesn't exist!");
        }

        Files.createDirectories(file);

        // Create the video parser
        try (TgqFile tgq = new TgqFile(file) {
            @Override
            protected void addVideoFrame(TgqFrame frame) {
                File outputfile = new File(args[1].concat("Frame").concat(frame.getFrameIndex() + "").concat(".png"));
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputfile))){
                    ImageIO.write(frame.getImage(), "png", bos);
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

    protected TgqFile(Path file) throws IOException {
        this.file = new BufferedResourceReader(file);
    }

    @Override
    public void close() throws Exception {
        file.close();
    }

    public boolean readFrame() throws IOException {
        boolean gotFrame = false;

        // Read the next FourCC
        IResourceChunkReader reader = file.readChunk(8);
        if(reader == null) {
            return false; // EOF
        }
        
        String tag = reader.readString(4);
        int frameSize = reader.readInteger();

        // See what kind of frame we are dealing with here
        switch (tag) {
            case SHEN_TAG, SCHl_TAG -> {

                // Audio header, set some context parameters
                readAudioHeader(frameSize - 8);
                onAudioHeader(audioHeader);

                gotFrame = true;
            }
            case SCEN_TAG, SCCl_TAG -> {

                // Number of audio data tags
                numberOfAudioStreamChunks = file.readChunk(4).readUnsignedInteger();
                gotFrame = true;
            }
            case SDEN_TAG, SCDl_TAG -> {

                // Audio data itself
                addAudioFrame(new EAAudioFrame(audioHeader, file.readChunk(frameSize - 8).getByteBuffer(), audioFrameIndex));

                gotFrame = true;
                audioFrameIndex++;
            }
            case SEEN_TAG, SCEl_TAG -> {

                // End of audio stream, nothing really to do
                gotFrame = true;
            }
            case TQG_TAG -> {

                // Video frame
                TgqFrame frame = new TgqFrame(file.readChunk(frameSize - 8).getByteBuffer(), videoFrameIndex);
                addVideoFrame(frame);

                // See if we have the data
                if (width == null || height == null) {
                    width = frame.getWidth();
                    height = frame.getHeight();
                }

                gotFrame = true;
                videoFrameIndex++;
            }
            default -> {
                LOGGER.log(Level.WARNING, "Unkown tag {0}!", tag);
            }
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

    private void readAudioHeader(int frameSize) throws IOException {
        IResourceChunkReader reader = file.readChunk(frameSize);

        // Support only PT patch
        String headerTag = reader.readString(2);
        if (!PT_PATCH_TAG.equals(headerTag)) {
            throw new RuntimeException(PT_PATCH_TAG + " was expected in audio header! But " + headerTag + " found!");
        }
        audioHeader = createDefaultAudioHeader();

        int platformIdentifier = reader.readShort();
        audioHeader.setPlatform(getPlatform(platformIdentifier));

        readAudioHeaderTagData(reader, audioHeader);
    }

    private static void readAudioHeaderTagData(IResourceChunkReader reader, EAAudioHeader audioHeader) throws IOException {
        while (reader.hasRemaining()) {
            short tag = reader.readUnsignedByte();
            switch (tag) {
                case 0xFC, 0xFE, 0xFD -> {
                    if (readAudioHeaderSubStream(reader, audioHeader)) {
                        return;
                    }
                }
                case 0xFF -> {

                    // The end
                    return;
                }
                default -> {
                    int val = getValue(reader);
                    LOGGER.log(Level.INFO, "Did not process tag {0}! Value: " + val, tag);
                }
            }
        }
    }

    /**
     * Reads audio header sub stream
     *
     * @param reader header resource reader
     * @param audioHeader the audio header data to read to
     * @return returns true if the whole stream is exhausted
     * @throws IOException
     */
    private static boolean readAudioHeaderSubStream(IResourceChunkReader reader, EAAudioHeader audioHeader) throws IOException {
        while (reader.hasRemaining()) {
            short subTag = reader.readUnsignedByte();
            switch (subTag) {
                case 0x82 -> {
                    audioHeader.setNumberOfChannels(getValue(reader));
                }
                case 0x83 -> {
                    int compressionIdentifier = getValue(reader);
                    audioHeader.setCompression(getCompression(compressionIdentifier));
                }
                case 0x85 -> {
                    audioHeader.setNumberOfSamplesInStream(getValue(reader));
                }
                case 0x8A -> {

                    // Exit sub stream
                    // Specs say this has no data but I beg to differ
                    getValue(reader);

                    return false;
                }
                case 0xFF -> {

                    // The end
                    return true;
                }
                default -> {
                    LOGGER.log(Level.INFO, "Did not process sub stream tag {0}!", subTag);
                    getValue(reader);
                }
            }
        }

        return false;
    }

    private static EAAudioHeader createDefaultAudioHeader() {
        EAAudioHeader audioHeader = new EAAudioHeader();

        // Set PT defaults
        audioHeader.setBitsPerSample(16);
        audioHeader.setCompression(EAAudioHeader.Compression.EA_XA_ADPCM);
        audioHeader.setNumberOfChannels(1);
        audioHeader.setSampleRate(22050);

        return audioHeader;
    }

    private static EAAudioHeader.Platform getPlatform(int platformIdentifier) {
        switch (platformIdentifier) {
            case 0x00 -> {
                return EAAudioHeader.Platform.PC;
            }
            case 0x03 -> {
                return EAAudioHeader.Platform.MACINTOSH;
            }
            case 0x05 -> {
                return EAAudioHeader.Platform.PLAYSTATION_2;
            }
            case 0x06 -> {
                return EAAudioHeader.Platform.GAME_CUBE;
            }
            case 0x07 -> {
                return EAAudioHeader.Platform.XBOX;
            }
            case 0x09 -> {
                return EAAudioHeader.Platform.XENON_XBOX_360;
            }
            case 0x0A -> {
                return EAAudioHeader.Platform.PSP;
            }
            default -> {
                return EAAudioHeader.Platform.UNKNOWN;
            }
        }
    }

    private static EAAudioHeader.Compression getCompression(int compressionIdentifier) {
        switch (compressionIdentifier) {
            case 0x00 -> {

                // We don't know the bits really
                return EAAudioHeader.Compression.PCM_16_I_LE;
            }
            case 0x07 -> {
                return EAAudioHeader.Compression.EA_XA_ADPCM;
            }
            case 0x09 -> {
                return EAAudioHeader.Compression.UNKNOWN;
            }
            default -> {
                return EAAudioHeader.Compression.UNKNOWN;
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
    private static int getValue(final IResourceChunkReader reader) throws IOException {
        int value = 0;
        short length = reader.readUnsignedByte();
        byte[] bytes = reader.read(length);

        // Always big endian
        for (byte b : bytes) {
            value <<= 8;
            value |= b & 0xff; // Unsigned
        }
        return value;
    }
}
