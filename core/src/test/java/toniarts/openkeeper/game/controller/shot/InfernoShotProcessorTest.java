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
package toniarts.openkeeper.game.controller.shot;

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.InfernoArea;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfernoShotProcessorTest {

    private static final double GAME_TIME = 20.0;
    private static final short PLAYER_ID = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final InfernoShotProcessor processor = new InfernoShotProcessor(entityData, () -> GAME_TIME, () -> 4);
    private final Shot inferno = ShotFixtures.inferno();

    @Test
    void fireBurnsWhereTheSpellIsCastForAboutEightSeconds() {
        Vector3f position = new Vector3f(6.2f, 1f, 9.8f);

        // Shot data as cast: strength 100, reach 1 tile
        processor.process(new ShotRequest(inferno, 100, 1, PLAYER_ID, position, null));

        EntitySet fires = entityData.getEntities(InfernoArea.class, ShotViewState.class, Position.class, Decay.class);
        assertEquals(1, fires.size());
        Entity fire = fires.iterator().next();
        assertEquals(ShotFixtures.INFERNO_ID, fire.get(ShotViewState.class).shotId);
        assertEquals(position, fire.get(Position.class).position);
        InfernoArea area = fire.get(InfernoArea.class);
        assertEquals(1, area.radius);
        assertEquals(0, area.burns);
        assertEquals(GAME_TIME, fire.get(Decay.class).startTime, 1e-9);
        assertEquals(7.5, fire.get(Decay.class).duration, 1e-9);
        fires.release();
    }

    @Test
    void burnsTheShotDataEveryTick() {
        processor.process(new ShotRequest(inferno, 100, 1, PLAYER_ID, new Vector3f(), null));

        assertEquals(100, burnDamage());
    }

    @Test
    void upgradedSpellBurnsMore() {
        processor.process(new ShotRequest(inferno, 125, 1, PLAYER_ID, new Vector3f(), null));

        assertEquals(125, burnDamage());
    }

    private int burnDamage() {
        EntitySet fires = entityData.getEntities(InfernoArea.class);
        int damage = fires.iterator().next().get(InfernoArea.class).damage;
        fires.release();
        return damage;
    }

}
