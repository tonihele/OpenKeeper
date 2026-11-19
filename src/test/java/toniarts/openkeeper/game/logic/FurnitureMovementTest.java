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

import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.*;

class FurnitureMovementTest {
    private float creatureWidth = 0.18f;
    private float movementTpf = 1 / 4f;
    private boolean endInObstacle;
    private boolean softFurniture;
    private boolean wallBelow;
    private float furnitureWidth = .6f;
    private float creatureHeight = 1f;
    private final java.util.Map<Short, GameObject> extraDefinitions = new java.util.HashMap<>();
    private java.util.function.IntConsumer onFrame = frame -> {};
    private final EntityData data = new DefaultEntityData();
    private final List<Vector2> furniture = new ArrayList<>();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void onlyExplicitObstacleDestinationsYieldOnTheFinalLeg(boolean solid) throws Exception {
        GameObject object = GameObjectFixtures.gameObject("Synthetic destination", GameObject.ObjectFlag.OBSTACLE);
        if (solid) {
            object.getFlags().add(GameObject.ObjectFlag.SOLID_OBSTACLE);
        }
        set(object, "width", .6f);
        Creature definition = new Creature();
        set(definition.getAttributes(), "width", .2f);
        set(definition, "unk3cc", .5f);
        IKwdFile level = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (p, m, args) -> m.getName().equals("getObject") ? object : definition);
        obstacle(5, 5);
        float[] positions = new float[3];
        for (int allow = 0; allow < 3; allow++) {
            EntityId id = data.createEntity();
            CreatureComponent creature = new CreatureComponent();
            Navigation navigation = new Navigation(new Point(5, 5), null,
                    List.of(new Vector2(4.8f, 5), new Vector2(5, 5)));
            navigation.endInObstacle = allow != 0;
            if (allow == 2) {
                navigation.navigationPath = List.of(new Vector2(4.8f, 5), new Vector2(4.8f, 10),
                        new Vector2(5, 10), new Vector2(5, 5));
            }
            data.setComponents(id, creature, new Mobile(false, false, false, 1),
                    new Position(0, new Vector3f(4.8f, 0, 5)), navigation);
            IMapInformation map = (IMapInformation) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{IMapInformation.class}, (p, m, args) -> false);
            MovementSystem movement = new MovementSystem(data, map, level);
            try {
                movement.processTick(.25f);
                positions[allow] = data.getComponent(id, Position.class).position.x;
            } finally {
                movement.stop();
                data.removeEntity(id);
            }
        }
        assertTrue(positions[0] < positions[1], "ordinary final legs must retain furniture contact");
        assertTrue(positions[2] < positions[1], "the destination flag must not exempt earlier legs");
    }

    @Test
    void crossesASingleObstacleHeadOn() throws Exception {
        obstacle(5, 5);
        trial("head-on", List.of(new Vector2(1, 5), new Vector2(9, 5)), null);
    }

    @Test
    void crossesADenselyFurnishedRoom() throws Exception {
        for (int x = 2; x <= 8; x++) {
            for (int y = 2; y <= 8; y++) {
                obstacle(x, y);
            }
        }
        trial("dense", List.of(new Vector2(1, 5), new Vector2(9, 5)), null);
    }

    @ParameterizedTest
    @ValueSource(floats = {0.14f, 0.32f, 0.44f, 0.46f})
    void differentCreatureSizesCrossDenseFurniture(float width) throws Exception {
        creatureWidth = width;
        crossesADenselyFurnishedRoom();
    }

    @Test
    void largeCreaturePassesANarrowGapWithoutStalling() throws Exception {
        creatureWidth = 0.44f;
        obstacle(5, 4);
        obstacle(5, 5);
        trial("narrow-gap", List.of(new Vector2(1, 4.5f), new Vector2(9, 4.5f)), null);
    }

    @Test
    void largeCreatureSlidesAroundAnIsolatedObstacle() throws Exception {
        creatureWidth = 0.44f;
        crossesASingleObstacleHeadOn();
    }

    @Test
    void largeCreatureAvoidsFurnitureBesideAWall() throws Exception {
        creatureWidth = 0.44f;
        wallBelow = true;
        crossesASingleObstacleHeadOn();
    }

    @Test
    void reachesAWorkingPositionInsideSoftFurniture() throws Exception {
        softFurniture = true;
        endInObstacle = true;
        obstacle(5, 5);
        trial("soft-work-position", List.of(new Vector2(1, 5), new Vector2(5, 5)), null);
    }

    @Test
    void reachesAResearchPositionAndFacesTheFurniture() throws Exception {
        endInObstacle = true;
        obstacle(5, 5);
        trial("research", List.of(new Vector2(5, 1), new Vector2(5, 4.5f)), new Point(5, 5));
    }

    @ParameterizedTest
    @ValueSource(floats = {.3f, .8f, 1.2f})
    void authoredFurnitureWidthsAllowProgress(float width) throws Exception {
        furnitureWidth = width;
        creatureWidth = .44f;
        crossesASingleObstacleHeadOn();
    }

    @ParameterizedTest
    @ValueSource(floats = {.5f, 1f, 2f})
    void visualHeightDoesNotEnlargeTheHorizontalFootprint(float height) throws Exception {
        creatureHeight = height;
        creatureWidth = .44f;
        largeCreaturePassesANarrowGapWithoutStalling();
    }

    @Test
    void mixedSolidAndSoftFurnitureBesideAWallAllowsArrival() throws Exception {
        creatureWidth = .44f;
        wallBelow = true;
        GameObject soft = GameObjectFixtures.gameObject("Synthetic room pillar", GameObject.ObjectFlag.OBSTACLE);
        set(soft, "width", .4f);
        extraDefinitions.put((short) 7, soft);
        EntityId pillar = data.createEntity();
        data.setComponents(pillar, new ObjectComponent((short) 7, null),
                new Position(0, new Vector3f(6, 1, 5.5f)));
        obstacle(4, 5);
        obstacle(7, 5);
        trial("mixed-wall", List.of(new Vector2(1, 5), new Vector2(9, 5)), null);
    }

    @Test
    void furnitureCanBeBuiltMovedRemovedAndRebuiltDuringTravel() throws Exception {
        EntityId furnitureId = data.createEntity();
        onFrame = frame -> {
            if (frame == 4 || frame == 12) {
                data.setComponents(furnitureId, new ObjectComponent((short) 64, null),
                        new Position(0, new Vector3f(5, 1, 5)));
            } else if (frame == 8) {
                data.removeComponent(furnitureId, Position.class);
            } else if (frame == 16) {
                data.setComponent(furnitureId, new Position(0, new Vector3f(20, 1, 20)));
            } else if (frame == 20) {
                data.removeEntity(furnitureId);
            }
        };
        trial("lifecycle", List.of(new Vector2(1, 5), new Vector2(9, 5)), null);
    }

    @ParameterizedTest
    @ValueSource(floats = {.25f, .125f})
    void largeCreatureCrossesFurnitureAtTheLogicTickRate(float tick) throws Exception {
        movementTpf = tick;
        creatureWidth = .44f;
        crossesADenselyFurnishedRoom();
    }

    @ParameterizedTest
    @ValueSource(floats = {.25f, .125f})
    void workPositionArrivalAndFacingCompleteAtTheLogicTickRate(float tick) throws Exception {
        movementTpf = tick;
        reachesAResearchPositionAndFacesTheFurniture();
    }

    private void obstacle(int x, int y) {
        EntityId id = data.createEntity();
        data.setComponents(id, new ObjectComponent((short) 64, null), new Position(0, new Vector3f(x, 1, y)));
        furniture.add(new Vector2(x, y));
    }

    private void trial(String label, List<Vector2> path, Point faceTarget) throws Exception {
        GameObject bookcase = GameObjectFixtures.gameObject("Synthetic furniture", GameObject.ObjectFlag.OBSTACLE, GameObject.ObjectFlag.SOLID_OBSTACLE);
        set(bookcase, "width", furnitureWidth);
        if (softFurniture) {
            bookcase.getFlags().remove(GameObject.ObjectFlag.SOLID_OBSTACLE);
        }
        Creature definition = new Creature();
        set(definition.getAttributes(), "width", creatureWidth);
        set(definition, "unk3cc", .5f);
        IKwdFile kwd = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (p, m, args) -> switch (m.getName()) {
                    case "getObject" -> extraDefinitions.getOrDefault(((Number) args[0]).shortValue(), bookcase);
                    case "getCreature" -> definition;
                    default -> throw new AssertionError(m.getName());
                });
        IMapInformation map = (IMapInformation) Proxy.newProxyInstance(IMapInformation.class.getClassLoader(), new Class<?>[]{IMapInformation.class},
                (p, m, args) -> false);
        EntityId id = data.createEntity();
        CreatureComponent creature = new CreatureComponent();
        creature.creatureId = 5;
        creature.height = creatureHeight;
        Vector2 start = path.getFirst();
        Vector2 goal = path.getLast();
        data.setComponents(id, creature, new Mobile(false, false, false, 1),
                new Position(0, new Vector3f(start.x, 1, start.y)),
                new Navigation(new Point(Math.round(goal.x), Math.round(goal.y)), faceTarget, path));
        data.getComponent(id, Navigation.class).endInObstacle = endInObstacle;
        MovementSystem movement;
        if (wallBelow) {
            var accessMap = accessMap();
            var controller = (ICreatureController) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{ICreatureController.class},
                    (p, m, args) -> {
                        var tile = (IMapTileInformation) args[1];
                        return tile.getY() < 5 ? null : 1f;
                    });
            var controllers = (ICreaturesController) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{ICreaturesController.class},
                    (p, m, args) -> controller);
            movement = new MovementSystem(data, accessMap, kwd, null, controllers);
        } else {
            movement = new MovementSystem(data, map, kwd);
        }
        float minClearance = Float.POSITIVE_INFINITY;
        Vector2 progressPosition = new Vector2(start);
        int frames = 0;
        for (; frames < Math.ceil(30 / movementTpf) && data.getComponent(id, Navigation.class) != null; frames++) {
            onFrame.accept(frames);
            movement.processTick(movementTpf);
            Vector3f pos = data.getComponent(id, Position.class).position;
            if (wallBelow) {
                assertTrue(Math.round(pos.z) >= 5, "Furniture steering entered the wall: " + pos);
            }
            for (Vector2 object : furniture) {
                minClearance = Math.min(minClearance, object.dst(pos.x, pos.z));
            }
            if (frames > 0 && frames % (int) Math.ceil(3 / movementTpf) == 0 && data.getComponent(id, Navigation.class) != null
                    && goal.dst(pos.x, pos.z) > .1f) {
                assertTrue(progressPosition.dst(pos.x, pos.z) > 0.02f,
                        "Stalled while navigating: " + pos);
                progressPosition.set(pos.x, pos.z);
            }
        }
        Position end = data.getComponent(id, Position.class);
        float distance = goal.dst(end.position.x, end.position.z);
        System.out.printf("%s frames=%d final=(%.4f,%.4f) distance=%.4f min-clearance=%.4f%n", label, frames, end.position.x, end.position.z, distance, minClearance);
        movement.stop();
        // Native large creatures squeeze through gaps narrower than their body,
        // overlapping the combined radii. Do not require rigid-circle clearance.
        // These synthetic routes must still go around the furniture's own disc,
        // but DK II finds the contacts once a tick, so a tick's walk (at 1 tile
        // a second here) can take a creature into it (0x4D31D0).
        if (!softFurniture) {
            assertTrue(minClearance >= bookcase.getWidth() * 0.5f - movementTpf,
                    "Passed through the furniture disc: " + minClearance);
        }
        assertNull(data.getComponent(id, Navigation.class), "Did not finish: " + end.position);
        assertTrue(distance < 0.2f, "Did not reach destination: " + distance);
        if (faceTarget != null) {
            assertTrue(Math.cos(end.rotation) > 0.98f, "Did not face the bookcase");
        }
    }

    private IMapController accessMap() {
        var mapData = Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapDataInformation.class}, (p, m, args) -> {
                    Point point = args[0] instanceof Point location ? location : new Point((int) args[0], (int) args[1]);
                    return Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[]{IMapTileController.class},
                            (tile, method, values) -> switch (method.getName()) {
                                case "getX" -> point.x;
                                case "getY" -> point.y;
                                case "getLocation" -> point;
                                default -> throw new AssertionError(method.getName());
                            });
                });
        return (IMapController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapController.class},
                (p, m, args) -> switch (m.getName()) {
                    case "getMapData" -> mapData;
                    case "isWater", "isLava" -> false;
                    default -> throw new AssertionError(m.getName());
                });
    }

    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
