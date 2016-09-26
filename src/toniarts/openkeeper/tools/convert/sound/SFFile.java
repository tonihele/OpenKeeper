/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.tools.convert.sound;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author ArchDemon
 */
public class SFFile {
    private final SFChunk chunk;

    public SFFile(File file) {
        //Read the file
        try (RandomAccessFile f = new RandomAccessFile(file, "r")) {
            chunk = new SFChunk(f);
        } catch (IOException e) {
            //Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }

    public SFChunk getChunk() {
        return chunk;
    }

    @Override
    public String toString() {
        return "Sf2File{" + "chunk=" + chunk + '}';
    }
}
