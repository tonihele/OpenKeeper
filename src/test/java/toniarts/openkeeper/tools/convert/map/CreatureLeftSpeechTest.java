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
package toniarts.openkeeper.tools.convert.map;

import java.lang.reflect.Proxy;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.sound.GameEventSounds;
import toniarts.openkeeper.game.sound.SoundCategory;

/**
 * The mentor's words when a creature leaves the dungeon. In the package of
 * {@link Creature} for its protected setters.
 */
class CreatureLeftSpeechTest {

    private static final short WARLOCK_ID = 5;
    private static final short MAIDEN_OF_THE_NEST_ID = 45;

    @Test
    void mentorNamesTheCreatureTypeThatLeft() {

        // The Warlock's entry in the 1.7 data, "A Warlock has left your Dungeon"
        List<GameEventSounds.Sound> sounds = getSounds(WARLOCK_ID, 1120);

        assertEquals(List.of(new GameEventSounds.Sound(SoundCategory.SPEECH_MENTOR, 1121, null)), sounds);
    }

    @Test
    void maidenOfTheNestLeavesWithoutAWord() {
        assertTrue(getSounds(MAIDEN_OF_THE_NEST_ID, 1540).isEmpty());
    }

    @Test
    void creatureWithoutTheSpeechLeavesWithoutAWord() {
        assertTrue(getSounds(WARLOCK_ID, 0).isEmpty());
    }

    private static List<GameEventSounds.Sound> getSounds(short creatureId, int leftSpeechId) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setAngerStringIdPay(leftSpeechId);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getCreature") ? creature : null);

        return GameEventSounds.getSounds(new GameEvent(GameEventType.CREATURE_LEFT, GameEvent.ThingFlag.CREATURE,
                creatureId, null, 0, null, null), kwdFile);
    }

}
