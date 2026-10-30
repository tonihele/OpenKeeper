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
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapData;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.HeartAttackFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonHeartAttackTest {

    private static final short FIGHTER = 1;
    private static final short IMP = 2;
    private static final short HERO = Player.GOOD_PLAYER_ID;
    private static final short KEEPER = Player.KEEPER1_ID;
    private static final Point HEART_TILE = new Point(10, 10);

    private EntityData entityData;
    private Map<Short, Creature> creatures;
    private Map<EntityId, Set<EntityId>> sensed;
    private Set<Point> solidTiles;
    private int pathSearches;
    private boolean walledOff;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        creatures = new HashMap<>();
        creatures.put(FIGHTER, HeartAttackFixtures.creature(FIGHTER, Creature.FightStyle.BLOCKER));
        creatures.put(IMP, HeartAttackFixtures.creature(IMP, Creature.FightStyle.NON_FIGHTER));
        sensed = new HashMap<>();
        solidTiles = new HashSet<>();
        pathSearches = 0;
        walledOff = false;
    }

    @Test
    void creatureGoesForTheEnemyHeartWhenThereIsNoOneToFight() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 14));
        EntityId heart = createHeart(KEEPER);
        sensed.put(hero, Set.of(heart));

        assertEquals(heart, createController(hero).getAttackTarget().getEntityId());
    }

    @Test
    void creatureLeavesItsOwnHeartAlone() {
        EntityId keeperCreature = createCreature(KEEPER, FIGHTER, new Point(10, 14));
        EntityId heart = createHeart(KEEPER);
        sensed.put(keeperCreature, Set.of(heart));

        assertNull(createController(keeperCreature).getAttackTarget());
    }

    @Test
    void nonFighterLeavesTheHeartAlone() {
        EntityId imp = createCreature(HERO, IMP, new Point(10, 14));
        EntityId heart = createHeart(KEEPER);
        sensed.put(imp, Set.of(heart));

        assertNull(createController(imp).getAttackTarget());
    }

    @Test
    void enemyCreatureComesBeforeTheHeart() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 13));
        EntityId heart = createHeart(KEEPER);
        EntityId defender = createCreature(KEEPER, FIGHTER, new Point(10, 15));
        sensed.put(hero, Set.of(heart, defender));

        assertEquals(defender, createController(hero).getAttackTarget().getEntityId());
    }

    @Test
    void creatureStaysOnTheHeartWithNoFighterInSight() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 12));
        EntityId heart = createHeart(KEEPER);
        EntityId worker = createCreature(KEEPER, IMP, new Point(10, 15));
        sensed.put(hero, Set.of(heart, worker));
        entityData.setComponent(hero, new AttackTarget(heart));

        assertEquals(heart, createController(hero).getAttackTarget().getEntityId());
    }

    @Test
    void creatureNextToTheHeartOnlyLeavesItForAWorkerAttackingIt() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 12));
        EntityId heart = createHeart(KEEPER);
        EntityId worker = createCreature(KEEPER, IMP, new Point(11, 12));
        sensed.put(hero, Set.of(heart, worker));
        entityData.setComponent(hero, new AttackTarget(heart));

        assertEquals(heart, createController(hero).getAttackTarget().getEntityId());

        entityData.setComponent(hero, new AttackTarget(heart));
        entityData.setComponent(worker, new AttackTarget(hero));
        assertEquals(worker, createController(hero).getAttackTarget().getEntityId());
    }

    @Test
    void creatureOnItsWayLeavesTheHeartForAWorkerNearerThanThePillar() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 13));
        EntityId heart = createHeart(KEEPER);
        EntityId worker = createCreature(KEEPER, IMP, new Point(10, 14));
        sensed.put(hero, Set.of(heart, worker));
        entityData.setComponent(hero, new AttackTarget(heart));

        assertEquals(worker, createController(hero).getAttackTarget().getEntityId());
    }

    @Test
    void creatureHittingTheHeartPicksItsTargetAgainEveryTenTurns() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 12));
        EntityId heart = createHeart(KEEPER);
        EntityId defender = createCreature(KEEPER, FIGHTER, new Point(10, 16));
        sensed.put(hero, Set.of(heart, defender));

        // Ten turns at four turns a second, the game time is 10.0
        entityData.setComponent(hero, new AttackTarget(heart, 7.6, new Point(10, 12)));
        assertEquals(heart, createController(hero).getAttackTarget().getEntityId());

        entityData.setComponent(hero, new AttackTarget(heart, 7.5, new Point(10, 12)));
        assertEquals(defender, createController(hero).getAttackTarget().getEntityId());
    }

    @Test
    void creatureInReachPicksItsTargetAgainOnAnotherTileOrAfterTenTurns() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 20));
        EntityId target = createCreature(KEEPER, FIGHTER, new Vector3f(10.4f, 0, 20));
        EntityId nearer = createCreature(KEEPER, FIGHTER, new Vector3f(10.1f, 0, 20));
        sensed.put(hero, Set.of(target, nearer));

        entityData.setComponent(hero, new AttackTarget(target, 7.6, new Point(10, 20)));
        assertEquals(target, createController(hero).getAttackTarget().getEntityId());

        entityData.setComponent(hero, new AttackTarget(target, 7.6, new Point(10, 19)));
        assertEquals(nearer, createController(hero).getAttackTarget().getEntityId());

        entityData.setComponent(hero, new AttackTarget(target, 7.5, new Point(10, 20)));
        assertEquals(nearer, createController(hero).getAttackTarget().getEntityId());
    }

    @Test
    void impPicksItsTargetAgainEveryFourTurns() {
        EntityId imp = createCreature(KEEPER, IMP, new Point(10, 20));
        EntityId target = createCreature(HERO, FIGHTER, new Vector3f(10.4f, 0, 20));
        EntityId nearer = createCreature(HERO, FIGHTER, new Vector3f(10.1f, 0, 20));
        sensed.put(imp, Set.of(target, nearer));

        entityData.setComponent(imp, new AttackTarget(target, 9.1, new Point(10, 20)));
        assertEquals(target, createController(imp).getAttackTarget().getEntityId());

        entityData.setComponent(imp, new AttackTarget(target, 9.0, new Point(10, 20)));
        assertEquals(nearer, createController(imp).getAttackTarget().getEntityId());
    }

    @Test
    void creatureChasingPicksAgainEveryTurnAndStopsWhenItPicksAnother() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 20));
        EntityId target = createCreature(KEEPER, FIGHTER, new Point(10, 23));
        EntityId nearer = createCreature(KEEPER, FIGHTER, new Point(10, 22));
        sensed.put(hero, Set.of(target, nearer));

        entityData.setComponent(hero, new AttackTarget(target, 9.9, new Point(10, 20)));
        assertEquals(nearer, createController(hero).getAttackTarget().getEntityId());

        // Out of sight it is not picked again, so the chase ends
        sensed.put(hero, Set.of());
        entityData.setComponent(hero, new AttackTarget(target, 9.9, new Point(10, 20)));
        assertNull(createController(hero).getAttackTarget());
    }

    @Test
    void creatureLeavesTheHeartForAFighterAndDoesNotGoBack() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 12));
        EntityId heart = createHeart(KEEPER);
        EntityId defender = createCreature(KEEPER, FIGHTER, new Point(10, 16));
        sensed.put(hero, Set.of(heart, defender));
        entityData.setComponent(hero, new AttackTarget(heart));

        assertEquals(defender, createController(hero).getAttackTarget().getEntityId());
        assertEquals(defender, createController(hero).getAttackTarget().getEntityId());
    }

    @Test
    void workerComesBeforeTheHeartWhenAFighterIsInSightToo() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 13));
        EntityId heart = createHeart(KEEPER);
        EntityId worker = createCreature(KEEPER, IMP, new Point(10, 14));
        EntityId defender = createCreature(KEEPER, FIGHTER, new Point(10, 18));
        sensed.put(hero, Set.of(heart, worker, defender));

        assertEquals(worker, createController(hero).getAttackTarget().getEntityId());
    }

    @Test
    void walledInTilesAreSkippedWithoutAPathSearch() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 15));
        EntityId heart = createHeart(KEEPER);
        for (int x = 8; x <= 12; x++) {
            for (int y = 8; y <= 12; y++) {
                if (Math.max(Math.abs(x - HEART_TILE.x), Math.abs(y - HEART_TILE.y)) == 2 && !(x == 11 && y == 8)) {
                    solidTiles.add(new Point(x, y));
                }
            }
        }

        createController(hero).navigateToAttackTarget(heart);

        // The tiles next to the south pillars are walled in, so it goes round to the north one
        assertEquals(new Point(11, 8), entityData.getComponent(hero, Navigation.class).target);
        // One search to pick the tile, one to build the route
        assertEquals(2, pathSearches);
    }

    @Test
    void heartWithNoWayToItIsDroppedAsTheTarget() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 15));
        EntityId heart = createHeart(KEEPER);
        entityData.setComponent(hero, new AttackTarget(heart));
        walledOff = true;

        createController(hero).navigateToAttackTarget(heart);

        assertNull(entityData.getComponent(hero, AttackTarget.class));
        assertNull(entityData.getComponent(hero, Navigation.class));
    }

    @Test
    void heartIsHitFromTheTilesNextToItsPillars() {
        EntityId heart = createHeart(KEEPER);

        assertTrue(createController(createCreature(HERO, FIGHTER, new Point(10, 12))).isWithinAttackDistance(heart));
        assertTrue(createController(createCreature(HERO, FIGHTER, new Point(12, 12))).isWithinAttackDistance(heart));
        assertTrue(createController(createCreature(HERO, FIGHTER, new Point(9, 8))).isWithinAttackDistance(heart));
        assertFalse(createController(createCreature(HERO, FIGHTER, new Point(10, 13))).isWithinAttackDistance(heart));

        // The north corners are too far from any pillar
        assertFalse(createController(createCreature(HERO, FIGHTER, new Point(8, 8))).isWithinAttackDistance(heart));
        assertFalse(createController(createCreature(HERO, FIGHTER, new Point(12, 9))).isWithinAttackDistance(heart));
    }

    @Test
    void hitOnTheHeartDamagesIt() {
        EntityId heart = createHeart(KEEPER);

        createController(createCreature(HERO, FIGHTER, new Point(10, 12))).executeAttack(heart);

        assertEquals(100, entityData.getComponent(heart, Damage.class).damage);
    }

    @Test
    void creatureWalksToTheNearestTileNextToTheHeart() {
        EntityId hero = createCreature(HERO, FIGHTER, new Point(10, 15));
        EntityId heart = createHeart(KEEPER);

        createController(hero).navigateToAttackTarget(heart);

        assertEquals(new Point(10, 12), entityData.getComponent(hero, Navigation.class).target);
    }

    private EntityId createHeart(short ownerId) {
        EntityId heart = entityData.createEntity();
        entityData.setComponents(heart, new ObjectComponent(FiveByFiveRotatedController.OBJECT_HEART_ID, null),
                new Owner(ownerId, ownerId), new Health(100000, 100000),
                new Position(0, new Vector3f(HEART_TILE.x, 0, HEART_TILE.y)));
        return heart;
    }

    private EntityId createCreature(short ownerId, short creatureId, Point tile) {
        return createCreature(ownerId, creatureId, new Vector3f(tile.x, 0, tile.y));
    }

    private EntityId createCreature(short ownerId, short creatureId, Vector3f position) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        creatureComponent.worker = creatureId == IMP;
        entityData.setComponents(entity, creatureComponent, new Owner(ownerId, ownerId), new Health(1000, 1000),
                new Position(0, position), new CreatureMeleeAttack(0, 100, 1, 0.5f));
        return entity;
    }

    private CreatureController createController(EntityId entity) {
        Map<Short, Keeper> keepers = Map.of(KEEPER, new Keeper(false, KEEPER), HERO, new Keeper(false, HERO));
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> switch (name) {
            case "getCreature" -> creatures.get((Short) args[0]);
            case "getGameLevel" -> CreatureSleepFixtures.level(4);
            default -> null;
        });
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> switch (name) {
            case "getLevelData" -> kwdFile;
            case "getPlayer" -> keepers.get((Short) args[0]);
            default -> null;
        });
        IGameTimer gameTimer = stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? 10.0 : null);
        IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class,
                (name, args) -> "getSensedEntities".equals(name) ? sensed.getOrDefault((EntityId) args[0], Set.of()) : null);
        ICreaturesController creaturesController = stub(ICreaturesController.class, (name, args) -> switch (name) {
            case "isValidEntity" -> entityData.getComponent((EntityId) args[0], CreatureComponent.class) != null;
            case "createController" -> createController((EntityId) args[0]);
            default -> null;
        });
        IObjectsController objectsController = stub(IObjectsController.class, (name, args) -> switch (name) {
            case "isValidEntity" -> entityData.getComponent((EntityId) args[0], ObjectComponent.class) != null;
            case "createController" -> stub(IObjectController.class, (method, params) -> "getEntityId".equals(method) ? args[0] : null);
            default -> null;
        });

        // The centre 3x3 of the heart can't be walked on
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> {
            if (!"findPath".equals(name)) {
                return null;
            }
            pathSearches++;
            Point to = (Point) args[1];
            boolean onHeart = Math.abs(to.x - HEART_TILE.x) <= 1 && Math.abs(to.y - HEART_TILE.y) <= 1;
            return onHeart || walledOff ? null : new DefaultGraphPath<>();
        });
        IMapData mapData = stub(IMapData.class, (name, args) -> "getTile".equals(name) && args.length == 1
                ? stub(IMapTileController.class, (method, params) -> args[0]) : null);
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> mapData;
            case "getTerrain" -> TerrainFixtures.terrain((short) 1, solidTiles.contains(((IMapTileController) args[0]).getLocation())
                    ? TerrainFixtures.flags(Terrain.TerrainFlag.SOLID) : TerrainFixtures.flags(), 0, 0, 0, (short) 0);
            default -> null;
        });
        CreatureComponent creatureComponent = entityData.getComponent(entity, CreatureComponent.class);

        return new CreatureController(entity, entityData, creatures.get(creatureComponent.creatureId), navigationService,
                null, gameTimer, Map.of(), creaturesController, positionLookup, mapController, levelInfo, objectsController, null,
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
