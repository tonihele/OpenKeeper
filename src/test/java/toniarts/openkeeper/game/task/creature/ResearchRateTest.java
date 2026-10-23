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
package toniarts.openkeeper.game.task.creature;

import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerResearchControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ResearchFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DK II adds a researcher's research points on every game tick, not once a
 * second
 */
class ResearchRateTest {

    private static final short DOOR_ID = 1;

    @Test
    void researchesOnEveryTick() {
        assertEquals(4 * 5 * 60, researchFor(5, 4, 60));
    }

    @Test
    void researchFollowsTheTickRate() {
        assertEquals(10 * 3 * 60, researchFor(3, 10, 60));
    }

    @Test
    void firstTickAlreadyResearches() {
        assertEquals(5, researchFor(5, 4, 0.25f));
    }

    private static int researchFor(int researchPerTick, int ticksPerSecond, float seconds) {
        ResearchableEntity door = new ResearchableEntity(DOOR_ID, ResearchableType.DOOR);
        Keeper keeper = new Keeper(false, (short) 3);
        keeper.setCurrentResearch(door);

        // Make the door take forever so that it is never completed
        PlayerResearchControl researchControl = new PlayerResearchControl(keeper, kwdFile(ResearchFixtures.door(DOOR_ID, Integer.MAX_VALUE)));
        Research research = new Research(null, null, new Point(0, 0), keeper.getId(), null, null, researchControl, null);
        ICreatureController creature = creature(researchPerTick);
        research.assign(creature, false);

        float tpf = 1.0f / ticksPerSecond;
        int ticks = Math.round(seconds * ticksPerSecond);
        for (int tick = 1; tick <= ticks; tick++) {
            research.executeTask(creature, tick * tpf);
        }
        return door.getResearch();
    }

    private static IKwdFile kwdFile(Door door) {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getDoorById" -> door;
                    default -> method.getReturnType() == List.class ? List.of() : null;
                });
    }

    private static ICreatureController creature(int researchPerTick) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getResearchPerSecond" -> researchPerTick;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
