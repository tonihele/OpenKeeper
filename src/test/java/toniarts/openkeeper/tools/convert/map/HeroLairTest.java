/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.tools.convert.map;

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ReturnToHeroLair;
import toniarts.openkeeper.game.controller.CreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.logic.HealthSystem;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hero flagged to return to the hero lair withdraws there from a fight below
 * a sixth of its health, then walks about its own land until its health is
 * back. A hero standing on the Hero Lair heals every turn (DKII.EXE 0x45EDA0,
 * 0x4611D0, 0x461B50, 0x488930). In the map package for the protected setters
 * of the level data.
 */
class HeroLairTest {

    private static final short KNIGHT = 13;
    private static final short HERO_LAIR_TERRAIN = 35;
    private static final short PATH_TERRAIN = 3;
    private static final Point HERO_TILE = new Point(20, 20);

    private final EntityData data = new DefaultEntityData();
    private final List<ActionPoint> actionPoints = new ArrayList<>();
    private final Set<Point> unreachable = new HashSet<>();
    private short landOwner = Player.GOOD_PLAYER_ID;

    @AfterEach
    void close() {
        data.close();
    }

    @Test
    void heroSetToReturnToTheHeroLairIsMarked() {
        CreaturesController creatures = creaturesController();

        EntityId flagged = creatures.spawnCreature(placedKnight(Thing.Creature.CreatureFlag.RETURN_TO_HERO_LAIR), new Vector2f(4, 4));
        EntityId other = creatures.spawnCreature(placedKnight(Thing.Creature.CreatureFlag.WILL_FIGHT), new Vector2f(4, 4));

        assertNotNull(data.getComponent(flagged, ReturnToHeroLair.class));
        assertNull(data.getComponent(other, ReturnToHeroLair.class));
    }

    @Test
    void heroBelowASixthOfItsHealthWithdrawsToTheFirstHeroLairItCanReach() {
        actionPoints.add(actionPoint(1, 5, 5, false));
        actionPoints.add(actionPoint(2, 8, 8, true));
        actionPoints.add(actionPoint(3, 30, 30, true));
        actionPoints.add(actionPoint(4, 33, 33, true));
        unreachable.add(new Point(8, 8));
        EntityId hero = hero(166);

        assertFalse(controller(hero).withdrawToHeroLair());
        assertNull(data.getComponent(hero, Navigation.class));

        data.setComponent(hero, new Health(165, 1000));
        assertTrue(controller(hero).withdrawToHeroLair());
        assertEquals(new Point(30, 30), data.getComponent(hero, Navigation.class).target);
    }

    @Test
    void heroFightsOnWithoutTheFlagWhenFearlessOrWithNoWayToTheLair() {
        actionPoints.add(actionPoint(1, 30, 30, true));
        EntityId hero = hero(100);

        data.removeComponent(hero, ReturnToHeroLair.class);
        assertFalse(controller(hero).withdrawToHeroLair());

        data.setComponent(hero, new ReturnToHeroLair());
        data.setComponent(hero, new Fearless(null));
        assertFalse(controller(hero).withdrawToHeroLair());

        data.removeComponent(hero, Fearless.class);
        unreachable.add(new Point(30, 30));
        assertFalse(controller(hero).withdrawToHeroLair());

        // A converted hero is no longer the heroes'
        unreachable.clear();
        data.setComponent(hero, new Owner(Player.KEEPER1_ID, Player.KEEPER1_ID));
        assertFalse(controller(hero).withdrawToHeroLair());
    }

    @Test
    void recuperatingHeroWandersTenTilesOffOnItsOwnLand() {
        EntityId hero = hero(100);

        controller(hero).wanderOnOwnLand();

        Point target = data.getComponent(hero, Navigation.class).target;
        assertEquals(10, Math.max(Math.abs(target.x - HERO_TILE.x), Math.abs(target.y - HERO_TILE.y)));
    }

    @Test
    void recuperatingHeroStaysPutWithNoOwnLandAround() {
        landOwner = Player.KEEPER1_ID;
        EntityId hero = hero(100);

        controller(hero).wanderOnOwnLand();

        assertNull(data.getComponent(hero, Navigation.class));
    }

    @Test
    void withdrawingHeroLeavesTheFightForTheLair() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.FIGHT);
        ICreatureController creature = creature(state, Map.of("withdrawToHeroLair", true), new ArrayList<>());

        CreatureState.FIGHT.update(creature);

        assertEquals(CreatureState.HERO_RECUPERATING, state.get());
    }

    @Test
    void recuperatingHeroGoesBackToItsJobOnceItsHealthIsBack() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.HERO_RECUPERATING);
        List<String> calls = new ArrayList<>();

        // On the way
        CreatureState.HERO_RECUPERATING.update(creature(state, Map.of("isStopped", false), calls));
        assertEquals(CreatureState.HERO_RECUPERATING, state.get());
        assertFalse(calls.contains("wanderOnOwnLand"));

        // Still hurt, on to another tile
        CreatureState.HERO_RECUPERATING.update(creature(state, Map.of("isStopped", true, "isFullHealth", false), calls));
        assertEquals(CreatureState.HERO_RECUPERATING, state.get());
        assertTrue(calls.contains("wanderOnOwnLand"));

        CreatureState.HERO_RECUPERATING.update(creature(state, Map.of("isStopped", true, "isFullHealth", true), calls));
        assertEquals(CreatureState.IDLE, state.get());
    }

    @Test
    void heroOnTheHeroLairHealsEveryTurn() {
        Terrain heroLair = TerrainFixtures.terrain(HERO_LAIR_TERRAIN, EnumSet.noneOf(Terrain.TerrainFlag.class), 0, 660, 660, HERO_LAIR_TERRAIN);
        heroLair.setDamage(65469);
        EntityId hero = creature(Player.GOOD_PLAYER_ID, 100);
        EntityId keeperCreature = creature(Player.KEEPER1_ID, 100);
        EntityId heroElsewhere = creature(Player.GOOD_PLAYER_ID, 100);
        EntityId healthyHero = creature(Player.GOOD_PLAYER_ID, 990);
        IEntityPositionLookup lookup = stub(IEntityPositionLookup.class, (method, args) -> method.equals("getEntityLocation")
                ? tile(args[0].equals(heroElsewhere) ? PATH_TERRAIN : HERO_LAIR_TERRAIN, Player.GOOD_PLAYER_ID) : null);
        IKwdFile kwd = stub(IKwdFile.class, (method, args) -> method.equals("getTerrain") && (Short) args[0] == HERO_LAIR_TERRAIN ? heroLair : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (method, args) -> method.equals("getLevelData") ? kwd : null);
        HealthSystem healthSystem = new HealthSystem(data, lookup, settings(), null, null, levelInfo, List.of(), null, () -> 0, null);

        healthSystem.processTick(0.25f);
        healthSystem.processTick(0.25f);

        assertEquals(234, data.getComponent(hero, Health.class).health);
        assertEquals(100, data.getComponent(keeperCreature, Health.class).health);
        assertEquals(100, data.getComponent(heroElsewhere, Health.class).health);
        assertEquals(1000, data.getComponent(healthyHero, Health.class).health);
    }

    private EntityId hero(int health) {
        EntityId hero = creature(Player.GOOD_PLAYER_ID, health);
        data.setComponents(hero, new ReturnToHeroLair(), new Position(0, WorldUtils.pointToVector3f(HERO_TILE)));
        return hero;
    }

    private EntityId creature(short ownerId, int health) {
        EntityId entity = data.createEntity();
        data.setComponents(entity, new CreatureComponent(), new Health(health, 1000), new Owner(ownerId, ownerId));
        return entity;
    }

    private static ActionPoint actionPoint(int id, int x, int y, boolean heroLair) {
        Thing.ActionPoint actionPoint = new Thing.ActionPoint();
        actionPoint.setId((short) id);
        actionPoint.setStartX(x);
        actionPoint.setStartY(y);
        actionPoint.setEndX(x + 2);
        actionPoint.setEndY(y + 2);
        actionPoint.setFlags(heroLair ? EnumSet.of(Thing.ActionPoint.ActionPointFlag.HERO_LAIR)
                : EnumSet.noneOf(Thing.ActionPoint.ActionPointFlag.class));
        return new ActionPoint(actionPoint);
    }

    private static Thing.GoodCreature placedKnight(Thing.Creature.CreatureFlag flag) {
        Thing.GoodCreature knight = new Thing.GoodCreature();
        knight.setCreatureId(KNIGHT);
        knight.setLevel((short) 1);
        knight.setInitialHealth(100);
        knight.setFlags(EnumSet.of(Thing.Creature.CreatureFlag.WILL_FIGHT, flag));
        return knight;
    }

    private CreaturesController creaturesController() {
        Creature knight = new Creature();
        knight.setCreatureId(KNIGHT);
        knight.setFlags(EnumSet.noneOf(Creature.CreatureFlag.class));
        knight.setMeleeAttackType(Creature.AttackType.MELEE_BLADE);
        knight.setSpells(List.of());
        IKwdFile level = stub(IKwdFile.class, (method, args) -> switch (method) {
            case "getThings" -> List.of();
            case "getCreature" -> knight;
            case "getImp" -> new Creature();
            default -> null;
        });
        return new CreaturesController(level, data, Map.of(), stub(IGameTimer.class, (method, args) -> null),
                stub(IGameController.class, (method, args) -> method.equals("getGameWorldController")
                ? stub(IGameWorldController.class, (m, a) -> null) : null),
                stub(IMapController.class, (method, args) -> null), stub(ILevelInfo.class, (method, args) -> null));
    }

    private CreatureController controller(EntityId entity) {
        INavigationService navigation = stub(INavigationService.class, (method, args) -> method.equals("findPath")
                && !unreachable.contains((Point) args[1]) ? new DefaultGraphPath<IMapTileInformation>() : null);
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (method, args) -> method.equals("getTile")
                ? tile(PATH_TERRAIN, landOwner) : null);
        IMapController mapController = stub(IMapController.class, (method, args) -> method.equals("getMapData") ? mapData : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (method, args) -> method.equals("getActionPoints") ? actionPoints : null);
        return new CreatureController(entity, data, CreatureSleepFixtures.sleeper(1), navigation, null, () -> 0,
                new EnumMap<>(MiscType.class), null, null, mapController, levelInfo, null, null, null);
    }

    private static ICreatureController creature(AtomicReference<CreatureState> state, Map<String, Boolean> answers, List<String> calls) {
        StateMachine<?, ?> stateMachine = stub(StateMachine.class, (method, args) -> {
            if (method.equals("changeState")) {
                state.set((CreatureState) args[0]);
            }
            return null;
        });
        AtomicReference<ICreatureController> self = new AtomicReference<>();
        self.set(stub(ICreatureController.class, (method, args) -> {
            calls.add(method);
            return switch (method) {
                case "getStateMachine" -> stateMachine;
                case "getAttackTarget" -> self.get();
                default -> answers.get(method);
            };
        }));
        return self.get();
    }

    private static IMapTileInformation tile(short terrainId, short ownerId) {
        return stub(IMapTileInformation.class, (method, args) -> switch (method) {
            case "getTerrainId" -> terrainId;
            case "getOwnerId" -> ownerId;
            default -> null;
        });
    }

    private static Map<MiscType, MiscVariable> settings() {
        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        for (MiscType type : List.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND)) {
            settings.put(type, CreatureSleepFixtures.variable(type, 0));
        }
        return settings;
    }

    private interface Calls {

        Object call(String method, Object[] args);
    }

    private static <T> T stub(Class<T> type, Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    Object result = switch (method.getName()) {
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> calls.call(method.getName(), args);
                    };
                    Class<?> returnType = method.getReturnType();

                    // Default primitives, e.g. a game time of 0
                    return result != null || !returnType.isPrimitive() || returnType == void.class
                            ? result : Array.get(Array.newInstance(returnType, 1), 0);
                }));
    }
}
