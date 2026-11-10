package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector2f;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.room;

class PartialRoomSelectionTest {

    private static final short PLAYER_ID = 3;
    private static final short ROOM_ID = 2;
    private static final int ROOM_COST = 100;

    private final Set<Point> buildableTiles = new HashSet<>();
    private final IKwdFile kwdFile = createKwdFile(room(ROOM_ID, ROOM_COST, Room.RoomFlag.BUILDABLE, Room.RoomFlag.PLACEABLE_ON_LAND));
    private final IMapInformation<?> mapInformation = createMapInformation(buildableTiles, 10, 10);

    @Test
    void buildsOnTheBuildableTilesOfTheSelection() {
        addBuildableTiles(0, 0, 2, 2);
        buildableTiles.remove(new Point(1, 1)); // Not claimed or not dug out

        RoomPlacementValidator.Result result = validate(Collections.emptySet(), 1000);

        assertTrue(result.isValid());
        assertEquals(8, result.plots().size());
        assertFalse(result.plots().contains(new Point(1, 1)));
    }

    @Test
    void onlyBuiltTilesCostGold() {
        addBuildableTiles(0, 0, 2, 2);
        buildableTiles.remove(new Point(1, 1));

        // 8 buildable tiles cost 800, the 9th tile of the selection is free
        assertTrue(validate(Collections.emptySet(), 800).isValid());
        assertEquals(RoomPlacementValidator.Failure.NOT_ENOUGH_GOLD, validate(Collections.emptySet(), 799).failure());
    }

    @Test
    void skipsTilesBlockedByEntities() {
        addBuildableTiles(0, 0, 2, 2);

        RoomPlacementValidator.Result result = validate(Set.of(new Point(2, 2)), 1000);

        assertTrue(result.isValid());
        assertEquals(8, result.plots().size());
        assertFalse(result.plots().contains(new Point(2, 2)));
    }

    @Test
    void selectionWithoutBuildableTilesIsInvalid() {
        assertEquals(RoomPlacementValidator.Failure.INVALID_TERRAIN, validate(Collections.emptySet(), 1000).failure());
    }

    @Test
    void selectionWithOnlyBlockedTilesIsBlocked() {
        buildableTiles.add(new Point(0, 0));

        RoomPlacementValidator.Result result = RoomPlacementValidator.validate(kwdFile, mapInformation,
                Set.of(new Point(0, 0)), new Vector2f(0, 0), new Vector2f(0, 0), PLAYER_ID, ROOM_ID, 1000);

        assertEquals(RoomPlacementValidator.Failure.BLOCKED_BY_ENTITY, result.failure());
    }

    private RoomPlacementValidator.Result validate(Set<Point> blockedTiles, int gold) {
        return RoomPlacementValidator.validate(kwdFile, mapInformation, blockedTiles,
                new Vector2f(0, 0), new Vector2f(2, 2), PLAYER_ID, ROOM_ID, gold);
    }

    private void addBuildableTiles(int x1, int y1, int x2, int y2) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                buildableTiles.add(new Point(x, y));
            }
        }
    }

    private static IKwdFile createKwdFile(Room room) {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getRoomById")) {
                        return ((Number) args[0]).shortValue() == room.getRoomId() ? room : null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static IMapInformation<?> createMapInformation(Set<Point> buildableTiles, int width, int height) {
        IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(),
                new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getWidth" -> width;
                    case "getHeight" -> height;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (IMapInformation<?>) Proxy.newProxyInstance(IMapInformation.class.getClassLoader(),
                new Class<?>[]{IMapInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getMapData" -> mapData;
                    case "isBuildable" -> buildableTiles.contains((Point) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
