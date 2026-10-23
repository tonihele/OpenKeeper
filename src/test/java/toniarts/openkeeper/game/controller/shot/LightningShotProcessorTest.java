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
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.KnockedDown;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LightningShotProcessorTest {

    private static final double GAME_TIME = 12.5;
    private static final short PLAYER_ID = 3;
    private static final int SHOT_DATA = 150;

    /**
     * 15 damage, as Prima's guide gives for the Thunderbolt
     */
    private static final int DAMAGE = 15;

    private final EntityData entityData = new DefaultEntityData();
    private final LightningShotProcessor processor = new LightningShotProcessor(entityData, () -> GAME_TIME);
    private final Shot thunderbolt = ShotFixtures.thunderbolt();

    @Test
    void strikeDamagesAndKnocksDownTheCreature() {
        EntityId creature = createCreature(new Vector3f(3.5f, 1f, 4.5f));

        processor.process(strike(creature));

        assertEquals(DAMAGE, entityData.getComponent(creature, Damage.class).damage);
        assertNotNull(entityData.getComponent(creature, KnockedDown.class));
    }

    @Test
    void upgradedStrikeDamagesTwenty() {
        EntityId creature = createCreature(new Vector3f(3.5f, 1f, 4.5f));

        processor.process(new ShotRequest(thunderbolt, 200, 0, PLAYER_ID, new Vector3f(), creature));

        assertEquals(20, entityData.getComponent(creature, Damage.class).damage);
    }

    @Test
    void strikeLeavesTheBoltWhereTheTargetStands() {
        Vector3f creaturePosition = new Vector3f(3.5f, 1f, 4.5f);
        EntityId creature = createCreature(creaturePosition);

        // The cursor is a bit off, the bolt still hits the creature
        processor.process(new ShotRequest(thunderbolt, SHOT_DATA, 0, PLAYER_ID, new Vector3f(3.9f, 1f, 4.1f), creature));

        EntitySet shots = entityData.getEntities(ShotViewState.class, Position.class, Decay.class);
        assertEquals(1, shots.size());
        Entity shot = shots.iterator().next();
        assertEquals(ShotFixtures.THUNDERBOLT_ID, shot.get(ShotViewState.class).shotId);
        assertEquals(creaturePosition, shot.get(Position.class).position);
        assertEquals(GAME_TIME, shot.get(Decay.class).startTime);
        assertEquals(0.8, shot.get(Decay.class).duration, 1e-9);
        shots.release();
    }

    @Test
    void everyCastIsItsOwnStrike() {
        EntityId creature = createCreature(new Vector3f(3.5f, 1f, 4.5f));

        // Ten casts in a row, as fast as a player clicks
        for (int i = 0; i < 10; i++) {
            processor.process(strike(creature));
        }

        assertEquals(10 * DAMAGE, entityData.getComponent(creature, Damage.class).damage);
        EntitySet shots = entityData.getEntities(ShotViewState.class);
        assertEquals(10, shots.size());
        shots.release();
    }

    @Test
    void somethingElseThanACreatureIsOnlyDamaged() {
        EntityId thing = entityData.createEntity();
        entityData.setComponent(thing, new Position(0, new Vector3f(2.5f, 1f, 2.5f)));

        processor.process(strike(thing));

        assertEquals(DAMAGE, entityData.getComponent(thing, Damage.class).damage);
        assertNull(entityData.getComponent(thing, KnockedDown.class));
    }

    @Test
    void targetThatIsGoneIsNotStruck() {
        EntityId gone = entityData.createEntity();

        processor.process(strike(gone));
        processor.process(strike(null));

        assertNull(entityData.getComponent(gone, Damage.class));
        EntitySet shots = entityData.getEntities(ShotViewState.class);
        assertEquals(0, shots.size());
        shots.release();
    }

    private EntityId createCreature(Vector3f position) {
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Position(0, position.clone()), new CreatureComponent());
        return creature;
    }

    private ShotRequest strike(EntityId target) {
        return new ShotRequest(thunderbolt, SHOT_DATA, 0, PLAYER_ID, new Vector3f(), target);
    }

}
