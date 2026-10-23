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

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.CombatCreatureFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreatureCombatTest {

    private static final short FIGHTER = 1;
    private static final short HORNY = 2;
    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;

    private EntityData entityData;
    private Map<Short, Creature> creatures;
    private Map<EntityId, Set<EntityId>> sensed;
    private EntityId us;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        creatures = new HashMap<>();
        creatures.put(FIGHTER, CombatCreatureFixtures.fighter(FIGHTER, Creature.FightStyle.BLOCKER));
        creatures.put(HORNY, CombatCreatureFixtures.fighter(HORNY, Creature.FightStyle.BLITZER, Creature.CreatureFlag.IS_HORNY));
        sensed = new HashMap<>();
        us = createCreature(KEEPER, FIGHTER, new Vector3f(5, 0, 4), 0);
    }

    @Test
    void enemyAttackingUsIsPreferredOverANearerOne() {
        EntityId nearer = createCreature(HERO, FIGHTER, new Vector3f(6, 0, 4), 0);
        EntityId attacker = createCreature(HERO, FIGHTER, new Vector3f(8, 0, 4), 0);
        entityData.setComponent(attacker, new AttackTarget(us));
        sensed.put(us, new LinkedHashSet<>(List.of(nearer, attacker)));

        assertEquals(attacker, createController(us).getAttackTarget().getEntityId());
    }

    @Test
    void enemyWithAFullSetOfAttackersIsNotChosen() {
        EntityId crowded = createCreature(HERO, FIGHTER, new Vector3f(6, 0, 4), 0);
        EntityId free = createCreature(HERO, FIGHTER, new Vector3f(8, 0, 4), 0);
        for (int i = 0; i < CombatRules.MAX_MELEE_ATTACKERS; i++) {
            entityData.setComponent(entityData.createEntity(), new AttackTarget(crowded));
        }
        sensed.put(us, new LinkedHashSet<>(List.of(crowded, free)));

        assertEquals(free, createController(us).getAttackTarget().getEntityId());
    }

    @Test
    void hitInTheBackGetsTheRearAttackBonus() {
        EntityId target = createCreature(HERO, FIGHTER, new Vector3f(5, 0, 5), 0);

        createController(us).executeAttack(target);

        assertEquals(151, entityData.getComponent(target, Damage.class).damage);
    }

    @Test
    void possessedTargetTakesHalfTheDamage() {
        EntityId target = createCreature(HERO, FIGHTER, new Vector3f(5, 0, 5), 0);
        entityData.setComponent(target, new Possessed(0, 0));

        createController(us).executeAttack(target);

        assertEquals(75, entityData.getComponent(target, Damage.class).damage);
    }

    @Test
    void hornedReaperTakesNoDamage() {
        EntityId target = createCreature(HERO, HORNY, new Vector3f(5, 0, 5), 0);

        createController(us).executeAttack(target);

        assertNull(entityData.getComponent(target, Damage.class));
    }

    private EntityId createCreature(short ownerId, short creatureId, Vector3f position, float rotation) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponents(entity, creatureComponent, new Owner(ownerId, ownerId), new Health(1000, 1000),
                new Position(rotation, position), new CreatureMeleeAttack(0, 100, 1, 1.5f));
        return entity;
    }

    private CreatureController createController(EntityId entity) {
        Map<Short, Keeper> keepers = Map.of(KEEPER, new Keeper(false, KEEPER), HERO, new Keeper(false, HERO));
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> "getCreature".equals(name) ? creatures.get((Short) args[0]) : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> switch (name) {
            case "getLevelData" -> kwdFile;
            case "getPlayer" -> keepers.get((Short) args[0]);
            default -> null;
        });
        IGameTimer gameTimer = stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? 10.0 : null);
        IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class,
                (name, args) -> "getSensedEntities".equals(name) ? sensed.getOrDefault((EntityId) args[0], Set.of()) : null);
        ICreaturesController creaturesController = stub(ICreaturesController.class, (name, args) -> switch (name) {
            case "isValidEntity" -> true;
            case "createController" -> createController((EntityId) args[0]);
            default -> null;
        });
        GraphPath<?> path = stub(GraphPath.class, (name, args) -> null);
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> "findPath".equals(name) ? path : null);
        CreatureComponent creatureComponent = entityData.getComponent(entity, CreatureComponent.class);

        return new CreatureController(entity, entityData, creatures.get(creatureComponent.creatureId), navigationService,
                null, gameTimer, Map.of(), creaturesController, positionLookup, null, levelInfo, null, null);
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method.getName(), args);
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }

}
