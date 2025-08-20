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
package toniarts.openkeeper.utils.handler;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import toniarts.openkeeper.tools.convert.FileResourceReader;
import toniarts.openkeeper.tools.convert.ISeekableResourceReader;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.MapDataTypeEnum;
import toniarts.openkeeper.utils.PathUtils;

/**
 *
 * @author ArchDemon
 */
public final class KwdFileHandler implements InvocationHandler {

    private final IKwdFile target;

    private final String basePath;

    private boolean loaded = false;

    private boolean initialized = false;

    public KwdFileHandler(String basePath, IKwdFile target) {
        this.target = target;
        this.basePath = PathUtils.fixFilePath(basePath);
    }

    public IKwdFile getTarget() {
        return target;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] os) throws Throwable {
        if (!initialized) {
            initialized = true;
            Method loader = getLoader();

            load(loader, PathUtils.DKII_MAPS_FOLDER + target.getName());

            target.getGameLevel().getPaths().stream()
                    .filter(item -> item.getId() == MapDataTypeEnum.MAP)
                    .forEach(item -> load(loader, item.getPath()));
        }

        if (!loaded && !List.of("getName", "getMap").contains(method.getName())) {
            loaded = true;
            Method loader = getLoader();
            target.getGameLevel().getPaths().stream()
                    .filter(item -> item.getId() != MapDataTypeEnum.MAP)
                    .forEach(item -> load(loader, item.getPath()));
        }

        return method.invoke(target, os);
    }

    private void load(Method loader, String path) {
        try {
            Path f = Paths.get(PathUtils.getRealFileName(this.basePath, path));
            try (ISeekableResourceReader data = new FileResourceReader(f)) {
                loader.invoke(target, data);
            }
        } catch (IOException | IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Method getLoader() throws NoSuchMethodException {
        Method method = KwdFile.class.getDeclaredMethod("readFileContents",
                new Class<?>[]{ISeekableResourceReader.class});
        method.setAccessible(true);

        return method;
    }

}
