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
import java.util.ArrayList;
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
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ResearchFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A creature that loses an experience level starts its experience towards the
 * next level from nothing and gets no level up fanfare (DKII.EXE 0x48AFA0)
 */
class CreatureLevelLossTest {

    private static final short CREATURE_ID = 8;

    private final EntityData entityData = new DefaultEntityData();
    private final List<Object> events = new ArrayList<>();

    @Test
    void losingALevelZeroesTheExperienceWithoutTheLevelUpEvent() {
        EntityId creature = createCreature(3);

        createController().levelUpCreature(creature, 2, 0);

        CreatureExperience experience = entityData.getComponent(creature, CreatureExperience.class);
        assertEquals(2, experience.level);
        assertEquals(0, experience.experience);
        assertEquals(List.of(), events);
    }

    @Test
    void gainingALevelStillRaisesTheLevelUpEvent() {
        EntityId creature = createCreature(3);

        createController().levelUpCreature(creature, 4, 0);

        assertEquals(List.of(GameEventType.CREATURE_LEVEL_UP), events);
    }

    private EntityId createCreature(int level) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = CREATURE_ID;
        entityData.setComponents(entityId, creatureComponent, new CreatureExperience(level, 70, 100, 0, 0), new Health(100, 100),
                new Gold(0, 0), new Senses(0, 0), new Threat(0), new CreatureMeleeAttack());
        return entityId;
    }

    private CreaturesController createController() {
        Creature creature = ResearchFixtures.researcher(CREATURE_ID, 0);
        GameLevel gameLevel = CreatureSleepFixtures.level(4);
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getCreature" -> creature;
                    case "getCreatureStats" -> ResearchFixtures.levelStats(100);
                    case "getGameLevel" -> gameLevel;
                    default -> method.getReturnType() == List.class ? List.of() : null;
                });
        GameEventService gameEventService = (GameEventService) Proxy.newProxyInstance(GameEventService.class.getClassLoader(),
                new Class<?>[]{GameEventService.class}, (proxy, method, args) -> {
                    events.add(args[0]);
                    return null;
                });
        IGameController gameController = (IGameController) Proxy.newProxyInstance(IGameController.class.getClassLoader(),
                new Class<?>[]{IGameController.class}, (proxy, method, args) -> method.getName().equals("getGameEventService") ? gameEventService : null);
        return new CreaturesController(kwdFile, entityData, Map.of(), null, gameController, null, null);
    }

}
