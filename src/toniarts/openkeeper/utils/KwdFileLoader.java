/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import toniarts.openkeeper.tools.convert.FileResourceReader;
import toniarts.openkeeper.tools.convert.ISeekableResourceReader;
import toniarts.openkeeper.tools.convert.map.FilePath;

/**
 *
 * @author ArchDemon
 */
public final class KwdFileLoader {

    private final String basePath;
    private final boolean fullLoad;
    private boolean loaded = false;

    public KwdFileLoader(String basePath) {
        this(basePath, true);
    }

    public KwdFileLoader(String basePath, boolean fullLoad) {
        this.basePath = PathUtils.fixFilePath(basePath);
        this.fullLoad = fullLoad;
    }

    public ISeekableResourceReader getReader(String name) throws IOException {
        Path file = Paths.get(PathUtils.getRealFileName(this.basePath, PathUtils.DKII_MAPS_FOLDER + name));
        return new FileResourceReader(file);
    }

    public ISeekableResourceReader getReader(FilePath filePath) throws IOException {
        Path file = Paths.get(PathUtils.getRealFileName(this.basePath, filePath.getPath()));
        return new FileResourceReader(file);
    }

    public boolean isFullLoad() {
        return fullLoad;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

}
