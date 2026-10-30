package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;

/**
 * Builds {@link Room} fixtures for tests. Lives in {@code Room}'s own package
 * because its setters are {@code protected} (parser-only).
 */
public final class RoomFixtures {

    private RoomFixtures() {
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

}
