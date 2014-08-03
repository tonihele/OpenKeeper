/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.opendungeonkeeper.tools.convert.kmf.KmfFile;

/**
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KmfModelLoader implements AssetLoader {

    public static void main(final String[] args) throws IOException {
        AssetInfo ai = new AssetInfo(null, null) {
            @Override
            public InputStream openStream() {
                try {
                    final File file = new File(args[0]);
                    key = new AssetKey() {
                        @Override
                        public String getName() {
                            return file.toPath().getFileName().toString();
                        }
                    };
                    return new FileInputStream(file);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(KmfModelLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }
        };
        KmfModelLoader kmfModelLoader = new KmfModelLoader();
        kmfModelLoader.load(ai);
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {

        KmfFile kmfFile = new KmfFile(inputStreamToFile(assetInfo.openStream(), assetInfo.getKey().getName()));

        return null;
    }

    /**
     * Converts input stream to a file by writing it to a temp file (yeah...)
     *
     * @param is the InputStream
     * @param prefix temp file prefix
     * @return random access file
     * @throws IOException
     */
    public static File inputStreamToFile(InputStream is, String prefix) throws IOException {
        File tempFile = File.createTempFile(prefix, "kmf");
        tempFile.deleteOnExit();

        //Write the file
        try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(tempFile))) {

            //Write in blocks
            byte[] buffer = new byte[2048];
            int tmp;

            while ((tmp = is.read(buffer)) != -1) {
                output.write(buffer, 0, tmp);
            }
        }


        return tempFile;
    }
}
