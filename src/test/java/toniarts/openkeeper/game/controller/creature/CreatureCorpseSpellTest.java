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
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CastShot;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureSpell;
import toniarts.openkeeper.game.component.CreatureSpells;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.CombatCreatureFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSpell.AlternativeShot;
import toniarts.openkeeper.tools.convert.map.CreatureSpell.CreatureSpellFlag;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A creature with a spell for dead bodies, the Vampire's Raise Dead, casts it
 * on a dead body it sees when it has nobody to fight (DKII.EXE 0x45CB00)
 */
class CreatureCorpseSpellTest {

    private static final short VAMPIRE = 8;
    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;
    private static final short RAISE_DEAD_SPELL_ID = 20;
    private static final short FIREBALL_SPELL_ID = 1;
    private static final short IMP_TELEPORT_SPELL_ID = 36;
    private static final float RECHARGE = 17f;
    private static final float RANGE = 3f;
    private static final double GAME_TIME = 100;

    private final Creature vampire = CombatCreatureFixtures.fighter(VAMPIRE, Creature.FightStyle.BLITZER);
    private final Shot raiseDead = ShotFixtures.raiseDead();
    private final Shot fireball = ShotFixtures.fireball();
    private EntityData entityData;
    private Set<EntityId> sensed;
    private EntityId us;
    private EntityId spell;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        sensed = new LinkedHashSet<>();
        us = createCreature(KEEPER, new Vector3f(5, 0, 5));
        sensed.remove(us);
        entityData.setComponents(us, new CreatureAi(0, CreatureState.IDLE, VAMPIRE), new Fearless(null));
        spell = entityData.createEntity();
        entityData.setComponent(spell, new CreatureSpell(RAISE_DEAD_SPELL_ID, us, RECHARGE, RANGE));
        entityData.setComponent(us, new CreatureSpells(List.of(spell)));
    }

    @Test
    void castsTheSpellOnTheNearestDeadBodyInRange() {
        createDeadBody(new Vector3f(5, 0, 7.5f));
        EntityId near = createDeadBody(new Vector3f(6.5f, 0, 5));
        createDeadBody(new Vector3f(5, 0, 8.5f));
        CreatureController controller = createController();

        assertTrue(controller.shouldFleeOrAttack());

        assertEquals(CreatureState.CAST_SPELL, controller.getStateMachine().getCurrentState());
        CastShot castShot = entityData.getComponent(us, CastShot.class);
        assertEquals(RAISE_DEAD_SPELL_ID, castShot.creatureSpellId);
        assertEquals(near, castShot.target);
        assertEquals(RANGE, castShot.range);
        assertEquals(GAME_TIME, entityData.getComponent(spell, CreatureSpell.class).attactStartTime);
    }

    @Test
    void leavesADeadBodyOutOfTheSpellsRange() {
        createDeadBody(new Vector3f(5, 0, 8.5f));

        assertFalse(createController().shouldFleeOrAttack());

        assertNull(entityData.getComponent(us, CastShot.class));
    }

    @Test
    void leavesTheLivingAlone() {
        createCreature(KEEPER, new Vector3f(6, 0, 5));

        assertFalse(createController().shouldFleeOrAttack());
    }

    @Test
    void waitsForTheSpellToRecharge() {
        createDeadBody(new Vector3f(6, 0, 5));
        entityData.setComponent(spell, new CreatureSpell(entityData.getComponent(spell, CreatureSpell.class), GAME_TIME - RECHARGE + 1));

        assertFalse(createController().shouldFleeOrAttack());
    }

    @Test
    void anAttackSpellIsNotForDeadBodies() {
        createDeadBody(new Vector3f(6, 0, 5));
        entityData.setComponent(spell, new CreatureSpell(FIREBALL_SPELL_ID, us, RECHARGE, RANGE));

        assertFalse(createController().shouldFleeOrAttack());
    }

    /**
     * The Imp's Teleport is a defensive spell with no shot
     */
    @Test
    void aDefensiveSpellWithNoShotIsNotForDeadBodies() {
        createDeadBody(new Vector3f(6, 0, 5));
        entityData.setComponent(spell, new CreatureSpell(IMP_TELEPORT_SPELL_ID, us, RECHARGE, RANGE));

        assertFalse(createController().shouldFleeOrAttack());
    }

    @Test
    void fightsAnEnemyFirst() {
        createDeadBody(new Vector3f(6, 0, 5));
        createCreature(HERO, new Vector3f(5, 0, 7));
        CreatureController controller = createController();

        assertTrue(controller.shouldFleeOrAttack());

        assertEquals(CreatureState.FIGHT, controller.getStateMachine().getCurrentState());
        assertNull(entityData.getComponent(us, CastShot.class));
    }

    private EntityId createCreature(short ownerId, Vector3f position) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = VAMPIRE;
        entityData.setComponents(entity, creatureComponent, new Owner(ownerId, ownerId), new Health(1000, 1000),
                new Position(0, position), new CreatureMeleeAttack(0, 100, 1, 1.5f));
        sensed.add(entity);
        return entity;
    }

    private EntityId createDeadBody(Vector3f position) {
        EntityId entity = createCreature(HERO, position);
        entityData.removeComponent(entity, Health.class);
        entityData.setComponent(entity, new Death(GAME_TIME));
        return entity;
    }

    private CreatureController createController() {
        Map<Short, Keeper> keepers = Map.of(KEEPER, new Keeper(false, KEEPER), HERO, new Keeper(false, HERO));
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> switch (name) {
            case "getCreature" -> vampire;
            case "getCreatureSpellById" -> switch ((Short) args[0]) {
                case RAISE_DEAD_SPELL_ID -> ShotFixtures.creatureSpell(RAISE_DEAD_SPELL_ID, EnumSet.of(CreatureSpellFlag.IS_DEFENSIVE, CreatureSpellFlag.CAST_ON_OTHERS),
                        AlternativeShot.NONE, ShotFixtures.RAISE_DEAD_ID, 9);
                case IMP_TELEPORT_SPELL_ID -> ShotFixtures.creatureSpell(IMP_TELEPORT_SPELL_ID, EnumSet.of(CreatureSpellFlag.IS_DEFENSIVE,
                        CreatureSpellFlag.IMP_TELEPORT, CreatureSpellFlag.CAST_ON_OTHERS), AlternativeShot.NONE, (short) 0, 0);
                default -> ShotFixtures.creatureSpell(FIREBALL_SPELL_ID, EnumSet.of(CreatureSpellFlag.IS_ATTACKING, CreatureSpellFlag.CAST_ON_OTHERS),
                        AlternativeShot.NONE, ShotFixtures.FIREBALL_ID, 100);
            };
            case "getShotById" -> switch ((Short) args[0]) {
                case 0 -> null;
                case ShotFixtures.RAISE_DEAD_ID -> raiseDead;
                default -> fireball;
            };
            default -> null;
        });
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> switch (name) {
            case "getLevelData" -> kwdFile;
            case "getPlayer" -> keepers.get((Short) args[0]);
            default -> null;
        });
        IGameTimer gameTimer = stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? GAME_TIME : null);
        IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class,
                (name, args) -> "getSensedEntities".equals(name) ? sensed : null);
        ICreaturesController creaturesController = stub(ICreaturesController.class, (name, args) -> switch (name) {
            case "isValidEntity" -> true;
            case "createController" -> stub(ICreatureController.class, (n, a) -> "getEntityId".equals(n) ? args[0] : null);
            default -> null;
        });
        GraphPath<?> path = stub(GraphPath.class, (name, args) -> null);
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> "findPath".equals(name) ? path : null);

        return new CreatureController(us, entityData, vampire, navigationService, null, gameTimer, Map.of(), creaturesController, positionLookup, null,
                levelInfo, null, null, stub(toniarts.openkeeper.game.event.GameEventService.class, (name, args) -> null));
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
