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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Tremor;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.SpellShotFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShotsControllerTremorTest {

    private static final short TREMOR_SHOT_ID = 24;

    private final EntityData entityData = new DefaultEntityData();
    private final Shot tremorShot = SpellShotFixtures.shot(TREMOR_SHOT_ID, "Tremor", Shot.ProcessType.TREMOR, 10);

    @Test
    void castingTremorStartsATremorForShotData1Seconds() {
        IGameTimer gameTimer = () -> 100;
        ShotsController shotsController = new ShotsController(kwdFile(), entityData, null, gameTimer,
                null, null, null, null, null, null);

        // Shot data 1 is the duration in seconds, shot data 2 the radius in tiles
        shotsController.createShot(TREMOR_SHOT_ID, 5, 3, Player.KEEPER1_ID, new Vector3f(12.4f, 0, 7.6f), null, null);

        EntitySet tremors = entityData.getEntities(Tremor.class);
        assertEquals(1, tremors.size());
        Tremor tremor = tremors.iterator().next().get(Tremor.class);
        assertEquals(new Point(12, 8), tremor.centre);
        assertEquals(3, tremor.radius);
        assertEquals(100, tremor.startTime);
        assertEquals(105, tremor.endTime);
        tremors.release();
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getShotById") && (short) args[0] == TREMOR_SHOT_ID) {
                        return tremorShot;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

}
