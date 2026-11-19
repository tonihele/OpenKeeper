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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Frozen;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.component.SpeedChange;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapInformation;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How fast a possessed creature moves: the speed its keeper asks for, changed
 * by a haste, slow or slap while that runs, by its health and by the terrain.
 * A frozen one doesn't move.
 */
class PossessedMovementSystemTest {

    private static final short GROUND_ID = 1;
    private static final short WATER_ID = 2;
    private static final short LAVA_ID = 3;
    private static final float SPEED = 1f;
    private static final float TPF = 0.1f;
    private static final float DELTA = 0.0001f;

    private final EntityData entityData = new DefaultEntityData();
    private final FakeMapInformation mapInformation = new FakeMapInformation(new FakeMapData(8, 8), Map.of(
            GROUND_ID, TerrainFixtures.terrain(GROUND_ID, TerrainFixtures.flags(), 0, 0, 0, (short) 0),
            WATER_ID, TerrainFixtures.terrain(WATER_ID, TerrainFixtures.flags(Terrain.TerrainFlag.WATER), 0, 0, 0, (short) 0),
            LAVA_ID, TerrainFixtures.terrain(LAVA_ID, TerrainFixtures.flags(Terrain.TerrainFlag.LAVA), 0, 0, 0, (short) 0)));
    private final PossessedMovementSystem system;

    PossessedMovementSystemTest() {
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                mapInformation.getMapData().getTile(x, y).terrainId = GROUND_ID;
            }
        }
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getMapData" -> mapInformation.getMapData();
                    case "getTerrain" -> mapInformation.getTerrain((toniarts.openkeeper.game.map.IMapTileInformation) args[0]);
                    default -> null;
                });
        ICreatureController creatureController = (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> "getCost".equals(method.getName()) ? 1f : null);
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> creatureController);
        system = new PossessedMovementSystem(entityData, mapController, null, creaturesController);
    }

    @Test
    void healthyCreatureMovesAtItsSpeed() {
        EntityId creature = createPossessed(false, 100, false);

        system.processTick(TPF);

        assertEquals(SPEED * TPF, getDistanceMoved(creature), DELTA);
    }

    @Test
    void creatureBelowAFifthOfItsHealthMovesAtAFifthOfItsSpeed() {
        EntityId creature = createPossessed(false, 19, false);

        system.processTick(TPF);

        assertEquals(SPEED * TPF / 5, getDistanceMoved(creature), DELTA);
    }

    @Test
    void creatureAtAFifthOfItsHealthIsNotSlowed() {
        EntityId creature = createPossessed(false, 20, false);

        system.processTick(TPF);

        assertEquals(SPEED * TPF, getDistanceMoved(creature), DELTA);
    }

    @Test
    void hurtWorkerIsNotSlowed() {
        EntityId creature = createPossessed(true, 1, false);

        system.processTick(TPF);

        assertEquals(SPEED * TPF, getDistanceMoved(creature), DELTA);
    }

    @Test
    void waterHalvesTheSpeedAndLavaRaisesItByHalf() {
        mapInformation.getMapData().getTile(2, 2).terrainId = WATER_ID;
        EntityId wading = createPossessed(false, 100, false);

        system.processTick(TPF);

        assertEquals(SPEED * TPF / 2, getDistanceMoved(wading), DELTA);

        mapInformation.getMapData().getTile(2, 2).terrainId = LAVA_ID;
        entityData.setComponent(wading, new Position(0, new Vector3f(2, 0, 2)));

        system.processTick(TPF);

        assertEquals(SPEED * TPF * 1.5f, getDistanceMoved(wading), DELTA);
    }

    @Test
    void flyingCreatureIsNotSlowedByWater() {
        mapInformation.getMapData().getTile(2, 2).terrainId = WATER_ID;
        EntityId creature = createPossessed(false, 100, true);

        system.processTick(TPF);

        assertEquals(SPEED * TPF, getDistanceMoved(creature), DELTA);
    }

    @Test
    void movesFasterWhileItsSpeedIsChanged() {
        EntityId us = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.speed = 1;
        entityData.setComponents(us, creatureComponent, new Possessed(0, 0), new Position(0, new Vector3f(2, 0, 2)),
                new PossessedMovement(new Vector2f(0, 1), 0, 0, 2), new Mobile(false, false, false, 1.5f), new SpeedChange(100));

        // Hasted to 150 %
        system.processTick(0.1f);
        assertEquals(2.3f, entityData.getComponent(us, Position.class).position.z, DELTA);

        // The haste has run out
        entityData.removeComponent(us, SpeedChange.class);
        entityData.setComponent(us, new Mobile(false, false, false, 1));
        system.processTick(0.1f);
        assertEquals(2.5f, entityData.getComponent(us, Position.class).position.z, DELTA);
    }

    @Test
    void frozenCreatureNeitherMovesNorTurns() {
        EntityId creature = createPossessed(false, 100, false);
        entityData.setComponent(creature, new Frozen(0, 10));

        system.processTick(TPF);

        Position position = entityData.getComponent(creature, Position.class);
        assertEquals(0, getDistanceMoved(creature), DELTA);
        assertEquals(0, position.rotation, DELTA);
    }

    private EntityId createPossessed(boolean worker, int health, boolean canFly) {
        EntityId creature = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.worker = worker;
        entityData.setComponents(creature, creatureComponent, new Health(health, 100), new Mobile(canFly, false, false, SPEED),
                new Possessed(0, 0), new Position(0, new Vector3f(2, 0, 2)),
                new PossessedMovement(new Vector2f(1, 0), 1f, 0, SPEED));
        return creature;
    }

    private float getDistanceMoved(EntityId creature) {
        return entityData.getComponent(creature, Position.class).position.x - 2;
    }

}
