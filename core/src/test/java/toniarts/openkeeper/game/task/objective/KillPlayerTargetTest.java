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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeImporter;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Maps give heroes keepers to kill whose slots may be left empty, or that have
 * no Dungeon Heart, such as the heroes' own side, or who have been defeated
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

    @Test
    void keeperWithoutDungeonHeartIsNoTarget() {
        Keeper keeper = new Keeper();
        ILevelInfo level = level(PLAYING_KEEPER, keeper);

        assertFalse(KillPlayer.isTargetPresent(level, PLAYING_KEEPER));
    }

    @Test
    void defeatedKeeperIsNoTarget() throws IOException {

        // Loading a saved game is the way to get a defeated keeper here: every value is the default but "destroyed"
        InputCapsule capsule = (InputCapsule) Proxy.newProxyInstance(InputCapsule.class.getClassLoader(),
                new Class<?>[]{InputCapsule.class}, (proxy, method, args) -> "destroyed".equals(args[0]) ? Boolean.TRUE : args[args.length - 1]);
        JmeImporter importer = (JmeImporter) Proxy.newProxyInstance(JmeImporter.class.getClassLoader(),
                new Class<?>[]{JmeImporter.class}, (proxy, method, args) -> capsule);
        Keeper keeper = new Keeper();
        keeper.read(importer);
        ILevelInfo level = level(PLAYING_KEEPER, keeper);

        assertTrue(keeper.isDestroyed());
        assertFalse(KillPlayer.isTargetPresent(level, PLAYING_KEEPER));
    }

    private static ILevelInfo level(short keeperId) {
        Keeper keeper = new Keeper();
        keeper.setDungeonHeartLocation(new Point(10, 10));
        return level(keeperId, keeper);
    }

    private static ILevelInfo level(short keeperId, Keeper keeper) {
        Map<Short, Keeper> keepers = Map.of(keeperId, keeper);
        return (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("getPlayer")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return keepers.get((Short) args[0]);
                });
    }

}
