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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperDefeatFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A keeper whose Dungeon Heart is destroyed is defeated: he loses, his heart
 * room is a ruin, and in a skirmish the last keeper standing wins
 */
class KeeperDefeatSystemTest {

    private static final short HUMAN = Player.KEEPER1_ID;
    private static final short RIVAL = Player.KEEPER2_ID;
    private static final short OTHER_RIVAL = Player.KEEPER3_ID;
    private static final short PORTAL_GEM = 28;

    private EntityData entityData;
    private Map<Short, Keeper> keepers;
    private Map<Short, EntityId> hearts;
    private List<String> endings;
    private List<Point> ruinedHearts;
    private List<String> createdObjects;
    private GameLevel level;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        keepers = new HashMap<>();
        hearts = new HashMap<>();
        endings = new ArrayList<>();
        ruinedHearts = new ArrayList<>();
        createdObjects = new ArrayList<>();
        level = KeeperDefeatFixtures.level();
    }

    @Test
    void keeperWhoseHeartIsGoneIsDestroyed() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        entityData.removeEntity(hearts.get(RIVAL));
        system.processTick(0.25f);

        assertTrue(keepers.get(RIVAL).isDestroyed());
        assertFalse(keepers.get(HUMAN).isDestroyed());
    }

    @Test
    void heartWithNoHealthLeftDestroysTheKeeper() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        entityData.setComponent(hearts.get(RIVAL), new Health(0, 100_000));
        system.processTick(0.25f);

        assertTrue(keepers.get(RIVAL).isDestroyed());
    }

    @Test
    void ownerOfTheCreaturesHittingTheHeartIsTheKiller() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);
        attack(RIVAL, Player.GOOD_PLAYER_ID);

        entityData.removeEntity(hearts.get(RIVAL));
        system.processTick(0.25f);

        assertEquals(Player.GOOD_PLAYER_ID, keepers.get(RIVAL).getKillerId());
    }

    @Test
    void killerIsNotKnownWhenNobodyWasHittingTheHeart() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        entityData.removeEntity(hearts.get(RIVAL));
        system.processTick(0.25f);

        assertEquals(0, keepers.get(RIVAL).getKillerId());
    }

    @Test
    void humanWhoseHeartIsDestroyedLoses() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        entityData.removeEntity(hearts.get(HUMAN));
        system.processTick(0.25f);

        assertEquals(List.of(HUMAN + " lost"), endings);
    }

    @Test
    void destroyedHeartRoomIsLeftInRuins() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        entityData.removeEntity(hearts.get(RIVAL));
        system.processTick(0.25f);

        assertEquals(List.of(heartTile(RIVAL)), ruinedHearts);
    }

    @Test
    void keeperIsDefeatedOnlyOnce() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        entityData.setComponent(hearts.get(HUMAN), new Health(0, 100_000));
        system.processTick(0.25f);
        entityData.removeEntity(hearts.get(HUMAN));
        system.processTick(0.25f);

        assertEquals(List.of(HUMAN + " lost"), endings);
        assertEquals(1, ruinedHearts.size());
    }

    @Test
    void lastKeeperStandingWinsTheSkirmish() {
        KeeperDefeatSystem system = skirmish(HUMAN, RIVAL, OTHER_RIVAL);

        entityData.removeEntity(hearts.get(RIVAL));
        system.processTick(0.25f);
        assertTrue(endings.isEmpty());

        entityData.removeEntity(hearts.get(OTHER_RIVAL));
        system.processTick(0.25f);
        assertEquals(List.of(HUMAN + " won"), endings);
    }

    @Test
    void skirmishTellsAiFromHumanByTheLobbyNotTheMapRecord() {
        KeeperDefeatSystem system = skirmish(HUMAN, RIVAL);
        keepers.get(HUMAN).setPlayer(KeeperDefeatFixtures.player(HUMAN, true));
        keepers.get(RIVAL).setPlayer(KeeperDefeatFixtures.player(RIVAL, false));

        entityData.removeEntity(hearts.get(HUMAN));
        system.processTick(0.25f);

        // The human lost although his map record is an AI one, and the AI rival's win is no result
        assertEquals(List.of(HUMAN + " lost"), endings);
    }

    @Test
    void campaignIsNotWonJustByDestroyingTheOtherKeepers() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        entityData.removeEntity(hearts.get(RIVAL));
        system.processTick(0.25f);

        assertTrue(endings.isEmpty());
        assertTrue(keepers.get(RIVAL).isDestroyed());
    }

    @Test
    void lastEnemyHeartLeavesAPortalGemWhenTheLevelSaysSo() {
        level = KeeperDefeatFixtures.level(GameLevel.LevFlag.HEART_MAKES_GEM);
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL, OTHER_RIVAL);

        entityData.removeEntity(hearts.get(RIVAL));
        system.processTick(0.25f);
        assertTrue(createdObjects.isEmpty());

        entityData.removeEntity(hearts.get(OTHER_RIVAL));
        system.processTick(0.25f);
        assertEquals(List.of(PORTAL_GEM + " for " + Player.GOOD_PLAYER_ID + " at " + heartTile(OTHER_RIVAL)), createdObjects);
    }

    @Test
    void noPortalGemWithoutTheLevelFlag() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        entityData.removeEntity(hearts.get(RIVAL));
        system.processTick(0.25f);

        assertTrue(createdObjects.isEmpty());
    }

    private KeeperDefeatSystem campaign(short... keeperIds) {
        return createSystem(false, keeperIds);
    }

    private KeeperDefeatSystem skirmish(short... keeperIds) {
        return createSystem(true, keeperIds);
    }

    private KeeperDefeatSystem createSystem(boolean skirmish, short... keeperIds) {
        Map<Short, Keeper> players = new HashMap<>();
        for (short keeperId : keeperIds) {
            Keeper keeper = new Keeper(keeperId != HUMAN, keeperId);
            keeper.setPlayer(KeeperDefeatFixtures.player(keeperId, keeperId != HUMAN));
            keeper.setDungeonHeartLocation(heartTile(keeperId));
            keepers.put(keeperId, keeper);
            players.put(keeperId, keeper);

            EntityId heart = entityData.createEntity();
            entityData.setComponents(heart,
                    new ObjectComponent(FiveByFiveRotatedController.OBJECT_HEART_ID, null),
                    new Owner(keeperId, keeperId),
                    new Health(100_000, 100_000));
            hearts.put(keeperId, heart);
        }

        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> "getGameLevel".equals(name) ? level : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> switch (name) {
            case "getLevelData" -> kwdFile;
            case "getPlayer" -> players.get((Short) args[0]);
            case "getPlayers" -> players;
            default -> null;
        });
        IGameController gameController = stub(IGameController.class, (name, args) -> {
            if ("endGame".equals(name)) {
                endings.add(args[0] + ((Boolean) args[1] ? " won" : " lost"));
            }
            return null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> "getRoomControllerByCoordinates".equals(name)
                ? heartRoom((Point) args[0]) : null);
        IObjectsController objectsController = stub(IObjectsController.class, (name, args) -> {
            if ("loadObject".equals(name)) {
                createdObjects.add(args[0] + " for " + args[1] + " at " + new Point((Integer) args[2], (Integer) args[3]));
            }
            return null;
        });

        return new KeeperDefeatSystem(entityData, levelInfo, gameController, mapController, objectsController, skirmish);
    }

    private IRoomController heartRoom(Point tile) {
        return stub(IRoomController.class, (name, args) -> switch (name) {
            case "isDungeonHeart" -> true;
            case "destroy" -> {
                ruinedHearts.add(tile);
                yield null;
            }
            default -> null;
        });
    }

    private void attack(short keeperId, short attackerOwner) {
        EntityId attacker = entityData.createEntity();
        entityData.setComponents(attacker, new AttackTarget(hearts.get(keeperId)), new Owner(attackerOwner, attackerOwner));
    }

    private static Point heartTile(short keeperId) {
        return new Point(keeperId * 10, keeperId * 10);
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    /**
     * A stub that answers what it is told and otherwise runs the default
     * methods of the interface
     */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method.getName(), args);
            if (result == null && method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }

}
