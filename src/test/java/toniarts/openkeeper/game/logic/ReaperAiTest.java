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

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
class ReaperAiTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final short ALLY_ID = 5;
    private static final Point REAPER_TILE = new Point(10, 10);

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Keeper> keepers = new HashMap<>();
    private final Set<Point> unreachableTiles = new HashSet<>();
    private final EntityId reaper = entityData.createEntity();
    private CreatureState state = CreatureState.IDLE;
    private int roll = 1;
    private boolean wandered = false;

    ReaperAiTest() {
        Keeper keeper = new Keeper(false, PLAYER_ID);
        keeper.createAlliance(ALLY_ID);
        keepers.put(PLAYER_ID, keeper);
        keepers.put(ENEMY_ID, new Keeper(false, ENEMY_ID));
        keepers.put(ALLY_ID, new Keeper(false, ALLY_ID));

        entityData.setComponent(reaper, new Owner(PLAYER_ID, PLAYER_ID));
        entityData.setComponent(reaper, new Position(0, WorldUtils.pointToVector3f(REAPER_TILE)));
    }

    @Test
    void attacksTheNearestEnemyCreature() {
        addCreature(ENEMY_ID, new Point(16, 10));
        EntityId nearest = addCreature(ENEMY_ID, new Point(13, 10));

        ai().think(reaper, PLAYER_ID);

        assertEquals(CreatureState.FIGHT, state);
        assertEquals(nearest, entityData.getComponent(reaper, AttackTarget.class).entityId);
    }

    @Test
    void heroesAreEnemiesToo() {
        keepers.put(Player.GOOD_PLAYER_ID, new Keeper(false, Player.GOOD_PLAYER_ID));
        EntityId hero = addCreature(Player.GOOD_PLAYER_ID, new Point(14, 10));

        ai().think(reaper, PLAYER_ID);

        assertEquals(hero, entityData.getComponent(reaper, AttackTarget.class).entityId);
    }

    @Test
    void leavesOwnAlliedAndNeutralCreaturesAlone() {
        addCreature(PLAYER_ID, new Point(11, 10));
        addCreature(ALLY_ID, new Point(12, 10));
        addCreature(Player.NEUTRAL_PLAYER_ID, new Point(13, 10));

        ai().think(reaper, PLAYER_ID);

        assertNull(entityData.getComponent(reaper, AttackTarget.class));
    }

    @Test
    void skipsEnemiesHeCanNotReach() {
        Point walledIn = new Point(12, 10);
        unreachableTiles.add(walledIn);
        addCreature(ENEMY_ID, walledIn);
        EntityId reachable = addCreature(ENEMY_ID, new Point(18, 10));

        ai().think(reaper, PLAYER_ID);

        assertEquals(reachable, entityData.getComponent(reaper, AttackTarget.class).entityId);
    }

    @Test
    void skipsEnemiesThatCanNotBeAttacked() {
        entityData.setComponent(addCreature(ENEMY_ID, new Point(11, 10)), new Unconscious(0));
        entityData.setComponent(addCreature(ENEMY_ID, new Point(12, 10)), new CreatureImprisoned(0, 0));
        entityData.setComponent(addCreature(ENEMY_ID, new Point(13, 10)), new InHand(0, ENEMY_ID, null, (short) 0));
        EntityId leavingReaper = addCreature(ENEMY_ID, new Point(14, 10));
        entityData.setComponent(leavingReaper, new SummonedReaper(0, 0, 0, SummonedReaper.NOT_YET, 5));
        EntityId target = addCreature(ENEMY_ID, new Point(15, 10));

        ai().think(reaper, PLAYER_ID);

        assertEquals(target, entityData.getComponent(reaper, AttackTarget.class).entityId);
    }

    @Test
    void goesForTheEnemyDungeonHeartWhenThereAreNoEnemyCreatures() {
        Point heart = new Point(30, 12);
        keepers.get(ENEMY_ID).setDungeonHeartLocation(heart);
        keepers.get(ALLY_ID).setDungeonHeartLocation(new Point(12, 10));

        ai().think(reaper, PLAYER_ID);

        Navigation navigation = entityData.getComponent(reaper, Navigation.class);
        assertNotNull(navigation);
        assertEquals(heart, navigation.target);
        assertEquals(CreatureState.WANDER, state);
    }

    @Test
    void goesForTheHeartThatIsNearestTheWayDungeonKeeperIIMeasuresIt() {
        keepers.get(ENEMY_ID).setDungeonHeartLocation(new Point(13, 10));
        short otherEnemyId = 6;
        keepers.put(otherEnemyId, new Keeper(false, otherEnemyId));
        Point below = new Point(10, 14);
        keepers.get(otherEnemyId).setDungeonHeartLocation(below);

        // 3 * 3 = 9 for the one across, 2 * 4 = 8 for the one further away below
        assertTrue(ai().think(reaper, PLAYER_ID));

        assertEquals(below, entityData.getComponent(reaper, Navigation.class).target);
    }

    @Test
    void skipsADestroyedHeart() {
        keepers.get(ENEMY_ID).setDungeonHeartLocation(new Point(12, 10));
        keepers.get(ENEMY_ID).setDestroyed(PLAYER_ID);

        assertFalse(ai().think(reaper, PLAYER_ID));
    }

    @Test
    void hasNothingLeftToDoWhenHeCanReachNothing() {
        Point walledIn = new Point(12, 10);
        unreachableTiles.add(walledIn);
        addCreature(ENEMY_ID, walledIn);
        Point heart = new Point(30, 12);
        keepers.get(ENEMY_ID).setDungeonHeartLocation(heart);
        unreachableTiles.add(heart);

        assertFalse(ai().think(reaper, PLAYER_ID));
        assertFalse(wandered);
        assertNull(entityData.getComponent(reaper, AttackTarget.class));
    }

    @Test
    void wandersOneTimeInThree() {
        roll = 0;
        addCreature(ENEMY_ID, new Point(13, 10));

        ai().think(reaper, PLAYER_ID);

        assertTrue(wandered);
        assertEquals(CreatureState.WANDER, state);
        assertNull(entityData.getComponent(reaper, AttackTarget.class));
    }

    @Test
    void keepsFightingWhileHisTargetIsThere() {
        EntityId target = addCreature(ENEMY_ID, new Point(16, 10));
        addCreature(ENEMY_ID, new Point(11, 10));
        entityData.setComponent(reaper, new AttackTarget(target));
        state = CreatureState.FIGHT;

        ai().think(reaper, PLAYER_ID);

        assertEquals(target, entityData.getComponent(reaper, AttackTarget.class).entityId);
    }

    @Test
    void picksNewTargetWhenHisTargetIsDown() {
        EntityId target = addCreature(ENEMY_ID, new Point(11, 10));
        entityData.setComponent(target, new Unconscious(0));
        EntityId next = addCreature(ENEMY_ID, new Point(16, 10));
        entityData.setComponent(reaper, new AttackTarget(target));
        state = CreatureState.FIGHT;

        ai().think(reaper, PLAYER_ID);

        assertEquals(next, entityData.getComponent(reaper, AttackTarget.class).entityId);
    }

    @Test
    void doesNotDecideWhileWalking() {
        addCreature(ENEMY_ID, new Point(13, 10));
        entityData.setComponent(reaper, new Navigation(new Point(1, 1), null, null));
        state = CreatureState.WANDER;

        ai().think(reaper, PLAYER_ID);

        assertEquals(CreatureState.WANDER, state);
        assertNull(entityData.getComponent(reaper, AttackTarget.class));
    }

    @Test
    void doesNotDecideWhileStriking() {
        addCreature(ENEMY_ID, new Point(13, 10));
        state = CreatureState.MELEE_ATTACK;

        ai().think(reaper, PLAYER_ID);

        assertEquals(CreatureState.MELEE_ATTACK, state);
    }

    @Test
    void walksToThePortalGem() {
        Point gemTile = new Point(20, 14);
        EntityId gem = addGem(gemTile);

        ai().fetchPortalGem(reaper, gem);

        Navigation navigation = entityData.getComponent(reaper, Navigation.class);
        assertNotNull(navigation);
        assertEquals(gemTile, navigation.target);
        assertEquals(CreatureState.WANDER, state);
    }

    @Test
    void goesBackToTheGemIfHeWandersOff() {
        Point gemTile = new Point(20, 14);
        EntityId gem = addGem(gemTile);
        entityData.setComponent(reaper, new Navigation(new Point(2, 2), null, null));

        ai().fetchPortalGem(reaper, gem);

        assertEquals(gemTile, entityData.getComponent(reaper, Navigation.class).target);
    }

    @Test
    void picksUpThePortalGemOnItsTile() {
        EntityId gem = addGem(REAPER_TILE);
        entityData.setComponent(reaper, new CreatureAi(0, CreatureState.WANDER, (short) 27));
        entityData.setComponent(reaper, new Navigation(REAPER_TILE, null, null));

        ai().fetchPortalGem(reaper, gem);

        assertNotNull(entityData.getComponent(reaper, PortalGem.class));
        assertNull(entityData.getComponent(gem, ObjectComponent.class));
        assertNull(entityData.getComponent(gem, Position.class));

        // With the gem in his hands he has done his job
        assertNull(entityData.getComponent(reaper, CreatureAi.class));
        assertNull(entityData.getComponent(reaper, Navigation.class));
    }

    @Test
    void destroysTheBuiltTrapsOnAndNextToHisTileOnHisWayToTheGem() {
        EntityId gem = addGem(new Point(20, 14));
        EntityId under = addThing(new TrapComponent((short) 1), ENEMY_ID, REAPER_TILE, 50);
        EntityId own = addThing(new TrapComponent((short) 1), PLAYER_ID, new Point(REAPER_TILE.x, REAPER_TILE.y + 1), 70);
        EntityId blueprint = addThing(new TrapComponent((short) 1), ENEMY_ID, new Point(REAPER_TILE.x - 1, REAPER_TILE.y), 0);
        EntityId diagonal = addThing(new TrapComponent((short) 1), ENEMY_ID, new Point(REAPER_TILE.x + 1, REAPER_TILE.y + 1), 50);
        EntityId far = addThing(new TrapComponent((short) 1), ENEMY_ID, new Point(REAPER_TILE.x + 2, REAPER_TILE.y), 50);

        ai().fetchPortalGem(reaper, gem);

        // DK II asks no owner, the whole health is the damage
        assertEquals(50, entityData.getComponent(under, Damage.class).damage);
        assertEquals(70, entityData.getComponent(own, Damage.class).damage);
        assertNull(entityData.getComponent(blueprint, Damage.class));
        assertNull(entityData.getComponent(diagonal, Damage.class));
        assertNull(entityData.getComponent(far, Damage.class));
    }

    @Test
    void unlocksHisKeepersLockedDoorNextToHimOnHisWayToTheGem() {
        EntityId gem = addGem(new Point(20, 14));
        EntityId own = addThing(new DoorComponent((short) 1, true, false), PLAYER_ID, new Point(REAPER_TILE.x + 1, REAPER_TILE.y), 100);
        EntityId enemy = addThing(new DoorComponent((short) 1, true, false), ENEMY_ID, new Point(REAPER_TILE.x, REAPER_TILE.y - 1), 100);
        EntityId blueprint = addThing(new DoorComponent((short) 1, true, true), PLAYER_ID, new Point(REAPER_TILE.x - 1, REAPER_TILE.y), 0);

        ai().fetchPortalGem(reaper, gem);

        assertFalse(entityData.getComponent(own, DoorComponent.class).locked);
        assertTrue(entityData.getComponent(enemy, DoorComponent.class).locked);
        assertTrue(entityData.getComponent(blueprint, DoorComponent.class).locked);
        for (EntityId door : List.of(own, enemy, blueprint)) {
            assertNull(entityData.getComponent(door, Damage.class));
        }
    }

    /**
     * A trap or a door, without health when it is a blueprint
     */
    private EntityId addThing(EntityComponent component, short ownerId, Point tile, int health) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, component, new Owner(ownerId, ownerId), new Position(0, WorldUtils.pointToVector3f(tile)));
        if (health > 0) {
            entityData.setComponent(entityId, new Health(health, health));
        }
        return entityId;
    }

    private EntityId addGem(Point tile) {
        EntityId gem = entityData.createEntity();
        entityData.setComponent(gem, new ObjectComponent((short) 28, null));
        entityData.setComponent(gem, new Position(0, WorldUtils.pointToVector3f(tile)));
        return gem;
    }

    private EntityId addCreature(short ownerId, Point tile) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new CreatureComponent());
        entityData.setComponent(entityId, new Owner(ownerId, ownerId));
        entityData.setComponent(entityId, new Position(0, WorldUtils.pointToVector3f(tile)));
        entityData.setComponent(entityId, new Health(100, 100));
        return entityId;
    }

    private ReaperAi ai() {
        IEntityPositionLookup entityPositionLookup = (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(),
                new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("getEntitiesInLocation")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return entityData.findEntities(null, Position.class).stream().filter(entityId
                            -> args[0].equals(WorldUtils.vectorToPoint(entityData.getComponent(entityId, Position.class).position))).toList();
                });
        return new ReaperAi(entityData, keepers, creaturesController(), navigationService(), entityPositionLookup, new Random() {
            @Override
            public int nextInt(int bound) {
                return roll;
            }
        });
    }

    private ICreaturesController creaturesController() {
        StateMachine<ICreatureController, CreatureState> stateMachine = (StateMachine<ICreatureController, CreatureState>) Proxy.newProxyInstance(
                StateMachine.class.getClassLoader(), new Class<?>[]{StateMachine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "changeState" -> {
                        state = (CreatureState) args[0];
                        yield null;
                    }
                    case "getCurrentState" -> state;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        ICreatureController reaperController = (ICreatureController) Proxy.newProxyInstance(
                ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getStateMachine" -> stateMachine;
                    case "isStopped" -> entityData.getComponent(reaper, Navigation.class) == null;
                    case "navigateToRandomPoint" -> {
                        wandered = true;
                        yield null;
                    }
                    case "getEntityId" -> reaper;
                    case "unassingCurrentTask" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("createController") || !reaper.equals(args[0])) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return reaperController;
                });
    }

    private INavigationService navigationService() {
        return (INavigationService) Proxy.newProxyInstance(INavigationService.class.getClassLoader(),
                new Class<?>[]{INavigationService.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "findPath" -> {
                        Point end = (Point) args[1];
                        if (unreachableTiles.contains(end)) {
                            yield null;
                        }
                        DefaultGraphPath<IMapTileInformation> path = new DefaultGraphPath<>();
                        path.add(tile((Point) args[0]));
                        path.add(tile(end));
                        yield path;
                    }
                    case "findRandomAccessibleTile" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static IMapTileInformation tile(Point location) {
        return (IMapTileInformation) Proxy.newProxyInstance(IMapTileInformation.class.getClassLoader(),
                new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("getLocation")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return location;
                });
    }

}
