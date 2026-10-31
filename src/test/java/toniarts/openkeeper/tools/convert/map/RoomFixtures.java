package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;
import java.util.List;

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

    public static Room roomWithObjects(short roomId, Short... objects) {
        Room room = room(roomId, 0);
        room.setObjects(List.of(objects));
        return room;
    }

}
