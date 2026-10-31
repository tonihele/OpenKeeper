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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Senses;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A creature only goes for an enemy it sees within its sight distance, or
 * hears within its hearing distance and can walk to within that distance too,
 * like DK II
 */
class CreatureNoticeEnemyTest {

    private final EntityData data = new DefaultEntityData();
    private final Set<Point> walls = new HashSet<>();
    private final Creature heroData = CreatureSleepFixtures.sleeper(1);
    private final Creature enemyData = CreatureSleepFixtures.sleeper(1);
    private List<Point> walk;
    private EntityId enemy;

    @AfterEach
    void close() {
        data.close();
    }

    @Test
    void goesForAnEnemyInSight() {
        enemy = creature(Player.KEEPER1_ID, 13, 10);
        walk = List.of(new Point(11, 10), new Point(12, 10), new Point(13, 10));

        assertEquals(enemy, attackTarget(creature(Player.GOOD_PLAYER_ID, 10, 10)));
    }

    @Test
    void leavesAnEnemyBeyondItsSensesAlone() {
        enemy = creature(Player.KEEPER1_ID, 14.4f, 10);
        walk = List.of(new Point(11, 10), new Point(12, 10), new Point(13, 10), new Point(14, 10));

        assertNull(attackTarget(creature(Player.GOOD_PLAYER_ID, 10, 10)));
    }

    @Test
    void hearsAnEnemyBehindAWallOnlyWhenTheWalkThereIsShort() {
        enemy = creature(Player.KEEPER1_ID, 12, 10);
        walls.add(new Point(11, 10));
        EntityId hero = creature(Player.GOOD_PLAYER_ID, 10, 10);

        walk = List.of(new Point(10, 11), new Point(11, 11), new Point(12, 11), new Point(12, 10));
        assertNull(attackTarget(hero));

        walk = List.of(new Point(11, 11), new Point(12, 10));
        assertEquals(enemy, attackTarget(hero));
    }

    @Test
    void seesAnEnemyOffTheMapAxes() {
        enemy = creature(Player.KEEPER1_ID, 12, 13);

        assertEquals(enemy, attackTarget(creature(Player.GOOD_PLAYER_ID, 10, 10)));
    }

    @Test
    void seesAnEnemyOfTheOtherAlignmentFurtherAway() {
        enemy = creature(Player.KEEPER1_ID, 14.2f, 10);
        EntityId hero = creature(Player.GOOD_PLAYER_ID, 10, 10);

        assertNull(attackTarget(hero));

        heroData.getFlags().add(Creature.CreatureFlag.IS_GOOD);
        enemyData.getFlags().add(Creature.CreatureFlag.IS_EVIL);
        assertEquals(enemy, attackTarget(hero));
    }

    private EntityId creature(short ownerId, float x, float y) {
        EntityId entity = data.createEntity();
        data.setComponents(entity, new Senses(4, 4), new Health(1000, 1000), new Owner(ownerId, ownerId),
                new Position(0, new Vector3f(x, WorldUtils.FLOOR_HEIGHT, y)), new CreatureComponent());
        return entity;
    }

    private EntityId attackTarget(EntityId hero) {
        Point heroTile = WorldUtils.vectorToPoint(data.getComponent(hero, Position.class).position);
        Point enemyTile = WorldUtils.vectorToPoint(data.getComponent(enemy, Position.class).position);
        IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
            case "getEntityLocation" -> tile(heroTile);
            case "getEntitiesInLocation" -> ((IMapTileInformation) args[0]).getLocation().equals(enemyTile) ? List.of(enemy) : List.of();

            // The sensed area spreads four steps along the map axes
            case "getSensedEntities" -> Math.abs(enemyTile.x - heroTile.x) + Math.abs(enemyTile.y - heroTile.y) <= 4 ? Set.of(enemy) : Set.of();
            default -> throw new UnsupportedOperationException(method);
        });
        HashMap<EntityId, ICreatureController> controllers = new HashMap<>();
        ICreaturesController creaturesController = proxy(ICreaturesController.class, (method, args) -> switch (method) {
            case "isValidEntity" -> true;
            case "createController" -> controllers.computeIfAbsent((EntityId) args[0],
                    id -> proxy(ICreatureController.class, (m, a) -> switch (m) {
                        case "getEntityId" -> id;
                        case "isWorker" -> false;
                        default -> throw new UnsupportedOperationException(m);
                    }));
            default -> throw new UnsupportedOperationException(method);
        });
        Keeper heroes = new Keeper(false, Player.GOOD_PLAYER_ID);
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> switch (method) {
            case "getPlayer" -> heroes;
            case "getLevelData" -> proxy(IKwdFile.class, (m, a) -> enemyData);
            default -> throw new UnsupportedOperationException(method);
        });
        IMapDataInformation<?> mapData = proxy(IMapDataInformation.class, (method, args) -> tile(new Point((int) args[0], (int) args[1])));
        IMapController mapController = proxy(IMapController.class, (method, args) -> switch (method) {
            case "isSolid" -> walls.contains((Point) args[0]);
            case "getMapData" -> mapData;
            default -> throw new UnsupportedOperationException(method);
        });
        INavigationService navigationService = proxy(INavigationService.class, (method, args) -> switch (method) {
            case "findPath" -> {
                if (walk == null) {
                    yield null;
                }
                DefaultGraphPath<IMapTileInformation> path = new DefaultGraphPath<>();
                path.add(tile((Point) args[0]));
                walk.forEach(point -> path.add(tile(point)));
                yield path;
            }
            default -> throw new UnsupportedOperationException(method);
        });
        CreatureController controller = new CreatureController(hero, data, heroData, navigationService, null, () -> 0,
                new HashMap<>(), creaturesController, lookup, mapController, levelInfo, null, null, null);

        var target = controller.getAttackTarget();
        return target != null ? target.getEntityId() : null;
    }

    private static IMapTileInformation tile(Point point) {
        return proxy(IMapTileInformation.class, (method, args) -> switch (method) {
            case "getX" -> point.x;
            case "getY" -> point.y;
            case "getLocation" -> point;
            default -> throw new UnsupportedOperationException(method);
        });
    }

    @FunctionalInterface
    private interface Invocation {

        Object call(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (object, method, args) -> switch (method.getName()) {
                    case "toString" -> type.getSimpleName();
                    case "hashCode" -> System.identityHashCode(object);
                    case "equals" -> object == args[0];
                    default -> call.call(method.getName(), args);
                });
    }
}
