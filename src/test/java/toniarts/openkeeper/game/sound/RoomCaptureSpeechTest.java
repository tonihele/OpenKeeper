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
package toniarts.openkeeper.game.sound;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.event.ClientEvents;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.SoundFixtures;
import static org.junit.jupiter.api.Assertions.*;

class RoomCaptureSpeechTest {
    @Test
    void selectsRoomSpecificRecordingsAndGenericFallback() {
        assertEquals(142, RoomCaptureSpeech.forRoom(SoundFixtures.room("ROOM_LAIR")).getId());
        assertEquals(150, RoomCaptureSpeech.forRoom(SoundFixtures.room("ROOM_PORTAL")).getId());
        assertEquals(155, RoomCaptureSpeech.forRoom(SoundFixtures.room("ROOM_BRIDGE")).getId());
        assertEquals(156, RoomCaptureSpeech.forRoom(SoundFixtures.room("synthetic custom room")).getId());
        assertEquals(156, RoomCaptureSpeech.forRoom(null).getId());
        assertEquals(156, RoomCaptureSpeech.forRoom(SoundFixtures.room("ROOM_DUNGEON_HEART")).getId());
    }

    @Test
    void existingCallToArmsNarrationUsesTheSameQueue() {
        var queued = new ArrayList<Integer>();
        var player = new GameEventSoundPlayer(null, queued::add);
        player.newEvent(ClientEvents.GAME_EVENT,
                new GameEvent(GameEventType.CALL_TO_ARMS_UNREACHABLE, null, 0, null, 0, null, null));
        assertEquals(List.of((int) MentorType.MISC054.getId()), queued);
    }

    @Test
    void completedCaptureQueuesOneMentorLineThroughTheSpeechQueue() {
        IKwdFile data = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) ->
                        method.getName().equals("getRoomById") ? SoundFixtures.room("ROOM_PORTAL") : null);
        var queued = new ArrayList<Integer>();
        var player = new GameEventSoundPlayer(data, queued::add);
        player.newEvent(ClientEvents.GAME_EVENT,
                new GameEvent(GameEventType.ROOM_CAPTURED, GameEvent.ThingFlag.ROOM, 42, null, 0, null, null));
        assertEquals(List.of(150), queued);
    }
}
