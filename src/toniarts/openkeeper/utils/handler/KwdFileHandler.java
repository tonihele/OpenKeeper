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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 *
 * @author ArchDemon
 */
public final class KwdFileHandler implements InvocationHandler {

    private final IKwdFile target;

    private final KwdFile.KwdFileLoader loader;

    private boolean loaded = false;

    private boolean initialized = false;

    public KwdFileHandler(KwdFile.KwdFileLoader loader, IKwdFile target) {
        this.loader = loader;
        this.target = target;
    }

    public IKwdFile getTarget() {
        return target;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] os) throws Throwable {
        if (!initialized) {
            initialized = true;
            getLoader().invoke(loader, target, true);
        }

        if (!loaded && !List.of("getName", "getMap").contains(method.getName())) {
            loaded = true;
            getLoader().invoke(loader, target, false);
        }

        return method.invoke(target, os);
    }

    private Method getLoader() throws NoSuchMethodException {
        Method method = KwdFile.KwdFileLoader.class.getDeclaredMethod("load", new Class<?>[]{KwdFile.class, boolean.class});
        method.setAccessible(true);

        return method;
    }
}
