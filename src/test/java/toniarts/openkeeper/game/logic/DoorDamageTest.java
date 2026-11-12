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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.EffectTrigger;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.door.DoorController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.tools.convert.map.DestroyedFixtures;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hits a creature gives to a door are damage like any other, the health
 * system takes the door down when its health is gone.
 */
class DoorDamageTest {

    private static final short KEEPER = 3;
    private static final short HERO = 1;
    private static final Point TILE = new Point(2, 2);

    private final EntityData entityData = new DefaultEntityData();
    private PositionSystem positionSystem;
    private HealthSystem healthSystem;
    private EntityId door;

    @BeforeEach
    void setUp() {
        FakeMapData mapData = new FakeMapData(5, 5);
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        door = entityData.createEntity();
        entityData.setComponents(door, new DoorComponent((short) 1, false, false), new Owner(KEEPER, KEEPER),
                new Position(0, WorldUtils.pointToVector3f(TILE)), new Health(2000, 2000));
        positionSystem = new PositionSystem(mapController, entityData, null, null, null);
        positionSystem.processTick(0.25f);

        IEntityPositionLookup lookup = (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(),
                new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getEntityController" -> new DoorController((EntityId) args[0], entityData, null, null, mapController);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        Door woodenDoor = DestroyedFixtures.door((short) 1, DestroyedFixtures.WOODEN_DOOR_EXPLOSION_ID);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> method.getName().equals("getDoorById") ? woodenDoor : null);
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getLevelData" -> kwdFile;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        healthSystem = new HealthSystem(entityData, lookup, Map.of(
                MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, TrapFiringFixtures.miscVariable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 10),
                MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, TrapFiringFixtures.miscVariable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, 0),
                MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, TrapFiringFixtures.miscVariable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 0)),
                null, levelInfo, List.of(), mapController, () -> 0.0, (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(), new Class<?>[]{GameEventService.class}, (proxy, method, args) -> null));
        healthSystem.processTick(0.25f);
    }

    @Test
    void aHitTakesItsDamageOffTheDoor() {
        EntityController.setDamage(entityData, door, 300);
        healthSystem.processTick(0.25f);

        assertEquals(1700, entityData.getComponent(door, Health.class).health);
    }

    @Test
    void theHitsAddUpUntilTheDoorBreaks() {
        for (int i = 0; i < 6; i++) {
            EntityController.setDamage(entityData, door, 300);
            healthSystem.processTick(0.25f);
        }
        assertEquals(200, entityData.getComponent(door, Health.class).health);

        EntityController.setDamage(entityData, door, 300);
        healthSystem.processTick(0.25f);

        assertNull(entityData.getComponent(door, DoorComponent.class));
        assertNull(entityData.getComponent(door, Health.class));
    }

    @Test
    void theBrokenDoorBlowsUpWithItsDeathEffect() {
        EntityController.setDamage(entityData, door, 2000);
        healthSystem.processTick(0.25f);

        EntitySet effects = entityData.getEntities(EffectTrigger.class, Position.class);
        assertEquals(1, effects.size());
        Entity effect = effects.iterator().next();
        assertEquals(DestroyedFixtures.WOODEN_DOOR_EXPLOSION_ID, effect.get(EffectTrigger.class).effectId);
        assertEquals(WorldUtils.pointToVector3f(TILE), effect.get(Position.class).position);
        effects.release();
    }

    @Test
    void theTileOpensWhenTheDoorIsGone() {
        assertTrue(positionSystem.isTileBlocked(TILE, HERO));

        EntityController.setDamage(entityData, door, 2000);
        healthSystem.processTick(0.25f);
        positionSystem.processTick(0.25f);

        assertFalse(positionSystem.isTileBlocked(TILE, HERO));
        assertNull(positionSystem.getBlockingDoor(TILE.x, TILE.y, HERO));
    }

    @Test
    void theDoorBlockingTheTileIsFoundForTheEnemyOnly() {
        assertSame(door, positionSystem.getBlockingDoor(TILE.x, TILE.y, HERO));
        assertNull(positionSystem.getBlockingDoor(TILE.x, TILE.y, KEEPER));
    }

}
