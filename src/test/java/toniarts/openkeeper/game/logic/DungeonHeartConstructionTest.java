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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.EffectTrigger;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Dungeon Heart opens up at the start of the level: the plug goes, then
 * the steps and the arches appear
 */
class DungeonHeartConstructionTest {

    private static final short PLAYER_ID = 3;
    private static final float TICK = 1 / 60f;
    private static final int PLUG_EFFECT_ID = 208;
    private static final int TICKS_PER_SECOND = 4;

    private EntityData entityData;
    private EntityId plug;
    private EntityId steps;
    private EntityId arches;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        plug = createObject(FiveByFiveRotatedController.OBJECT_PLUG_ID);
        steps = createObject(FiveByFiveRotatedController.OBJECT_BIG_STEPS_ID);
        arches = createObject(FiveByFiveRotatedController.OBJECT_ARCHES_ID);
    }

    @Test
    void stepsAndArchesAreHiddenUntilTheHeartIsBuilt() {
        new DungeonHeartConstruction(entityData, 0, PLUG_EFFECT_ID, TICKS_PER_SECOND);

        assertFalse(isVisible(steps));
        assertFalse(isVisible(arches));
        assertPlugPresent();
    }

    @Test
    void heartOpensWithoutADelay() {
        DungeonHeartConstruction construction = new DungeonHeartConstruction(entityData, 0, PLUG_EFFECT_ID, TICKS_PER_SECOND);

        runFor(construction, 5.9f);
        assertPlugPresent();

        runFor(construction, 0.2f);
        assertPlugRemoved();
        assertFalse(isVisible(arches));

        runFor(construction, 5f);
        assertPlugRemoved();
        assertTrue(isVisible(steps));
        assertTrue(isVisible(arches));
    }

    @Test
    void heartOpensAfterTheLevelsDelay() {

        // Level 2 waits 15 seconds and level 8 16 seconds before the construction begins
        DungeonHeartConstruction construction = new DungeonHeartConstruction(entityData, 15, PLUG_EFFECT_ID, TICKS_PER_SECOND);

        runFor(construction, 20.9f);
        assertPlugPresent();
        assertFalse(isVisible(arches));

        runFor(construction, 0.2f);
        assertPlugRemoved();
        assertFalse(isVisible(arches));

        runFor(construction, 5f);
        assertPlugRemoved();
        assertTrue(isVisible(steps));
        assertTrue(isVisible(arches));
    }

    @Test
    void plugIsRemovedEvenIfATickSkipsPastTheWholeConstruction() {
        DungeonHeartConstruction construction = new DungeonHeartConstruction(entityData, 0, PLUG_EFFECT_ID, TICKS_PER_SECOND);

        construction.processTick(12f);

        assertPlugRemoved();
        assertTrue(isVisible(steps));
        assertTrue(isVisible(arches));
    }

    @Test
    void levelStartsWithItsEffectAtTheHeart() {
        DungeonHeartConstruction construction = new DungeonHeartConstruction(entityData, 0, PLUG_EFFECT_ID, TICKS_PER_SECOND);

        runFor(construction, TICK);

        assertEquals(1, countEffects(225));
        assertEquals(0, countEffects(223));
    }

    @Test
    void plugCracklesWithStaticWhileTheLevelWaits() {
        DungeonHeartConstruction construction = new DungeonHeartConstruction(entityData, 15, PLUG_EFFECT_ID, TICKS_PER_SECOND);

        runFor(construction, 2f);

        // Every game tick, four a second
        assertEquals(8, countEffects(223));
        assertEquals(0, countEffects(225));
    }

    @Test
    void plugBreaksApartWithItsEffect() {
        DungeonHeartConstruction construction = new DungeonHeartConstruction(entityData, 0, PLUG_EFFECT_ID, TICKS_PER_SECOND);

        runFor(construction, 5.9f);
        assertEquals(0, countEffects(PLUG_EFFECT_ID));

        runFor(construction, 0.2f);
        assertEquals(1, countEffects(PLUG_EFFECT_ID));
    }

    @Test
    void archesFadeInWithTheirEffect() {
        DungeonHeartConstruction construction = new DungeonHeartConstruction(entityData, 0, PLUG_EFFECT_ID, TICKS_PER_SECOND);

        runFor(construction, 6.9f);
        assertEquals(0, countEffects(314));

        runFor(construction, 0.2f);
        assertEquals(1, countEffects(314));
    }

    @Test
    void effectsPlayOnTheFloorInTheMiddleOfTheHeart() {
        DungeonHeartConstruction construction = new DungeonHeartConstruction(entityData, 0, PLUG_EFFECT_ID, TICKS_PER_SECOND);

        runFor(construction, TICK);

        EntitySet effects = entityData.getEntities(EffectTrigger.class, Position.class);
        assertEquals(new Vector3f(0, WorldUtils.FLOOR_HEIGHT, 0), effects.iterator().next().get(Position.class).position);
        effects.release();
    }

    private int countEffects(int effectId) {
        int count = 0;
        EntitySet effects = entityData.getEntities(EffectTrigger.class);
        for (Entity effect : effects) {
            if (effect.get(EffectTrigger.class).effectId == effectId) {
                count++;
            }
        }
        effects.release();
        return count;
    }

    private EntityId createObject(short objectId) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new ObjectComponent(objectId, null), new Owner(PLAYER_ID, PLAYER_ID),
                new Position(0, new Vector3f()),
                new ObjectViewState(objectId, null, ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true));
        return entityId;
    }

    private static void runFor(DungeonHeartConstruction construction, float seconds) {
        for (int i = 0; i < Math.round(seconds / TICK); i++) {
            construction.processTick(TICK);
        }
    }

    private boolean isVisible(EntityId entityId) {
        return entityData.getComponent(entityId, ObjectViewState.class).visible;
    }

    private void assertPlugPresent() {
        assertNotNull(entityData.getComponent(plug, ObjectComponent.class));
    }

    private void assertPlugRemoved() {
        assertNull(entityData.getComponent(plug, ObjectComponent.class));
    }
}
