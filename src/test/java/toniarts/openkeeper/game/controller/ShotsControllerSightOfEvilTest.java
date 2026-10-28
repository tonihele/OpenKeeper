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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector3f;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.SpellShotFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShotsControllerSightOfEvilTest {

    private static final short SIGHT_OF_EVIL_SHOT_ID = 33;

    private final Shot sightOfEvilShot = SpellShotFixtures.shot(SIGHT_OF_EVIL_SHOT_ID, "Sight Of Evil", Shot.ProcessType.SIGHT_OF_EVIL, 64);
    private final List<List<Object>> reveals = new ArrayList<>();

    @Test
    void castingSightOfEvilRevealsTheAreaToTheCaster() {
        ShotsController shotsController = new ShotsController(kwdFile(), new DefaultEntityData(), null, null,
                null, mapController(), null, null, null, null);

        // Shot data 1 is the radius, the shot lifetime is the duration
        shotsController.createShot(SIGHT_OF_EVIL_SHOT_ID, 7, 0, Player.KEEPER2_ID, new Vector3f(12.4f, 0, 7.6f), null, null);

        assertEquals(List.of(List.of(new Point(12, 8), 7, 64, Player.KEEPER2_ID)), reveals);
    }

    @Test
    void noRadiusNoReveal() {
        ShotsController shotsController = new ShotsController(kwdFile(), new DefaultEntityData(), null, null,
                null, mapController(), null, null, null, null);

        shotsController.createShot(SIGHT_OF_EVIL_SHOT_ID, 0, 0, Player.KEEPER2_ID, new Vector3f(12.4f, 0, 7.6f), null, null);

        assertEquals(List.of(), reveals);
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getShotById") && (short) args[0] == SIGHT_OF_EVIL_SHOT_ID) {
                        return sightOfEvilShot;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private IMapController mapController() {
        return (IMapController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> {
                    if (method.getName().equals("revealSightOfEvil")) {
                        reveals.add(List.of(args));
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

}
