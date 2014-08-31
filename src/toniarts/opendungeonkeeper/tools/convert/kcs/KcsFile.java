/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.kcs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Vector3f;
import toniarts.opendungeonkeeper.tools.convert.Utils;

/**
 * Stores the KCS file entries<br>
 * KCS file is a path information file, used to move cameras and spatials
 * around. Paths.wad contains a load of them<br>
 * The file is LITTLE ENDIAN I might say. I don't know it is relevant but DK II
 * run these at 30 FPS. So 150 entries equals 5 seconds of movement animation...
 * I reverse engineered this comparing the KCS files and the accompanied TXT
 * file
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KcsFile {

    private final File file;
    private final List<KcsEntry> kcsEntries;

    /**
     * Constructs a new Kcs file reader<br>
     * Reads the KCS file
     *
     * @param file the kcs file to read
     */
    public KcsFile(File file) {
        this.file = file;

        //Read the file
        try (RandomAccessFile rawKcs = new RandomAccessFile(file, "r")) {

            //Header
            int numOfEntries = Utils.readUnsignedInteger(rawKcs);
            rawKcs.skipBytes(12); // 12 bytes of emptiness?

            //Read the entries
            kcsEntries = new ArrayList<>(numOfEntries);
            for (int i = 0; i < numOfEntries; i++) {

                //Entries have 56 bytes in them
                //To me it looks like 4 vectors + 2 floats
                //But I'm not completely sure, actually not really sure at all
                //They do look like 3D coordinates though
                KcsEntry entry = new KcsEntry();
                entry.setUnknown1(new Vector3f(Utils.readFloat(rawKcs), Utils.readFloat(rawKcs), Utils.readFloat(rawKcs)));
                entry.setUnknown2(new Vector3f(Utils.readFloat(rawKcs), Utils.readFloat(rawKcs), Utils.readFloat(rawKcs)));
                entry.setUnknown3(new Vector3f(Utils.readFloat(rawKcs), Utils.readFloat(rawKcs), Utils.readFloat(rawKcs)));
                entry.setUnknown4(new Vector3f(Utils.readFloat(rawKcs), Utils.readFloat(rawKcs), Utils.readFloat(rawKcs)));
                entry.setUnknown5(Utils.readFloat(rawKcs));
                entry.setUnknown6(Utils.readFloat(rawKcs));
                kcsEntries.add(entry);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }

    public List<KcsEntry> getKcsEntries() {
        return kcsEntries;
    }
}
