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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Boulder;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.GameObjectFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.tools.convert.map.VariableFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A boulder rolls the way it was sent, turns at what blocks it and slows down
 * as it wears out (DKII.EXE 0x4A1390, 0x4A0E30, 0x453C10). At the original
 * levels' speed of 4915 / 4096 tiles a second, 4 ticks a second and a speed
 * to health ratio of 50 a whole boulder rolls 3684 / 4096 tiles a tick.
 * It runs over the creatures it touches (0x4A0930)
 */
class BoulderSystemTest {

    private static final float TICK = 0.25f;
    private static final float DELTA = 0.0001f;
    private static final int HEALTH = 4000;
    private static final short PILLAR_ID = 76;
    private static final short CHICKEN_ID = 9;
    private static final short GOBLIN_ID = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final Set<Point> solidTiles = new HashSet<>();
    private final Map<Point, List<EntityId>> entitiesByTile = new HashMap<>();
    private final List<EntityId> knockedDown = new ArrayList<>();
    private final BoulderSystem system = createSystem();

    @Test
    void rollsTheWayItWasSent() {
        EntityId boulder = createBoulder(5, 5, 1, 0, HEALTH);

        system.processTick(TICK);

        Vector3f position = entityData.getComponent(boulder, Position.class).position;
        assertEquals(5 + 3684f / 4096, position.x, DELTA);
        assertEquals(5, position.z, DELTA);
        assertNull(entityData.getComponent(boulder, Damage.class));
    }

    @Test
    void rollsSlowerWhenWornOut() {
        EntityId boulder = createBoulder(5, 5, 0, -1, HEALTH / 2);

        system.processTick(TICK);

        assertEquals(5 - 3377f / 4096, entityData.getComponent(boulder, Position.class).position.z, DELTA);
    }

    @Test
    void turnsToTheOpenSideInTheMiddleOfTheTileAndLosesATenthOfItsHealth() {
        solidTiles.add(new Point(7, 5));
        solidTiles.add(new Point(6, 6));
        EntityId boulder = createBoulder(5, 5, 1, 0, HEALTH);

        system.processTick(TICK);
        system.processTick(TICK);

        // The turn ends the move
        Vector3f position = entityData.getComponent(boulder, Position.class).position;
        assertEquals(6, position.x, DELTA);
        assertEquals(5, position.z, DELTA);
        Boulder rolling = entityData.getComponent(boulder, Boulder.class);
        assertEquals(0, rolling.directionX, DELTA);
        assertEquals(-1, rolling.directionY, DELTA);
        assertEquals(HEALTH / 10, entityData.getComponent(boulder, Damage.class).damage);

        // Then it rolls on up the open side, a little slower
        system.processTick(TICK);

        assertEquals(5 - 3622f / 4096, entityData.getComponent(boulder, Position.class).position.z, DELTA);
    }

    @Test
    void turnsToTheSideItSetOffToWhenBothSidesAreOpen() {
        solidTiles.add(new Point(6, 5));
        EntityId boulder = entityData.createEntity();
        entityData.setComponents(boulder, new Boulder(1, 0, 1, 1), new Health(HEALTH, HEALTH), new Position(0, new Vector3f(5, 1, 5)));

        system.processTick(TICK);

        assertEquals(1, entityData.getComponent(boulder, Boulder.class).directionY, DELTA);
        assertEquals(5 + 3684f / 4096, entityData.getComponent(boulder, Position.class).position.z, DELTA);
    }

    @Test
    void turnsBackInADeadEnd() {
        solidTiles.add(new Point(6, 5));
        solidTiles.add(new Point(5, 4));
        solidTiles.add(new Point(5, 6));
        EntityId boulder = createBoulder(5, 5, 1, 0, HEALTH);

        system.processTick(TICK);

        assertEquals(-1, entityData.getComponent(boulder, Boulder.class).directionX, DELTA);
        assertEquals(5 - 3684f / 4096, entityData.getComponent(boulder, Position.class).position.x, DELTA);
    }

    @Test
    void theEdgeOfTheMapBlocksIt() {
        EntityId boulder = createBoulder(0, 5, -1, 0, HEALTH);
        solidTiles.add(new Point(0, 6));

        system.processTick(TICK);

        assertEquals(-1, entityData.getComponent(boulder, Boulder.class).directionY, DELTA);
    }

    @Test
    void objectsBlockItUnlessABoulderRollsThroughThem() {
        placeObject(PILLAR_ID, new Point(6, 5));
        placeObject(CHICKEN_ID, new Point(5, 4));
        solidTiles.add(new Point(5, 6));
        EntityId boulder = createBoulder(5, 5, 1, 0, HEALTH);

        system.processTick(TICK);

        assertEquals(-1, entityData.getComponent(boulder, Boulder.class).directionY, DELTA);
    }

    @Test
    void brokenBoulderStaysWhereItIs() {
        EntityId boulder = createBoulder(5, 5, 1, 0, HEALTH);
        entityData.setComponent(boulder, new Damage(HEALTH));

        system.processTick(TICK);

        assertEquals(5, entityData.getComponent(boulder, Position.class).position.x, DELTA);
    }

    @Test
    void runsOverTheCreatureItTouchesWhoeverOwnsItAndWearsByTheHealthItTakes() {
        EntityId boulder = createBoulder(5, 5, 1, 0, HEALTH);
        EntityId goblin = createCreature(5.6f, 5, new Health(200, 300));

        system.processTick(TICK);

        // Half the full health divided by 50 %, all of it
        assertEquals(300, entityData.getComponent(goblin, Damage.class).damage);
        assertEquals(200, entityData.getComponent(boulder, Damage.class).damage);
        assertEquals(List.of(goblin), knockedDown);
    }

    @Test
    void takesNoMoreHealthThanItHasLeft() {
        EntityId boulder = createBoulder(5, 5, 1, 0, 120);
        EntityId goblin = createCreature(5, 5.5f, new Health(300, 300));

        system.processTick(TICK);

        assertEquals(120, entityData.getComponent(goblin, Damage.class).damage);
        assertEquals(120, entityData.getComponent(boulder, Damage.class).damage);
    }

    @Test
    void leavesAloneTheCreaturesItDoesNotTouchOrThatAreDownAlready() {
        EntityId boulder = createBoulder(5, 5, 0, 1, HEALTH);

        // The boulder is a tile wide and the goblin half a tile, they touch within three quarters of a tile
        EntityId outOfReach = createCreature(5.8f, 5, new Health(300, 300));
        EntityId outCold = createCreature(5, 5, new Health(0, 300));
        entityData.setComponent(outCold, new Unconscious(0));
        EntityId down = createCreature(5, 5, new Health(300, 300));
        entityData.setComponent(down, new CreatureAi(0, CreatureState.FALLEN, GOBLIN_ID));

        system.processTick(TICK);

        assertNull(entityData.getComponent(outOfReach, Damage.class));
        assertNull(entityData.getComponent(outCold, Damage.class));
        assertNull(entityData.getComponent(down, Damage.class));
        assertNull(entityData.getComponent(boulder, Damage.class));
        assertTrue(knockedDown.isEmpty());
    }

    private EntityId createCreature(float x, float y, Health health) {
        EntityId creature = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = GOBLIN_ID;
        entityData.setComponents(creature, creatureComponent, health, new Position(0, new Vector3f(x, 1, y)));
        entitiesByTile.computeIfAbsent(new Point(Math.round(x), Math.round(y)), key -> new ArrayList<>()).add(creature);
        return creature;
    }

    private EntityId createBoulder(int x, int y, int directionX, int directionY, int health) {
        EntityId boulder = entityData.createEntity();
        entityData.setComponents(boulder, new Boulder(directionX, directionY, directionX, directionY), new Health(health, HEALTH),
                new Position(0, new Vector3f(x, 1, y)));
        return boulder;
    }

    private void placeObject(short objectId, Point tile) {
        EntityId object = entityData.createEntity();
        entityData.setComponent(object, new ObjectComponent(objectId, null));
        entitiesByTile.computeIfAbsent(tile, key -> new ArrayList<>()).add(object);
    }

    private BoulderSystem createSystem() {
        Creature goblin = ShotFixtures.creature(GOBLIN_ID, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
        ShotFixtures.setSize(goblin, 0.5f, 0.5f);
        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> switch (method) {
            case "getGameLevel" -> ShotFixtures.gameLevel(4);
            case "getCreature" -> goblin;
            case "getObject" -> switch ((int) args[0]) {
                case ObjectsController.OBJECT_BOULDER_ID ->
                    GameObjectFixtures.withWidth(GameObjectFixtures.gameObject("Boulder", GameObject.ObjectFlag.BOULDER_CAN_ROLL_THROUGH), 1);
                case CHICKEN_ID ->
                    GameObjectFixtures.gameObject("Chicken", GameObject.ObjectFlag.BOULDER_CAN_ROLL_THROUGH);
                default ->
                    GameObjectFixtures.gameObject("Pillar");
            };
            default -> null;
        });

        // A map of 20 x 20 tiles
        IMapTileController tile = proxy(IMapTileController.class, (method, args) -> null);
        IMapDataInformation<?> mapData = proxy(IMapDataInformation.class, (method, args) -> {
            Point p = (Point) args[0];
            return method.equals("getTile") && p.x >= 0 && p.y >= 0 && p.x < 20 && p.y < 20 ? tile : null;
        });
        IMapController mapController = proxy(IMapController.class, (method, args) -> switch (method) {
            case "getMapData" -> mapData;
            case "isSolid" -> solidTiles.contains((Point) args[0]);
            default -> null;
        });
        IEntityPositionLookup positionLookup = proxy(IEntityPositionLookup.class,
                (method, args) -> entitiesByTile.getOrDefault((Point) args[0], List.of()));

        ICreaturesController creaturesController = proxy(ICreaturesController.class,
                (method, args) -> proxy(ICreatureController.class, (controllerMethod, controllerArgs) -> {
                    if (controllerMethod.equals("knockDown")) {
                        knockedDown.add((EntityId) args[0]);
                    }
                    return null;
                }));

        return new BoulderSystem(entityData, kwdFile,
                Map.of(MiscType.BOULDER_SPEED_TILES_PER_SECOND, VariableFixtures.miscVariable(MiscType.BOULDER_SPEED_TILES_PER_SECOND, 4915),
                        MiscType.BOULDER_SPEED_TO_HEALTH_RATIO, VariableFixtures.miscVariable(MiscType.BOULDER_SPEED_TO_HEALTH_RATIO, 50),
                        MiscType.BOULDER_TO_CREATURE_DAMAGE_PERCENTAGE, VariableFixtures.miscVariable(MiscType.BOULDER_TO_CREATURE_DAMAGE_PERCENTAGE, 50)),
                mapController, positionLookup, creaturesController);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

    private interface Answer {

        Object answer(String method, Object[] args);
    }
}
