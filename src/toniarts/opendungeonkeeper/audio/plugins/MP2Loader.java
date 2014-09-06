/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.audio.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.audio.plugins.WAVLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;
import toniarts.opendungeonkeeper.audio.plugins.coverter.WaveFileObuffer;
import toniarts.opendungeonkeeper.audio.plugins.decoder.Bitstream;
import toniarts.opendungeonkeeper.audio.plugins.decoder.BitstreamException;
import toniarts.opendungeonkeeper.audio.plugins.decoder.Decoder;
import toniarts.opendungeonkeeper.audio.plugins.decoder.Header;
import toniarts.opendungeonkeeper.audio.plugins.decoder.Obuffer;

/**
 * Converts MP2/MP3 files to WAV on the fly, uses the previously converted files
 * from temp cache
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MP2Loader extends WAVLoader {

    private final static HashMap<AssetKey, File> convertedAssets = new HashMap<>();
    private final static Object lock = new Object();

    public static void main(String[] args) throws JavaLayerException, FileNotFoundException, BitstreamException {
        Decoder decoder = new Decoder();
        Bitstream stream = new Bitstream(new FileInputStream("C:\\temp\\OpenDungeonKeeper\\sounds\\speech_horny\\horng014.mp2"));
        int frameCount = Integer.MAX_VALUE;

        int frame = 0;
        Obuffer output = null;
        try {
            for (; frame < frameCount; ++frame) {
                try {
                    Header header = stream.readFrame();
                    if (header == null) {
                        break;
                    }

//						progressListener.readFrame(frame, header);

                    if (output == null) {
                        // REVIEW: Incorrect functionality.
                        // the decoder should provide decoded
                        // frequency and channels output as it may differ from
                        // the source (e.g. when downmixing stereo to mono.)
                        int channels = (header.mode() == Header.SINGLE_CHANNEL) ? 1 : 2;
                        int freq = header.frequency();
                        output = new WaveFileObuffer(channels, freq, "C:\\temp\\OpenDungeonKeeper\\sounds\\speech_horny\\horng014.wav");
                        decoder.setOutputBuffer(output);
                    }

                    Obuffer decoderOutput = decoder.decodeFrame(header, stream);

                    // REVIEW: the way the output buffer is set
                    // on the decoder is a bit dodgy. Even though
                    // this exception should never happen, we test to be sure.
//						if (decoderOutput!=output)
//							throw new InternalError("Output buffers are different.");


//						progressListener.decodedFrame(frame, header, output);

                    stream.closeFrame();

                } catch (Exception ex) {
                }
            }

        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    @Override
    public Object load(final AssetInfo assetInfo) throws IOException {
        AssetInfo newAssetInfo = new AssetInfo(assetInfo.getManager(), assetInfo.getKey()) {
            @Override
            public InputStream openStream() {

                //See if we have the asset already converted
                if (!convertedAssets.containsKey(key) || !convertedAssets.get(key).exists()) {
                    synchronized (lock) {
                        if (!convertedAssets.containsKey(key) || !convertedAssets.get(key).exists()) {

                            //Convert!
                            Converter c = new Converter();
                            try (InputStream inputStream = assetInfo.openStream()) {
                                File tempFile = File.createTempFile(key.getName().replaceAll("/", "_"), ".wav");
                                c.convert(inputStream, tempFile.toString(), null, null);
                                tempFile.deleteOnExit();

                                //Add it to our converted list
                                convertedAssets.put(key, tempFile);
                            } catch (Exception ex) {
                                Logger.getLogger(MP2Loader.class.getName()).log(Level.SEVERE, "Failed to convert the resource!", ex);
                            }
                        }
                    }
                }
                try {
                    return new FileInputStream(convertedAssets.get(key));
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(MP2Loader.class.getName()).log(Level.SEVERE, "Failed to open resource!", ex);
                    return null;
                }
            }
        };
        return super.load(newAssetInfo);
    }
}
