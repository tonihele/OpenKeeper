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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Senses;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ResearchFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DK II turns the research per second of the creature data into research per
 * tick when the level loads: divide by the ticks per second (at least 1 if
 * there is anything to divide) and only then apply the level percentage
 */
class CreatureResearchPerTickTest {

    private static final short CREATURE_ID = 3;

    @Test
    void researchPerSecondIsDividedByTheTicksPerSecond() {
        assertEquals(1, researchPerTick(5, 4, 100)); // Warlock
        assertEquals(2, researchPerTick(10, 4, 100)); // Wizard
        assertEquals(4, researchPerTick(18, 4, 100)); // Unique Warlock
        assertEquals(1, researchPerTick(10, 10, 100));
    }

    @Test
    void anythingToDivideKeepsAtLeastOnePoint() {
        assertEquals(1, researchPerTick(1, 4, 100)); // Skeleton
        assertEquals(1, researchPerTick(3, 4, 100)); // Monk
    }

    @Test
    void nothingToResearchStaysZero() {
        assertEquals(0, researchPerTick(0, 4, 100));
    }

    @Test
    void levelPercentageIsAppliedToThePerTickValue() {

        // 3 a second at 4 ticks a second is 1 a tick, so 125% of it is 1.
        // Scaling the per second value first would give 3 * 125 / 100 / 4 = 0
        assertEquals(1, researchPerTick(3, 4, 125));
        assertEquals(2, researchPerTick(3, 4, 200));
        assertEquals(7, researchPerTick(5, 4, 700));
    }

    private static int researchPerTick(int researchPerSecond, int ticksPerSecond, int levelPercent) {
        EntityData entityData = new DefaultEntityData();
        Creature creature = ResearchFixtures.researcher(CREATURE_ID, researchPerSecond);
        GameLevel gameLevel = CreatureSleepFixtures.level(ticksPerSecond);
        Map<Variable.CreatureStats.StatType, Variable.CreatureStats> stats = ResearchFixtures.levelStats(levelPercent);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getCreature" -> creature;
                    case "getCreatureStats" -> stats;
                    case "getGameLevel" -> gameLevel;
                    default -> method.getReturnType() == List.class ? List.of() : null;
                });
        IGameController gameController = (IGameController) Proxy.newProxyInstance(IGameController.class.getClassLoader(),
                new Class<?>[]{IGameController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getGameEventService" -> Proxy.newProxyInstance(GameEventService.class.getClassLoader(),
                            new Class<?>[]{GameEventService.class}, (p, m, a) -> null);
                    default -> null;
                });
        CreaturesController controller = new CreaturesController(kwdFile, entityData, Map.of(), null, gameController, null, null);

        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = CREATURE_ID;
        entityData.setComponents(entityId, creatureComponent, new CreatureExperience(), new Health(100, 100),
                new Gold(0, 0), new Senses(0, 0), new Threat(0), new CreatureMeleeAttack());

        controller.levelUpCreature(entityId, 1, 0);

        return entityData.getComponent(entityId, CreatureComponent.class).researchPerTick;
    }

}
