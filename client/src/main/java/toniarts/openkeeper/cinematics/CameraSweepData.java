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
package toniarts.openkeeper.cinematics;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Essentially a JME wrapper for the Dungeon Keeper II KCS files
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CameraSweepData implements Savable {

    private List<CameraSweepDataEntry> entries;

    public CameraSweepData(List<CameraSweepDataEntry> entries) {
        this.entries = entries;
    }

    /**
     * Serialization-only. Do not use.
     */
    public CameraSweepData() {
    }

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
        out.writeSavableArrayList((ArrayList) entries, "entries", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        entries = in.readSavableArrayList("entries", null);
    }
}
