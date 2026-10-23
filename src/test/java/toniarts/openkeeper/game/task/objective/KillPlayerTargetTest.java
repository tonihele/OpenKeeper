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
package toniarts.openkeeper.game.task.objective;

import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Multiplayer maps give heroes keepers to kill whose slots may be left empty
 */
class KillPlayerTargetTest {

    private static final short PLAYING_KEEPER = 3;
    private static final short EMPTY_SLOT = 5;

    @Test
    void keeperInTheLevelIsATarget() {
        assertTrue(KillPlayer.isTargetPresent(level(PLAYING_KEEPER), PLAYING_KEEPER));
    }

    @Test
    void emptyKeeperSlotIsNoTarget() {
        assertFalse(KillPlayer.isTargetPresent(level(PLAYING_KEEPER), EMPTY_SLOT));
    }

    private static ILevelInfo level(short keeperId) {
        Map<Short, Keeper> keepers = Map.of(keeperId, new Keeper());
        return (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("getPlayer")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return keepers.get((Short) args[0]);
                });
    }

}
