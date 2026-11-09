/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.MapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class RoomCaptureAnnouncementTest {
    @Test
    void threeByThreeRoomAnnouncesOnceOnlyWhenCaptureCompletes() throws Exception {
        for (short previousOwner : new short[]{Player.NEUTRAL_PLAYER_ID, Player.KEEPER2_ID}) {
            var entities = new DefaultEntityData();
            try {
                var floor = TerrainFixtures.terrain((short) 1, EnumSet.noneOf(Terrain.TerrainFlag.class), 0, 100, 100, (short) 1);
                var roomTerrain = TerrainFixtures.terrain((short) 2,
                        EnumSet.of(Terrain.TerrainFlag.OWNABLE, Terrain.TerrainFlag.ROOM), 0, 100, 100, (short) 2);
                var map = new GameMap(3, 3);
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 3; y++) {
                        map.getTiles()[x][y] = new Tile() {
                            @Override public short getTerrainId() { return 1; }
                            @Override public short getPlayerId() { return Player.NEUTRAL_PLAYER_ID; }
                        };
                    }
                }
                var data = proxy(IKwdFile.class, (name, args) -> switch (name) {
                    case "getMap" -> map;
                    case "getTerrain" -> (short) args[0] == 1 ? floor : roomTerrain;
                    default -> null;
                });
                var keepers = Map.of(Player.NEUTRAL_PLAYER_ID, new Keeper(false, Player.NEUTRAL_PLAYER_ID));
                var level = proxy(ILevelInfo.class, (name, args) -> keepers);
                var sent = new ArrayList<Object[]>();
                var events = proxy(GameEventService.class, (name, args) -> { sent.add(args); return null; });
                var roomType = new Room();
                roomType.setRoomId((short) 42);
                var instance = new RoomInstance(roomType);
                var room = proxy(IRoomController.class, (name, args) -> switch (name) {
                    case "getRoomInstance" -> instance;
                    case "getRoom" -> roomType;
                    case "getMaxHealth" -> 900;
                    default -> null;
                });
                var convert = Variable.MiscVariable.MiscType.CONVERT_ROOM_HEALTH;
                var attack = Variable.MiscVariable.MiscType.ATTACK_ROOM_HEALTH;
                var controller = new MapController(data, null,
                        Map.of(convert, CreatureSleepFixtures.variable(convert, 450),
                                attack, CreatureSleepFixtures.variable(attack, 450)), null, entities, level, events);
                var roomEntity = entities.createEntity();
                var registry = MapController.class.getDeclaredField("roomControllers");
                registry.setAccessible(true);
                @SuppressWarnings("unchecked")
                var rooms = (Map<com.simsilica.es.EntityId, IRoomController>) registry.get(controller);
                rooms.put(roomEntity, room);
                assertTrue(sent.isEmpty(), "Loading must not announce a capture");
                for (int x = 0; x < 3; x++) {
                    for (int y = 0; y < 3; y++) {
                        var point = new Point(x, y);
                        instance.addCoordinate(point);
                        var tile = controller.getMapData().getTile(point);
                        tile.setTerrainId((short) 2);
                        tile.setRoomId(roomEntity);
                        tile.setOwnerId(previousOwner);
                    }
                }
                controller.applyClaimTile(new Point(0, 0), Player.KEEPER1_ID, null);
                assertTrue(sent.isEmpty(), "Partial claiming must not announce success");
                controller.applyClaimTile(new Point(0, 0), Player.KEEPER1_ID, null);
                assertEquals(1, sent.size(), "One announcement per room, not per tile");
                assertArrayEquals(new Object[]{GameEventType.ROOM_CAPTURED, GameEvent.ThingFlag.ROOM, 42, Player.KEEPER1_ID}, sent.getFirst());
            } finally {
                entities.close();
            }
        }
    }

    private interface Handler { Object handle(String name, Object[] args); }
    private static <T> T proxy(Class<T> type, Handler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.handle(method.getName(), args)));
    }
}
