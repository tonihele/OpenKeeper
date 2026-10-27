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

}
