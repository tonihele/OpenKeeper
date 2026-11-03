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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * In DK II a creature gains its fight experience on every tick of a fight with
 * another creature, so it can level up in the middle of the fight. Walking up
 * to the target or hitting a Dungeon Heart teaches it nothing
 */
class CreatureFightExperienceTest {

    private static final float TPF = 0.25f;
    private static final int TICKS_PER_SECOND = 4;
    private static final int TEN_SECONDS = 40;
    private static final short IMP = 1;
    private static final short KNIGHT = 5;
    private static final short GIANT = 15;
    private static final int IMP_EXPERIENCE_GAIN_PER_SECOND = 30;
    private static final int EXPERIENCE_PER_SECOND = 200;

    private final EntityData entityData = new DefaultEntityData();
    private CreatureExperienceSystem system;
    private int tick;
    private int levelUpTick;

    @BeforeEach
    void setUp() {
        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings = new EnumMap<>(Variable.MiscVariable.MiscType.class);
        gameSettings.put(Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND,
                CreatureSleepFixtures.variable(Variable.MiscVariable.MiscType.IMP_EXPERIENCE_GAIN_PER_SECOND, IMP_EXPERIENCE_GAIN_PER_SECOND));

        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getImp" -> ShotFixtures.creature(IMP, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
            case "getGameLevel" -> CreatureSleepFixtures.level(TICKS_PER_SECOND);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(), new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
            if (method.getName().equals("levelUpCreature")) {
                levelUpTick = tick;
                return null;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        system = new CreatureExperienceSystem(entityData, kwdFile, gameSettings, creaturesController);
    }

    @AfterEach
    void tearDown() {
        system.stop();
    }

    @Test
    void hittingADungeonHeartGainsNothing() {
        EntityId knight = fighter(KNIGHT, CreatureState.MELEE_ATTACK, entityData.createEntity());

        tenSeconds();

        assertEquals(0, experience(knight));
    }

    @Test
    void fightingACreatureGainsTheCreaturesOwnExperienceOnEveryTick() {
        EntityId knight = fighter(KNIGHT, CreatureState.MELEE_ATTACK, enemy());

        tenSeconds();

        assertEquals(TEN_SECONDS * EXPERIENCE_PER_SECOND / TICKS_PER_SECOND, experience(knight));
    }

    @Test
    void shootingACreatureCountsAsFighting() {
        EntityId knight = fighter(KNIGHT, CreatureState.CAST_SPELL, enemy());

        tenSeconds();

        assertEquals(TEN_SECONDS * EXPERIENCE_PER_SECOND / TICKS_PER_SECOND, experience(knight));
    }

    @Test
    void waitingForTheNextSwingCountsAsFighting() {
        EntityId knight = fighter(KNIGHT, CreatureState.FIGHT, enemy());

        tenSeconds();

        assertEquals(TEN_SECONDS * EXPERIENCE_PER_SECOND / TICKS_PER_SECOND, experience(knight));
    }

    @Test
    void walkingToTheTargetGainsNothing() {
        EntityId knight = fighter(KNIGHT, CreatureState.FIGHT, enemy());
        entityData.setComponent(knight, new Navigation());

        tenSeconds();

        assertEquals(0, experience(knight));
    }

    /**
     * A level 1 Giant needs 3000 points and learns 150 a second, 37 a tick at
     * four ticks a second. DK II levels it up after 82 ticks of fighting
     */
    @Test
    void giantLevelsUpInTheMiddleOfTheFight() {
        EntityId giant = fighter(GIANT, CreatureState.FIGHT, enemy());
        entityData.setComponent(giant, new CreatureExperience(1, 0, 3000, 150, 30));

        for (tick = 1; tick <= 120 && levelUpTick == 0; tick++) {
            system.processTick(TPF);
        }

        assertEquals(82, levelUpTick);
    }

    private void tenSeconds() {
        for (int i = 0; i < TEN_SECONDS; i++) {
            system.processTick(TPF);
        }
    }

    private EntityId enemy() {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new CreatureComponent());
        return entityId;
    }

    private EntityId fighter(short creatureId, CreatureState state, EntityId target) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponents(entityId,
                creatureComponent,
                new CreatureExperience(1, 0, 21600, EXPERIENCE_PER_SECOND, 40),
                new CreatureAi(0, state, creatureId),
                new AttackTarget(target));
        return entityId;
    }

    private int experience(EntityId entityId) {
        return entityData.getComponent(entityId, CreatureExperience.class).experience;
    }

}
