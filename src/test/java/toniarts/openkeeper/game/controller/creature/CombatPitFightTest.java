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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.CombatPitFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CombatPitFightTest {

    private static final short FIGHTER = 1;
    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER1_ID + 1;
    private static final int MAX_PIT_LEVEL = 8;

    private EntityData entityData;
    private Map<Short, Creature> creatures;
    private Map<MiscType, MiscVariable> gameSettings;
    private Map<EntityId, Set<EntityId>> sensed;
    private EntityId pit;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        creatures = Map.of(FIGHTER, CombatPitFixtures.pitCreature(FIGHTER));
        gameSettings = new HashMap<>();
        gameSettings.put(MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL, CombatPitFixtures.pitVariable(MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL, MAX_PIT_LEVEL));
        gameSettings.put(MiscType.COMBAT_PIT_MELEE_DAMAGE_MODIFIER_PERCENTAGE, CombatPitFixtures.pitVariable(MiscType.COMBAT_PIT_MELEE_DAMAGE_MODIFIER_PERCENTAGE, 100));
        gameSettings.put(MiscType.PIT_PERCENTAGE_DAMAGE_TAKEN_OF_NORMAL_COMBAT, CombatPitFixtures.pitVariable(MiscType.PIT_PERCENTAGE_DAMAGE_TAKEN_OF_NORMAL_COMBAT, 20));
        sensed = new HashMap<>();
        pit = entityData.createEntity();
    }

    @Test
    void pitFighterGoesForAnEnemyFighterFirst() {
        EntityId us = createFighter(KEEPER, 1, 5, pit);
        createFighter(KEEPER, 1, 6, pit);
        EntityId enemy = createFighter(ENEMY, 1, 8, pit);

        assertEquals(enemy, createController(us).getAttackTarget().getEntityId());
    }

    @Test
    void pitFighterFightsItsOwnSideWhenThereIsNoEnemy() {
        EntityId us = createFighter(KEEPER, 1, 5, pit);
        EntityId friend = createFighter(KEEPER, 1, 7, pit);

        assertEquals(friend, createController(us).getAttackTarget().getEntityId());
    }

    @Test
    void pitFighterLeavesCreaturesOutsideThePitAlone() {
        EntityId us = createFighter(KEEPER, 1, 5, pit);
        EntityId outsider = createFighter(ENEMY, 1, 6, null);
        sensed.put(us, Set.of(outsider));

        assertNull(createController(us).getAttackTarget());
    }

    @Test
    void creatureOutsideThePitLeavesPitFightersAlone() {
        EntityId us = createFighter(ENEMY, 1, 5, null);
        EntityId fighter = createFighter(KEEPER, 1, 6, pit);
        sensed.put(us, Set.of(fighter));

        assertNull(createController(us).getAttackTarget());
    }

    @Test
    void fighterAtTheTopPitLevelIsNotFought() {
        EntityId us = createFighter(KEEPER, 1, 5, pit);
        createFighter(ENEMY, MAX_PIT_LEVEL, 6, pit);

        assertNull(createController(us).getAttackTarget());
    }

    @Test
    void fighterAtTheTopPitLevelDoesNotFight() {
        EntityId us = createFighter(KEEPER, MAX_PIT_LEVEL, 5, pit);
        createFighter(ENEMY, 1, 6, pit);

        assertNull(createController(us).getAttackTarget());
    }

    @Test
    void pitFightersDealDoubleAndTakeAFifthOfTheDamage() {
        EntityId us = createFighter(KEEPER, 1, 5, pit);
        EntityId other = createFighter(KEEPER, 1, 6, pit);

        // Face to face, so no bonus for hitting from the side or back
        entityData.setComponent(other, new Position(FastMath.PI, entityData.getComponent(other, Position.class).position));

        createController(us).executeAttack(other);

        assertEquals(40, entityData.getComponent(other, Damage.class).damage);
    }

    private EntityId createFighter(short ownerId, int level, int z, EntityId pitId) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = FIGHTER;
        entityData.setComponents(entity, creatureComponent, new Owner(ownerId, ownerId), new Health(1000, 1000),
                new Position(0, new Vector3f(5, 0, z)), new CreatureMeleeAttack(0, 100, 1, 1.5f),
                new CreatureExperience(level, 0, 1000, 10, 0));
        if (pitId != null) {
            entityData.setComponent(entity, new CombatPitFighter(pitId));
        }
        return entity;
    }

    private CreatureController createController(EntityId entity) {
        Map<Short, Keeper> keepers = Map.of(KEEPER, new Keeper(false, KEEPER), ENEMY, new Keeper(false, ENEMY));
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
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> "findPath".equals(name) ? new DefaultGraphPath<>() : null);

        return new CreatureController(entity, entityData, creatures.get(FIGHTER), navigationService,
                null, gameTimer, gameSettings, creaturesController, positionLookup, null, levelInfo, null, null);
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
