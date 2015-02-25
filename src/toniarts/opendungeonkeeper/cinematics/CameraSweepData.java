/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.cinematics;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Essentially a JME wrapper for the Dungeon Keeper II KCS files
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CameraSweepData implements Savable {

    private List<CameraSweepDataEntry> entries;

    /**
     * Get a list of camera sweep data entries
     *
     * @return list of entries
     */
    public List<CameraSweepDataEntry> getEntries() {
        return entries;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.writeSavableArrayList((ArrayList) entries, "entries", (ArrayList) Collections.emptyList());
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        in.readSavableArrayList("entries", (ArrayList) Collections.emptyList());
    }
}
