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
package toniarts.openkeeper.game.controller.creature;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.ResearchFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DK II gives a slapped researcher ticks per second × ticks per second ×
 * INCREASED_WORK_RATE_PECENTAGE_FROM_SLAPPING % of its points per tick extra,
 * on the first tick of every second
 */
class SlapResearchBonusTest {

    @Test
    void unslappedResearcherMakesItsPointsPerTick() {
        assertEquals(1, researchPerTick(5, false, 1.0));
    }

    @Test
    void slappedResearcherMakesTheBonusOnTheFirstTickOfASecond() {

        // A Warlock makes 1 a tick at 4 ticks a second, plus 4 × 4 × 50 % of it
        assertEquals(9, researchPerTick(5, true, 1.0));

        // And a Wizard 2 a tick
        assertEquals(18, researchPerTick(10, true, 1.0));
    }

    @Test
    void slappedResearcherMakesNoBonusOnTheOtherTicks() {
        assertEquals(1, researchPerTick(5, true, 0.25));
        assertEquals(1, researchPerTick(5, true, 0.5));
        assertEquals(1, researchPerTick(5, true, 0.75));
    }

    private static int researchPerTick(int researchPerSecond, boolean slapped, double time) {
        EntityData entityData = new DefaultEntityData();
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new CreatureExperience(1, 0, 0, 0, 0));
        if (slapped) {
            entityData.setComponent(entity, new Slapped(0));
        }

        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> switch (method) {
            case "getCreatureStats" -> null;
            case "getGameLevel" -> CreatureSleepFixtures.level(4);
            default -> throw new UnsupportedOperationException(method);
        });
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> switch (method) {
            case "getLevelData" -> kwdFile;
            default -> throw new UnsupportedOperationException(method);
        });
        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        settings.put(MiscType.INCREASED_WORK_RATE_PECENTAGE_FROM_SLAPPING,
                CreatureSleepFixtures.variable(MiscType.INCREASED_WORK_RATE_PECENTAGE_FROM_SLAPPING, 50));

        CreatureController controller = new CreatureController(entity, entityData, ResearchFixtures.researcher(researchPerSecond),
                null, null, () -> time, settings, null, null, null, levelInfo, null, null, null);
        return controller.getResearchPerTick();
    }

    @FunctionalInterface
    private interface Invocation {

        Object call(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (object, method, args) -> switch (method.getName()) {
                    case "toString" -> type.getSimpleName();
                    case "hashCode" -> System.identityHashCode(object);
                    case "equals" -> object == args[0];
                    default -> call.call(method.getName(), args);
                });
    }
}
