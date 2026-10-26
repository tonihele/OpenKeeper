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

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedAttack;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The melee attack of a creature its keeper possesses
 */
class PossessedAttackTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;
    private static final int DAMAGE = 100;
    private static final float RECHARGE = 1.5f;
    private static final float RANGE = 0.5f;
    private static final Terrain FLOOR = TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(), 0, 0, 0, (short) 0);
    private static final Terrain DIRT = TerrainFixtures.terrain((short) 2, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, 0, 0, (short) 0);
    private static final Terrain ROCK = TerrainFixtures.terrain((short) 3, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID,
            Terrain.TerrainFlag.IMPENETRABLE), 0, 0, 0, (short) 0);

    private EntityData entityData;
    private Map<Point, Terrain> terrain;
    private List<Point> dugTiles;
    private double gameTime;
    private EntityId us;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        terrain = new HashMap<>();
        dugTiles = new ArrayList<>();
        gameTime = 10;

        // At the middle of tile (5, 5), facing +z
        us = createCreature(KEEPER, new Vector3f(5, 0, 5));
        entityData.setComponent(us, new Possessed(0, 0));
    }

    @Test
    void hitsTheCreatureInFrontWithItsMeleeDamage() {
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 5.8f));

        createController().executePossessedAttack();

        assertEquals(DAMAGE, entityData.getComponent(enemy, Damage.class).damage);
        assertEquals(Creature.AnimationType.MELEE_ATTACK, entityData.getComponent(us, CreatureViewState.class).state);
    }

    @Test
    void reachesTheNextTileNotOnlyTheMeleeRange() {
        EntityId nextTile = createCreature(HERO, new Vector3f(5, 0, 6.3f));

        createController().executePossessedAttack();

        assertEquals(DAMAGE, entityData.getComponent(nextTile, Damage.class).damage);
    }

    @Test
    void doesNotReachPastTheNextTile() {
        EntityId twoTilesAway = createCreature(HERO, new Vector3f(5, 0, 6.6f));

        createController().executePossessedAttack();

        assertNull(entityData.getComponent(twoTilesAway, Damage.class));
    }

    @Test
    void hitsTheNearestCreatureInFrontOnly() {
        EntityId far = createCreature(HERO, new Vector3f(5, 0, 6.3f));
        EntityId near = createCreature(HERO, new Vector3f(5.3f, 0, 5.6f));

        createController().executePossessedAttack();

        assertEquals(DAMAGE, entityData.getComponent(near, Damage.class).damage);
        assertNull(entityData.getComponent(far, Damage.class));
    }

    @Test
    void doesNotHitWhatIsBehindOrToTheSide() {
        EntityId behind = createCreature(HERO, new Vector3f(5, 0, 4.5f));
        EntityId side = createCreature(HERO, new Vector3f(6, 0, 5.1f));

        createController().executePossessedAttack();

        assertNull(entityData.getComponent(behind, Damage.class));
        assertNull(entityData.getComponent(side, Damage.class));
    }

    @Test
    void hitsNeutralCreaturesButNotOwnOnes() {
        EntityId own = createCreature(KEEPER, new Vector3f(5, 0, 5.5f));
        EntityId neutral = createCreature(Player.NEUTRAL_PLAYER_ID, new Vector3f(5, 0, 5.8f));

        createController().executePossessedAttack();

        assertNull(entityData.getComponent(own, Damage.class));
        assertEquals(DAMAGE, entityData.getComponent(neutral, Damage.class).damage);
    }

    @Test
    void swingsAgainOnlyOnceRecharged() {
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 5.8f));
        CreatureController controller = createController();

        controller.executePossessedAttack();
        gameTime += RECHARGE / 2;
        controller.executePossessedAttack();
        assertEquals(DAMAGE, entityData.getComponent(enemy, Damage.class).damage);

        gameTime += RECHARGE / 2;
        controller.executePossessedAttack();
        assertEquals(2 * DAMAGE, entityData.getComponent(enemy, Damage.class).damage);
    }

    @Test
    void doesNotSwingAtNothing() {
        createController().executePossessedAttack();

        assertNull(entityData.getComponent(us, CreatureMeleeAttack.class).attactStartTime);
        assertNull(entityData.getComponent(us, CreatureViewState.class));
    }

    @Test
    void quickClickIsKeptUntilItHasSwung() {
        createCreature(HERO, new Vector3f(5, 0, 5.8f));
        CreatureController controller = createController();

        controller.setPossessedAttack(true);
        controller.setPossessedAttack(false);
        assertFalse(entityData.getComponent(us, PossessedAttack.class).held);

        controller.executePossessedAttack();
        gameTime += RECHARGE;
        controller.setPossessedAttack(true);
        controller.executePossessedAttack();
        controller.setPossessedAttack(false);
        assertNull(entityData.getComponent(us, PossessedAttack.class));
    }

    @Test
    void possessedWorkerDigsTheTileItFaces() {
        makeWorker();
        terrain.put(new Point(6, 5), DIRT);

        // Facing mostly +x
        entityData.setComponent(us, new Position(1.2f, new Vector3f(5, 0, 5)));
        createController().executePossessedAttack();

        assertEquals(List.of(new Point(6, 5)), dugTiles);
        assertEquals(gameTime, entityData.getComponent(us, CreatureMeleeAttack.class).attactStartTime);
    }

    @Test
    void possessedWorkerCannotDigImpenetrableRock() {
        makeWorker();
        terrain.put(new Point(5, 6), ROCK);

        createController().executePossessedAttack();

        assertTrue(dugTiles.isEmpty());
        assertNull(entityData.getComponent(us, CreatureMeleeAttack.class).attactStartTime);
    }

    @Test
    void possessedWorkerFightsRatherThanDigs() {
        makeWorker();
        terrain.put(new Point(5, 6), DIRT);
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 5.4f));

        createController().executePossessedAttack();

        assertEquals(DAMAGE, entityData.getComponent(enemy, Damage.class).damage);
        assertTrue(dugTiles.isEmpty());
    }

    private void makeWorker() {
        CreatureComponent worker = entityData.getComponent(us, CreatureComponent.class);
        worker.worker = true;
        entityData.setComponents(us, worker, new Gold(0, 100));
    }

    private EntityId createCreature(short ownerId, Vector3f position) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new CreatureComponent(), new Owner(ownerId, ownerId), new Health(1000, 1000),
                new Position(0, position), new CreatureMeleeAttack(0, DAMAGE, RECHARGE, RANGE));
        return entity;
    }

    private List<EntityId> getEntitiesInLocation(Point tile) {
        List<EntityId> entities = new ArrayList<>();
        for (Entity entity : entityData.getEntities(Position.class)) {
            if (WorldUtils.vectorToPoint(entity.get(Position.class).position).equals(tile)) {
                entities.add(entity.getId());
            }
        }
        return entities;
    }

    private IMapTileInformation getTile(int x, int y) {
        if (x < 0 || y < 0 || x > 9 || y > 9) {
            return null;
        }
        Terrain tileTerrain = terrain.getOrDefault(new Point(x, y), FLOOR);
        return stub(IMapTileInformation.class, (name, args) -> switch (name) {
            case "getTerrainId" -> tileTerrain.getTerrainId();
            case "getGold" -> 0;
            case "getLocation" -> new Point(x, y);
            default -> null;
        });
    }

    private Terrain getTerrain(IMapTileInformation tile) {
        for (Terrain candidate : List.of(DIRT, ROCK)) {
            if (candidate.getTerrainId() == tile.getTerrainId()) {
                return candidate;
            }
        }
        return FLOOR;
    }

    private CreatureController createController() {
        IGameTimer gameTimer = stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? gameTime : null);
        IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class,
                (name, args) -> "getEntitiesInLocation".equals(name) ? getEntitiesInLocation((Point) args[0]) : null);
        ICreaturesController creaturesController = stub(ICreaturesController.class,
                (name, args) -> "isValidEntity".equals(name) ? entityData.getComponent((EntityId) args[0], CreatureComponent.class) != null : null);
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (name, args) -> {
            if (!"getTile".equals(name)) {
                return null;
            }
            return args.length == 1 ? getTile(((Point) args[0]).x, ((Point) args[0]).y) : getTile((Integer) args[0], (Integer) args[1]);
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> mapData;
            case "getTerrain" -> getTerrain((IMapTileInformation) args[0]);
            case "damageTile" -> {
                dugTiles.add((Point) args[0]);
                yield 0;
            }
            default -> null;
        });

        return new CreatureController(us, entityData, new Creature(), null, null, gameTimer, Map.of(), creaturesController,
                positionLookup, mapController, null, null, null);
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
