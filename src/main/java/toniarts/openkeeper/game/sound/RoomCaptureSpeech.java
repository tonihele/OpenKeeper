/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.sound;

import toniarts.openkeeper.tools.convert.map.Room;

/** Selects the existing, localized mentor recording for a completed room capture. */
final class RoomCaptureSpeech {
    private RoomCaptureSpeech() {
    }

    static MentorType forRoom(Room room) {
        if (room == null || room.getSoundCategory() == null) {
            return MentorType.ROOMA095;
        }
        return switch (room.getSoundCategory()) {
            case "ROOM_TREASURE" -> MentorType.ROOMA080;
            case "ROOM_LAIR" -> MentorType.ROOMA081;
            case "ROOM_HATCHERY" -> MentorType.ROOMA082;
            case "ROOM_TRAINING" -> MentorType.ROOMA083;
            case "ROOM_PIT" -> MentorType.ROOMA084;
            case "ROOM_TEMPLE" -> MentorType.ROOMA085;
            case "ROOM_WORKSHOP" -> MentorType.ROOMA086;
            case "ROOM_CASINO" -> MentorType.ROOMA087;
            case "ROOM_GRAVEYARD" -> MentorType.ROOMA088;
            case "ROOM_PORTAL" -> MentorType.ROOMA089;
            case "ROOM_TORTURE" -> MentorType.ROOMA090;
            case "ROOM_PRISON" -> MentorType.ROOMA091;
            case "ROOM_LIBRARY" -> MentorType.ROOMA092;
            case "ROOM_GUARD_POST" -> MentorType.ROOMA093;
            case "ROOM_BRIDGE" -> MentorType.ROOMA094;
            default -> MentorType.ROOMA095;
        };
    }
}
