/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

final class DoorMovementFixtures {
    static final short PLAYER = 3;
    static final short ENEMY = 4;
    final EntityData data = new DefaultEntityData();
    final FakeMapData map = new FakeMapData(10, 10);
    final IMapController mapController;
    final PositionSystem positions;
    final PossessedMovementSystem possession;
    final ICreaturesController creatures;
    float creatureWidth = .25f;
    boolean canFly, canWalkOnWater, canWalkOnLava, roomAccessible = true;

    DoorMovementFixtures() {
        mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> map;
            case "getTerrain" -> terrain(((toniarts.openkeeper.game.map.IMapTileInformation) args[0]).getTerrainId());
            case "getRoomControllerByCoordinates" -> Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                    new Class<?>[]{IRoomController.class}, (p, m, a) -> switch (m.getName()) {
                case "isTileAccessible" -> roomAccessible;
                default -> throw new AssertionError(m.getName());
            });
            case "isWater" -> map.getTile((Point) args[0]).terrainId == 2;
            case "isLava" -> map.getTile((Point) args[0]).terrainId == 3;
            default -> throw new AssertionError(method.getName());
        });
        positions = new PositionSystem(mapController, data, null, null, null);
        creatures = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "createController" -> creatureController((EntityId) args[0]);
            default -> throw new AssertionError(method.getName());
        });
        possession = new PossessedMovementSystem(data, mapController, positions, creatures);
    }

    private static Terrain terrain(short id) {
        Terrain.TerrainFlag[] flags = switch (id) {
            case 1 -> new Terrain.TerrainFlag[]{Terrain.TerrainFlag.SOLID};
            case 2 -> new Terrain.TerrainFlag[]{Terrain.TerrainFlag.WATER};
            case 3 -> new Terrain.TerrainFlag[]{Terrain.TerrainFlag.LAVA};
            case 4 -> new Terrain.TerrainFlag[]{Terrain.TerrainFlag.ROOM};
            default -> new Terrain.TerrainFlag[]{};
        };
        return TerrainFixtures.terrain(id, TerrainFixtures.flags(flags), 0, 0, 0, (short) 0);
    }

    ICreatureController creatureController(EntityId id) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" -> definition();
            case "getOwnerId" -> data.getComponent(id, Owner.class).ownerId;
            case "canFly" -> canFly;
            case "canWalkOnWater" -> canWalkOnWater;
            case "canWalkOnLava" -> canWalkOnLava;
            case "canMoveDiagonally", "canBreakDoor" -> true;
            case "getCost" -> InvocationHandler.invokeDefault(proxy, method, args);
            default -> throw new AssertionError(method.getName());
        });
    }

    private toniarts.openkeeper.tools.convert.map.Creature definition() {
        var definition = new toniarts.openkeeper.tools.convert.map.Creature();
        try {
            var width = definition.getAttributes().getClass().getDeclaredField("width");
            width.setAccessible(true);
            width.setFloat(definition.getAttributes(), creatureWidth);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return definition;
    }

    EntityId door(short type, float rotation, boolean locked, boolean blueprint, boolean open) {
        EntityId id = data.createEntity();
        data.setComponents(id, new Position(rotation, new Vector3f(4, 0, 4)), new Owner(PLAYER, PLAYER),
                new DoorComponent(type, locked, blueprint), new DoorViewState(type, locked, blueprint, open));
        positions.processTick(0);
        return id;
    }

    void open(EntityId id, boolean open) {
        DoorComponent door = data.getComponent(id, DoorComponent.class);
        data.setComponent(id, new DoorViewState(door.doorId, door.locked, door.blueprint, open));
    }

    EntityId possessed(float x, float z, float height, Vector2f direction, float speed) {
        EntityId id = data.createEntity();
        CreatureComponent creature = new CreatureComponent();
        creature.height = height;
        data.setComponents(id, new Position(0, new Vector3f(x, 0, z)), new Owner(PLAYER, PLAYER),
                new Possessed(0, 0), new PossessedMovement(direction, 0.7f, speed), creature);
        positions.processTick(0);
        return id;
    }

    void move(EntityId id, Vector2f direction, float speed) {
        data.setComponent(id, new PossessedMovement(direction, 0.7f, speed));
    }

    void tick(float tpf) {
        possession.processTick(tpf);
        positions.processTick(tpf);
    }

    Vector3f position(EntityId id) {
        return data.getComponent(id, Position.class).position;
    }

    MovementSystem ai() {
        return new MovementSystem(data, mapController, null, positions, creatures);
    }

    EntityId walker(float x, float z, float speed, short owner, int targetX, int targetZ) {
        EntityId id = data.createEntity();
        data.setComponents(id, new Position(0, new Vector3f(x, 0, z)), new Owner(owner, owner),
                new Mobile(false, false, false, speed), new CreatureComponent(),
                new Navigation(new Point(targetX, targetZ), null,
                        List.of(new com.badlogic.gdx.math.Vector2(x, z), new com.badlogic.gdx.math.Vector2(targetX, targetZ))));
        positions.processTick(0);
        return id;
    }

    void tickAi(MovementSystem ai) {
        ai.processTick(.25f);
        positions.processTick(.25f);
    }
}
