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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Frozen;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A knocked out creature dies when its dying time runs out, but like in DK II
 * the time doesn't run while an Imp carries it (DKII.EXE 0x4599D0)
 */
class HealthSystemDyingTest {

    private static final short CREATURE_ID = 14;
    private static final int DYING_SECONDS = 60;
    private static final float TICK = 0.25f;

    private final EntityData entityData = new DefaultEntityData();
    private final Keeper owner = new Keeper(false, (short) 3);
    private final Keeper enemy = new Keeper(false, (short) 4);
    private final PlayerStatsControl enemyStats = new PlayerStatsControl(enemy);
    private final List<String> calls = new ArrayList<>();
    private final HealthSystem healthSystem = createHealthSystem();
    private final EntityId creature = createKnockedOutCreature();

    @Test
    void knockedOutCreatureDiesWhenItsDyingTimeRunsOut() {
        runSeconds(DYING_SECONDS - 1);
        assertNull(entityData.getComponent(creature, Death.class));

        runSeconds(1);
        assertNotNull(entityData.getComponent(creature, Death.class));
    }

    @Test
    void carriedCreatureDoesNotLoseDyingTime() {
        runSeconds(30);
        entityData.setComponent(creature, new HauledBy(entityData.createEntity()));
        runSeconds(45);
        entityData.removeComponent(creature, HauledBy.class);

        runSeconds(DYING_SECONDS - 30 - 1);
        assertNull(entityData.getComponent(creature, Death.class));

        runSeconds(1);
        assertNotNull(entityData.getComponent(creature, Death.class));
    }

    @Test
    void deathCountsALossToItsOwnerAndAKillToTheKeeperThatKnockedItOut() {
        List<String> sent = new ArrayList<>();
        enemyStats.addListener((keeperId, enemyCreaturesKilled, heroesDestroyed, ownCreaturesKilled, researchCompleted)
                -> sent.add(keeperId + ":" + enemyCreaturesKilled + "," + heroesDestroyed + "," + ownCreaturesKilled + "," + researchCompleted));
        entityData.setComponent(creature, new Unconscious(0, enemy.getId()));
        runSeconds(DYING_SECONDS);

        // The clients get the new stats for the window
        assertEquals(List.of("4:1,0,0,0"), sent);

        // DK II's 0x487C70: CPlayer +0x146 for the killer, +0x173 for the owner
        assertEquals(1, enemy.getEnemyCreaturesKilled());
        assertEquals(0, enemy.getOwnCreaturesKilled());
        assertEquals(0, owner.getEnemyCreaturesKilled());
        assertEquals(1, owner.getOwnCreaturesKilled());
    }

    @Test
    void killGoesToTheKeeperOfACreatureFightingIt() {
        EntityId goblin = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = CREATURE_ID;
        creatureComponent.diesInstantly = true;
        entityData.setComponents(goblin, creatureComponent, new Owner(owner.getId(), owner.getId()), new Health(10, 100),
                new Damage(20));
        EntityId knight = entityData.createEntity();
        entityData.setComponents(knight, new AttackTarget(goblin), new Owner(enemy.getId(), enemy.getId()));

        healthSystem.processTick(TICK);

        // DK II falls back on the owner of a creature fighting it (creature +0x504, 0x487C70)
        assertNotNull(entityData.getComponent(goblin, Death.class));
        assertEquals(1, enemy.getEnemyCreaturesKilled());
        assertEquals(1, owner.getOwnCreaturesKilled());
    }

    @Test
    void ownKeepersKillCountsOnlyAsALoss() {
        entityData.setComponent(creature, new Unconscious(0, owner.getId()));
        runSeconds(DYING_SECONDS);

        assertEquals(0, owner.getEnemyCreaturesKilled());
        assertEquals(1, owner.getOwnCreaturesKilled());
    }

    void creatureThatDiesFrozenShattersAndLeavesNoBody() {
        entityData.setComponents(creature, new Frozen(0, DYING_SECONDS + 1), new Position(0, new Vector3f(3, 0, 4)));

        runSeconds(DYING_SECONDS);
        assertNull(entityData.getComponent(creature, Death.class));
        assertEquals(List.of("emitEffect", "remove"), calls);
    }

    private void runSeconds(int seconds) {
        for (int tick = 0; tick < seconds / TICK; tick++) {
            healthSystem.processTick(TICK);
        }
    }

    private EntityId createKnockedOutCreature() {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = CREATURE_ID;
        entityData.setComponents(entityId, creatureComponent, new Owner(owner.getId(), owner.getId()), new Health(0, 100),
                new Unconscious(0));
        return entityId;
    }

    private HealthSystem createHealthSystem() {
        Creature dwarf = ShotFixtures.creature(CREATURE_ID, EnumSet.of(Creature.CreatureFlag.GENERATE_DEAD_BODY),
                EnumSet.noneOf(Creature.CreatureFlag3.class));
        IKwdFile kwdFile = proxy(IKwdFile.class, method -> method.equals("getCreature") ? dwarf : null);
        ILevelInfo levelInfo = proxy(ILevelInfo.class, method -> method.equals("getLevelData") ? kwdFile : null);

        IEntityController controller = proxy(IEntityController.class, calls::add);
        GameEventService gameEventService = proxy(GameEventService.class,
                method -> method.equals("emitEffect") ? calls.add(method) : null);

        return new HealthSystem(entityData, proxy(IEntityPositionLookup.class, method -> method.equals("getEntityController") ? controller : null),
                Map.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, CreatureSleepFixtures.variable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, DYING_SECONDS),
                        MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, CreatureSleepFixtures.variable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, -3),
                        MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, CreatureSleepFixtures.variable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 10)),
                null, levelInfo, List.of(player(owner, new PlayerStatsControl(owner)), player(enemy, enemyStats)), null, () -> 0.0, gameEventService);
    }

    private static IPlayerController player(Keeper keeper, PlayerStatsControl statsControl) {
        return proxy(IPlayerController.class, method -> switch (method) {
            case "getKeeper" -> keeper;
            case "getStatsControl" -> statsControl;
            default -> null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName()));
    }

    private interface Answer {

        Object answer(String method);
    }
}
