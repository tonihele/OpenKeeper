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
package toniarts.openkeeper.game.controller;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.GameObjectFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.SummonReaperFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReaperSummonerTest {

    private static final short PLAYER_ID = 3;
    private static final short REAPER_ID = 27;
    private static final short REAPER_SHOT_ID = 46;
    private static final double NOW = 120;
    private static final short HERO_ID = 1;
    private static final short GEM_ID = 28;
    private static final Point GEM_TILE = new Point(20, 20);

    private final EntityData entityData = new DefaultEntityData();
    private final Set<Point> unreachableTiles = new HashSet<>();
    private final Set<Point> solidTiles = new HashSet<>();
    private final List<Point> searchedTiles = new ArrayList<>();
    private final Deque<Integer> rolls = new ArrayDeque<>();
    private final List<GameObject> objects = List.of(GameObjectFixtures.gameObject("Object", (short) 1, 0),
            GameObjectFixtures.gameObject("Portal Gem", GEM_ID, 0, GameObject.ObjectFlag.OBJECT_TYPE_LEVEL_GEM));
    private final List<Object[]> spawns = new ArrayList<>();
    private final List<Creature> creatures = new ArrayList<>(List.of(
            SummonReaperFixtures.creature((short) 1, Creature.CreatureFlag.IS_WORKER),
            SummonReaperFixtures.creature(REAPER_ID, Creature.CreatureFlag.IS_HORNY)));
    private final List<KeeperSpell> keeperSpells = List.of(
            SummonReaperFixtures.keeperSpell((short) 4, (short) 2, 1, 1500, 7),
            SummonReaperFixtures.keeperSpell((short) 5, REAPER_SHOT_ID, 0, 100000, 2000),
            SummonReaperFixtures.keeperSpell((short) 14, REAPER_SHOT_ID, 99, 0, 125));

    @Test
    void summonsTheHornedReaperAtTheCastPoint() {
        Vector2f position = new Vector2f(10.5f, 4.5f);

        EntityId reaper = summoner().summon(REAPER_SHOT_ID, 0, PLAYER_ID, position);

        assertEquals(1, spawns.size());
        Object[] spawn = spawns.get(0);
        assertEquals(REAPER_ID, spawn[0]);
        assertEquals(PLAYER_ID, spawn[1]);
        assertEquals(position, spawn[3]);
        assertEquals(ICreaturesController.SpawnType.CONJURE, spawn[4]);
        assertNotNull(entityData.getComponent(reaper, SummonedReaper.class));
    }

    @Test
    void reaperSummonedBySpellIsLevelTenAndPaidWithMana() {
        EntityId reaper = summoner().summon(REAPER_SHOT_ID, 0, PLAYER_ID, new Vector2f(1, 1));

        assertEquals(10, spawns.get(0)[2]);
        SummonedReaper summonedReaper = entityData.getComponent(reaper, SummonedReaper.class);
        assertEquals(NOW, summonedReaper.summonTime);
        assertEquals(0, summonedReaper.lifetime);

        // The drain is the one of the spell that summons the Reaper for good
        assertEquals(2000, summonedReaper.manaDrain);
    }

    @Test
    void oneShotReaperUsesTheLevelOfTheMapAndStaysForTheShotTime() {
        EntityId reaper = summoner().summon(REAPER_SHOT_ID, 99, PLAYER_ID, new Vector2f(1, 1));

        assertEquals(6, spawns.get(0)[2]);
        SummonedReaper summonedReaper = entityData.getComponent(reaper, SummonedReaper.class);
        assertEquals(99, summonedReaper.lifetime);
        assertEquals(0, summonedReaper.manaDrain);
    }

    @Test
    void nothingIsSummonedWithoutReaperCreature() {
        creatures.remove(1);

        assertNull(summoner().summon(REAPER_SHOT_ID, 0, PLAYER_ID, new Vector2f(1, 1)));
        assertEquals(0, spawns.size());
    }

    @Test
    void reaperAppearsAtLeastFiveTilesFromTheGemAndGoesForIt() {
        EntityId gem = addGem(GEM_TILE);

        // The first spot is right on the gem, too close
        roll(0, 0);
        roll(5, 0);
        EntityId reaper = sendForGem();

        assertEquals(1, spawns.size());
        Object[] spawn = spawns.get(0);
        assertEquals(REAPER_ID, spawn[0]);
        assertEquals(PLAYER_ID, spawn[1]);
        assertEquals(6, spawn[2]);
        assertEquals(WorldUtils.pointToVector2f(25, 20), spawn[3]);
        assertEquals(gem, entityData.getComponent(reaper, SummonedReaper.class).gem);
    }

    @Test
    void reaperDoesNotAppearWhereHeCanNotReachTheGemOrOnACreature() {
        addGem(GEM_TILE);
        unreachableTiles.add(new Point(25, 20));
        addCreature((short) 1, HERO_ID, new Point(20, 26));

        roll(5, 0);
        roll(0, 6);
        roll(-6, 1);
        sendForGem();

        assertEquals(WorldUtils.pointToVector2f(14, 21), spawns.get(0)[3]);
    }

    @Test
    void reaperCanAppearEightTilesAwayOnEachAxis() {
        addGem(GEM_TILE);

        // Rolls are the raw nextInt values, 16 is the top of the range
        rolls.add(16);
        rolls.add(16);
        sendForGem();

        assertEquals(WorldUtils.pointToVector2f(28, 28), spawns.get(0)[3]);
    }

    @Test
    void solidTilesAreNotSearchedAsEntrances() {
        addGem(GEM_TILE);
        Point solid = new Point(GEM_TILE.x + 5, GEM_TILE.y);
        solidTiles.add(solid);

        roll(5, 0);
        roll(0, 5);
        sendForGem();

        assertFalse(searchedTiles.contains(solid));
        assertEquals(WorldUtils.pointToVector2f(20, 25), spawns.get(0)[3]);
    }

    @Test
    void keepersOwnReaperGoesForTheGem() {
        EntityId gem = addGem(GEM_TILE);
        EntityId ownReaper = addCreature(REAPER_ID, PLAYER_ID, new Point(3, 3));
        entityData.setComponent(ownReaper, new SummonedReaper(0, 0, 2000));
        entityData.setComponent(ownReaper, new AttackTarget(entityData.createEntity()));

        EntityId reaper = sendForGem();

        assertEquals(ownReaper, reaper);
        assertTrue(spawns.isEmpty());
        assertEquals(gem, entityData.getComponent(reaper, SummonedReaper.class).gem);
        assertEquals(2000, entityData.getComponent(reaper, SummonedReaper.class).manaDrain);
        assertNull(entityData.getComponent(reaper, AttackTarget.class));
    }

    @Test
    void reaperOfTheMapGoesForTheGemToo() {
        EntityId gem = addGem(GEM_TILE);
        EntityId mapReaper = addCreature(REAPER_ID, PLAYER_ID, new Point(3, 3));

        assertEquals(mapReaper, sendForGem());
        assertEquals(gem, entityData.getComponent(mapReaper, SummonedReaper.class).gem);
    }

    @Test
    void ownReaperInHandIsNotSentForTheGem() {
        addGem(GEM_TILE);
        EntityId held = addCreature(REAPER_ID, PLAYER_ID, new Point(3, 3));
        entityData.setComponent(held, new InHand());

        roll(5, 0);
        EntityId reaper = sendForGem();

        assertFalse(reaper.equals(held));
        assertEquals(1, spawns.size());
        assertNull(entityData.getComponent(held, SummonedReaper.class));
    }

    @Test
    void walledOffOwnReaperIsNotSentForTheGem() {
        addGem(GEM_TILE);
        EntityId walledOff = addCreature(REAPER_ID, PLAYER_ID, new Point(3, 3));
        unreachableTiles.add(GEM_TILE);

        roll(5, 0);
        EntityId reaper = sendForGem();

        assertFalse(reaper.equals(walledOff));
        assertEquals(1, spawns.size());
    }

    @Test
    void gemStillCarriedIsDroppedWhereTheCarrierIs() {
        EntityId lord = addCreature((short) 1, HERO_ID, new Point(7, 9));
        entityData.setComponent(lord, new PortalGem());

        roll(5, 0);
        EntityId reaper = sendForGem();

        assertNull(entityData.getComponent(lord, PortalGem.class));
        EntityId gem = entityData.getComponent(reaper, SummonedReaper.class).gem;
        assertEquals(WorldUtils.pointToVector3f(7, 9), entityData.getComponent(gem, Position.class).position);
    }

    @Test
    void reaperIsSentOnlyOnceWhenCalledOnEveryTick() {
        addGem(GEM_TILE);

        assertNotNull(sendForGem());
        assertNull(sendForGem());
        assertEquals(1, spawns.size());
    }

    @Test
    void nothingHappensWithoutPortalGem() {
        assertNull(sendForGem());
        assertTrue(spawns.isEmpty());
    }

    private void roll(int dx, int dy) {
        rolls.add(dx + 8);
        rolls.add(dy + 8);
    }

    private EntityId sendForGem() {
        return summoner().sendForPortalGem(PLAYER_ID, objectsController(), navigationService(), mapInformation(), new Random() {
            @Override
            public int nextInt(int bound) {
                return rolls.isEmpty() ? 8 : rolls.poll();
            }
        });
    }

    private EntityId addGem(Point tile) {
        EntityId gem = entityData.createEntity();
        entityData.setComponent(gem, new ObjectComponent(GEM_ID, null));
        entityData.setComponent(gem, new Owner(HERO_ID, HERO_ID));
        entityData.setComponent(gem, new Position(0, WorldUtils.pointToVector3f(tile)));
        return gem;
    }

    private EntityId addCreature(short creatureId, short ownerId, Point tile) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponent(entityId, creatureComponent);
        entityData.setComponent(entityId, new Owner(ownerId, ownerId));
        entityData.setComponent(entityId, new Position(0, WorldUtils.pointToVector3f(tile)));
        entityData.setComponent(entityId, new Health(100, 100));
        return entityId;
    }

    private IObjectsController objectsController() {
        return (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("dropPortalGem")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    EntityId carrier = (EntityId) args[0];
                    entityData.removeComponent(carrier, PortalGem.class);
                    return addGem(WorldUtils.vectorToPoint(entityData.getComponent(carrier, Position.class).position));
                });
    }

    private IMapInformation<?> mapInformation() {
        return (IMapInformation<?>) Proxy.newProxyInstance(IMapInformation.class.getClassLoader(),
                new Class<?>[]{IMapInformation.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("isSolid")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return solidTiles.contains((Point) args[0]);
                });
    }

    private INavigationService navigationService() {
        return (INavigationService) Proxy.newProxyInstance(INavigationService.class.getClassLoader(),
                new Class<?>[]{INavigationService.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("findPath")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    Point from = (Point) args[0];
                    Point to = (Point) args[1];
                    searchedTiles.add(to);
                    if (unreachableTiles.contains(to)) {
                        return null;
                    }
                    return new DefaultGraphPath<IMapTileInformation>();
                });
    }

    private ReaperSummoner summoner() {
        return new ReaperSummoner(kwdFile(), entityData, () -> NOW, creaturesController());
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCreatureList" -> creatures;
                    case "getCreature" -> creatures.stream().filter(c -> c.getCreatureId() == (short) args[0]).findFirst().orElse(null);
                    case "getObjectList" -> objects;
                    case "getKeeperSpells" -> keeperSpells;
                    case "getShotById" -> SummonReaperFixtures.shot((short) args[0], Shot.ProcessType.SUMMON_REAPER);
                    case "getGameLevel" -> SummonReaperFixtures.gameLevel((short) 6);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ICreaturesController creaturesController() {
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("spawnCreature") || args.length != 5) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    spawns.add(args);
                    return entityData.createEntity();
                });
    }

}
