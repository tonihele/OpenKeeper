/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.state;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.nulldevice.NullSoundDevice;
import de.lessvoid.nifty.spi.input.InputSystem;
import de.lessvoid.nifty.spi.render.RenderDevice;
import de.lessvoid.nifty.spi.time.impl.AccurateTimeProvider;
import java.lang.reflect.Proxy;
import java.util.ListResourceBundle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The HUD is taken down when the level ends, but the level still runs and its
 * speeches still show their texts
 */
public class PlayerScreenControllerTest {

    @Test
    public void levelTextsStayAfterTheHudIsTakenDown() {
        Nifty nifty = new Nifty(stub(RenderDevice.class), new NullSoundDevice(), stub(InputSystem.class),
                new AccurateTimeProvider());
        nifty.addResourceBundle("level", new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[][]{{"7", "Sweet victory is yours"}};
            }
        });

        new PlayerScreenController(null, nifty).cleanup();

        assertEquals("Sweet victory is yours", nifty.specialValuesReplace("${level.7}"));
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == int.class) {
                return 0;
            }
            return null;
        });
    }
}
