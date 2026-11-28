package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;
import java.util.List;

/**
 * Builds {@link Room} fixtures for tests. Lives in {@code Room}'s own package
 * because its setters are {@code protected} (parser-only).
 */
public final class RoomFixtures {

    /** DK II's flags for the rooms that place floor furniture (Library, Casino, Prison etc.) */
    private static final Room.RoomFlag[] SPECIAL_TILES_ROOM_FLAGS = {Room.RoomFlag.PLACEABLE_ON_LAND,
        Room.RoomFlag.HAS_WALLS, Room.RoomFlag.SPECIAL_TILES, Room.RoomFlag.BUILDABLE,
        Room.RoomFlag.SPECIAL_WALLS, Room.RoomFlag.HAS_FLAME};

    private RoomFixtures() {
    }

    public static Room roomCreating(short createdCreatureId) {
        Room room = new Room();
        room.setCreatedCreatureId(createdCreatureId);
        return room;
    }

    public static Room room(short roomId, int cost, Room.RoomFlag... flags) {
        EnumSet<Room.RoomFlag> set = EnumSet.noneOf(Room.RoomFlag.class);
        for (Room.RoomFlag flag : flags) {
            set.add(flag);
        }

        Room room = new Room();
        room.setRoomId(roomId);
        room.setCost(cost);
        room.setFlags(set);
        return room;
    }

    public static Room room(String name, Room.TileConstruction tileConstruction) {
        Room room = new Room();
        room.setName(name);
        room.setTileConstruction(tileConstruction);
        room.setFlags(EnumSet.noneOf(Room.RoomFlag.class));
        return room;
    }

    public static Thing.Room roomThing(int initialHealth) {
        return roomThing(initialHealth, 0, 0);
    }

    public static Thing.Room roomThing(int initialHealth, int posX, int posY) {
        Thing.Room thing = new Thing.Room();
        thing.setInitialHealth(initialHealth);
        thing.setPosX(posX);
        thing.setPosY(posY);
        return thing;
    }

    public static Room roomWithObjects(short roomId, Short... objects) {
        Room room = room(roomId, 0, SPECIAL_TILES_ROOM_FLAGS);
        room.setObjects(List.of(objects));
        return room;
    }

    public static Room room(short roomId, List<Short> objects, Room.RoomFlag... flags) {
        Room room = room(roomId, 0, flags);
        room.getFlags().addAll(List.of(SPECIAL_TILES_ROOM_FLAGS));
        room.setObjects(objects);
        return room;
    }

    public static Room roomWithEffects(Integer... effects) {
        Room room = new Room();
        room.setEffects(List.of(effects));
        return room;
    }

}
