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

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CastShot;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureSpell;
import toniarts.openkeeper.game.component.CreatureSpells;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IShotsController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.PossessionFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A possessed creature casts its spells as its keeper chooses them
 *
 * @author OpenKeeper
 */
class PossessedSpellTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;
    private static final short FIREBALL = 1;
    private static final short HEAL = 2;
    private static final short LIGHTNING = 3;
    private static final short SHOT = 5;
    private static final float RECHARGE = 2f;
    private static final float RANGE = 8f;

    private EntityData entityData;
    private Creature creature;
    private Map<Short, toniarts.openkeeper.tools.convert.map.CreatureSpell> levelSpells;
    private Map<Variable.CreatureStats.StatType, Variable.CreatureFirstPerson> firstPersonStats;
    private Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private List<Object> shotTargets;
    private Set<Point> walls;
    private double gameTime;
    private EntityId us;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        levelSpells = new HashMap<>();
        firstPersonStats = new HashMap<>();
        gameSettings = new HashMap<>();
        shotTargets = new ArrayList<>();
        walls = new HashSet<>();
        gameTime = 10;

        // A level 1 creature that gets Lightning only at level 4
        creature = new Creature();
        PossessionFixtures.setSpells(creature, List.of(PossessionFixtures.spell(FIREBALL, 1, 0.5f),
                PossessionFixtures.spell(HEAL, 1, 0), PossessionFixtures.spell(LIGHTNING, 4, 0)));
        levelSpells.put(FIREBALL, PossessionFixtures.creatureSpell(FIREBALL, SHOT, RECHARGE, RANGE,
                EnumSet.of(toniarts.openkeeper.tools.convert.map.CreatureSpell.CreatureSpellFlag.IS_ATTACKING)));
        levelSpells.put(HEAL, PossessionFixtures.creatureSpell(HEAL, SHOT, RECHARGE, RANGE,
                EnumSet.of(toniarts.openkeeper.tools.convert.map.CreatureSpell.CreatureSpellFlag.CREATURE)));
        levelSpells.put(LIGHTNING, PossessionFixtures.creatureSpell(LIGHTNING, SHOT, RECHARGE, RANGE,
                EnumSet.of(toniarts.openkeeper.tools.convert.map.CreatureSpell.CreatureSpellFlag.IS_ATTACKING)));

        // In the middle of tile (10, 2), facing +z
        us = createCreature(KEEPER, new Vector3f(10, 0, 2));
        entityData.setComponent(us, new Possessed(0, 0));
        entityData.setComponent(us, new CreatureSpells(List.of(createSpell(FIREBALL), createSpell(HEAL))));
    }

    @Test
    void castsAtTheCreatureItLooksAt() {
        EntityId enemy = createCreature(HERO, new Vector3f(10, 0, 7));

        cast(createController(), 1);

        assertEquals(List.of(enemy), shotTargets);
        assertEquals(gameTime, getSpell(FIREBALL).attactStartTime);
        assertEquals(Creature.AnimationType.CAST_SPELL, entityData.getComponent(us, CreatureViewState.class).state);
    }

    @Test
    void shotLeavesAtOnceWithoutTheShotDelay() {
        createCreature(HERO, new Vector3f(10, 0, 7));

        cast(createController(), 1);

        assertEquals(1, shotTargets.size());
        assertNull(entityData.getComponent(us, CastShot.class));
    }

    @Test
    void doesNotCastASpellAboveItsLevel() {
        createCreature(HERO, new Vector3f(10, 0, 7));

        cast(createController(), 3);

        assertTrue(shotTargets.isEmpty());
        assertNull(entityData.getComponent(us, CreatureViewState.class));
    }

    @Test
    void castsAgainOnlyOnceRechargedWithTheFirstPersonModifiers() {
        firstPersonStats.put(Variable.CreatureStats.StatType.MELEE_RECHARGE_TIME_SECONDS,
                PossessionFixtures.firstPersonStat(Variable.CreatureStats.StatType.MELEE_RECHARGE_TIME_SECONDS, 25));
        gameSettings.put(Variable.MiscVariable.MiscType.FIRST_PERSON_RECHARGE_MODIFY_PERCENTAGE,
                PossessionFixtures.miscVariable(Variable.MiscVariable.MiscType.FIRST_PERSON_RECHARGE_MODIFY_PERCENTAGE, 50));
        createCreature(HERO, new Vector3f(10, 0, 7));
        CreatureController controller = createController();

        // 2 seconds, 25 % slower and then halved
        cast(controller, 1);
        gameTime += 1.2;
        cast(controller, 1);
        assertEquals(1, shotTargets.size());

        gameTime += 0.05;
        cast(controller, 1);
        assertEquals(2, shotTargets.size());
        assertEquals(RECHARGE, getSpell(FIREBALL).rechargeTime);
    }

    @Test
    void aimsAtTheCreatureClosestToTheViewNotTheNearest() {
        createCreature(HERO, aside(3, 15));
        EntityId straightAhead = createCreature(HERO, new Vector3f(10, 0, 8));

        cast(createController(), 1);

        assertEquals(List.of(straightAhead), shotTargets);
    }

    @Test
    void shootsStraightAheadWhenNothingIsWithinTheAccuracy() {

        // The 45 degrees of the levels let through about 20 degrees off the view
        createCreature(HERO, aside(4, 25));
        gameSettings.put(Variable.MiscVariable.MiscType.FIRST_PERSON_ACCURACY_ANGLE_DEGREES,
                PossessionFixtures.miscVariable(Variable.MiscVariable.MiscType.FIRST_PERSON_ACCURACY_ANGLE_DEGREES, 45));

        cast(createController(), 1);

        Vector3f aim = (Vector3f) shotTargets.get(0);
        assertEquals(10, aim.x, 0.001f);
        assertTrue(aim.z > 2);
        assertEquals(gameTime, getSpell(FIREBALL).attactStartTime);
    }

    @Test
    void aimsWithinTheAccuracyOfTheLevel() {
        EntityId enemy = createCreature(HERO, aside(4, 25));
        gameSettings.put(Variable.MiscVariable.MiscType.FIRST_PERSON_ACCURACY_ANGLE_DEGREES,
                PossessionFixtures.miscVariable(Variable.MiscVariable.MiscType.FIRST_PERSON_ACCURACY_ANGLE_DEGREES, 90));

        cast(createController(), 1);

        assertEquals(List.of(enemy), shotTargets);
    }

    @Test
    void doesNotAimThroughAWall() {
        createCreature(HERO, new Vector3f(10, 0, 7));
        walls.add(new Point(10, 4));

        cast(createController(), 1);

        assertFalse(shotTargets.get(0) instanceof EntityId);
    }

    @Test
    void aimsPastAWallThatIsNotInTheWay() {
        EntityId enemy = createCreature(HERO, new Vector3f(10, 0, 7));
        walls.add(new Point(12, 4));

        cast(createController(), 1);

        assertEquals(List.of(enemy), shotTargets);
    }

    @Test
    void shotFliesInTheDirectionOfThePitch() {
        entityData.setComponent(us, new PossessedMovement(new Vector2f(), 0, 0.5f, 0));

        cast(createController(), 1);

        Vector3f aim = (Vector3f) shotTargets.get(0);
        assertTrue(aim.y > 0);
        assertEquals(10, aim.x, 0.001f);
        assertTrue(aim.z > 2);
    }

    @Test
    void doesNotAimAtACreatureBelowTheViewWhenLookingUp() {
        createCreature(HERO, new Vector3f(10, 0, 7));
        entityData.setComponent(us, new PossessedMovement(new Vector2f(), 0, 0.5f, 0));

        cast(createController(), 1);

        assertFalse(shotTargets.get(0) instanceof EntityId);
    }

    @Test
    void doesNotAimBeyondTheReach() {
        createCreature(HERO, new Vector3f(10, 0, 12));

        cast(createController(), 1);

        assertFalse(shotTargets.get(0) instanceof EntityId);
    }

    @Test
    void attackSpellsAimAtEnemiesAndCreatureSpellsAtOurOwn() {
        EntityId own = createCreature(KEEPER, new Vector3f(10, 0, 5));
        EntityId enemy = createCreature(HERO, new Vector3f(10, 0, 7));
        CreatureController controller = createController();

        cast(controller, 1);
        cast(controller, 2);

        assertEquals(List.of(enemy, own), shotTargets);
    }

    @Test
    void quickClickIsKeptUntilTheSpellIsCast() {
        CreatureController controller = createController();

        controller.setPossessedAttack(true, 1);
        controller.setPossessedAttack(false, 1);

        Possessed possessed = entityData.getComponent(us, Possessed.class);
        assertFalse(possessed.attackHeld);
        assertTrue(possessed.isAttackPending());
        assertEquals(1, possessed.attackSpell);
    }

    private void cast(CreatureController controller, int spell) {
        entityData.setComponent(us, new Possessed(0, 0, true, gameTime, spell));
        controller.executePossessedAttack();
    }

    /**
     * A spot in front of us, the given angle off our view
     */
    private static Vector3f aside(float distance, float degrees) {
        float angle = degrees * FastMath.DEG_TO_RAD;
        return new Vector3f(10 + distance * FastMath.sin(angle), 0, 2 + distance * FastMath.cos(angle));
    }

    private EntityId createSpell(short creatureSpellId) {
        EntityId spell = entityData.createEntity();
        entityData.setComponent(spell, new CreatureSpell(creatureSpellId, us, RECHARGE, RANGE));
        return spell;
    }

    private CreatureSpell getSpell(short creatureSpellId) {
        for (EntityId spell : entityData.getComponent(us, CreatureSpells.class).creatureSpells) {
            CreatureSpell creatureSpell = entityData.getComponent(spell, CreatureSpell.class);
            if (creatureSpell.creatureSpellId == creatureSpellId) {
                return creatureSpell;
            }
        }
        return null;
    }

    private EntityId createCreature(short ownerId, Vector3f position) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new CreatureComponent(), new Owner(ownerId, ownerId), new Health(1000, 1000),
                new Position(0, position), new CreatureMeleeAttack(0, 10, 1, 0.5f));
        return entity;
    }

    private List<EntityId> getEntitiesInLocation(IMapTileInformation tile) {
        List<EntityId> entities = new ArrayList<>();
        for (Entity entity : entityData.getEntities(Position.class)) {
            if (WorldUtils.vectorToPoint(entity.get(Position.class).position).equals(tile.getLocation())) {
                entities.add(entity.getId());
            }
        }
        return entities;
    }

    private IMapTileInformation getTile(int x, int y) {
        if (x < 0 || y < 0 || x > 19 || y > 19) {
            return null;
        }
        return stub(IMapTileInformation.class, (name, args) -> "getLocation".equals(name) ? new Point(x, y) : null);
    }

    private CreatureController createController() {
        IGameTimer gameTimer = stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? gameTime : null);
        IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class,
                (name, args) -> "getEntitiesInLocation".equals(name) ? getEntitiesInLocation((IMapTileInformation) args[0]) : null);
        ICreaturesController creaturesController = stub(ICreaturesController.class,
                (name, args) -> "isValidEntity".equals(name) ? entityData.getComponent((EntityId) args[0], CreatureComponent.class) != null : null);
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class,
                (name, args) -> "getTile".equals(name) ? getTile((Integer) args[0], (Integer) args[1]) : null);
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> mapData;
            case "isSolid" -> walls.contains((Point) args[0]);
            default -> null;
        });
        IShotsController shotsController = stub(IShotsController.class, (name, args) -> {
            if ("fireShot".equals(name)) {
                shotTargets.add(args[5]);
                return true;
            }
            return null;
        });
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> switch (name) {
            case "getCreatureFirstPersonStats" -> firstPersonStats;
            case "getCreatureSpellById" -> levelSpells.get((Short) args[0]);
            default -> null;
        });
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> name.equals("getLevelData") ? kwdFile : null);

        return new CreatureController(us, entityData, creature, null, null, gameTimer, gameSettings, creaturesController,
                positionLookup, mapController, levelInfo, null, shotsController,
                stub(GameEventService.class, (name, args) -> null));
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
