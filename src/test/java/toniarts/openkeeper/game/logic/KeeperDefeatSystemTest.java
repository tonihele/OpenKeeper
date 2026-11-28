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

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.filter.FieldFilter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.GameEnded;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.KeeperComponent;
import toniarts.openkeeper.game.component.KeeperDefeated;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerGoldControl;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperDefeatFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A keeper whose Dungeon Heart is destroyed is defeated: he loses, his heart
 * room is a ruin, his land goes back to nobody, and in a skirmish the last
 * keeper standing wins. From 5 turns after the defeat his imps die and his
 * other creatures leave. The turns are a quarter of a second
 */
class KeeperDefeatSystemTest {

    private static final short HUMAN = Player.KEEPER1_ID;
    private static final short RIVAL = Player.KEEPER2_ID;
    private static final short OTHER_RIVAL = Player.KEEPER3_ID;
    private static final short FOURTH_RIVAL = Player.KEEPER4_ID;
    private static final short PORTAL_GEM = 28;
    private static final int HEART_GOLD = 2000;
    private static final int HEART_CAPACITY = 16000;
    private static final int TREASURY_CAPACITY = 27000;
    private static final short ROCK = 2;
    private static final short DIRT_PATH = 3;
    private static final short CLAIMED_PATH = 8;
    private static final short REINFORCED_WALL = 9;
    private static final short LAIR = 13;

    private EntityData entityData;
    private Map<Short, Keeper> keepers;
    private Map<Short, Keeper> players;
    private Map<Short, EntityId> hearts;
    private Map<Short, IPlayerController> playerControllers;
    private List<String> endings;
    private List<Point> ruinedHearts;
    private List<String> createdObjects;
    private GameLevel level;
    private List<IMapTileController> tiles;
    private Map<Point, IRoomController> rooms;
    private Map<Short, Terrain> terrains;
    private Map<Point, String> alteredTiles;
    private List<String> transferredRooms;
    private Map<EntityId, DefaultStateMachine<ICreatureController, CreatureState>> stateMachines;
    private List<EntityId> leaving;
    private List<EntityId> walking;
    private List<EntityId> noWayOut;
    private double gameTime;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        keepers = new HashMap<>();
        players = new HashMap<>();
        hearts = new HashMap<>();
        playerControllers = new HashMap<>();
        endings = new ArrayList<>();
        ruinedHearts = new ArrayList<>();
        createdObjects = new ArrayList<>();
        level = KeeperDefeatFixtures.level();
        tiles = new ArrayList<>();
        rooms = new HashMap<>();
        terrains = Map.of(
                ROCK, KeeperDefeatFixtures.terrain(ROCK, DIRT_PATH, Terrain.TerrainFlag.SOLID),
                CLAIMED_PATH, KeeperDefeatFixtures.terrain(CLAIMED_PATH, DIRT_PATH, Terrain.TerrainFlag.OWNABLE),
                REINFORCED_WALL, KeeperDefeatFixtures.terrain(REINFORCED_WALL, ROCK, Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.OWNABLE));
        alteredTiles = new HashMap<>();
        transferredRooms = new ArrayList<>();
        stateMachines = new HashMap<>();
        leaving = new ArrayList<>();
        walking = new ArrayList<>();
        noWayOut = new ArrayList<>();
    }

    @Test
    void defeatedKeepersLandGoesBackToNobody() {
        tile(1, 1, RIVAL, REINFORCED_WALL);
        tile(2, 1, RIVAL, CLAIMED_PATH);
        tile(3, 1, RIVAL, ROCK);
        tile(1, 2, HUMAN, REINFORCED_WALL);
        room("rival's lair", 4, 1, RIVAL);
        room("human's lair", 4, 2, HUMAN);
        // The ruined heart stays his
        tile(heartTile(RIVAL).x, heartTile(RIVAL).y, RIVAL, LAIR);
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        entityData.removeEntity(hearts.get(RIVAL));
        system.processTick(0.25f);

        String neutral = " for " + Player.NEUTRAL_PLAYER_ID;
        assertEquals(Map.of(
                new Point(1, 1), "terrain " + ROCK + neutral,
                new Point(2, 1), "terrain " + DIRT_PATH + neutral,
                new Point(3, 1), "terrain " + ROCK + neutral), alteredTiles);
        assertEquals(List.of("rival's lair to " + Player.NEUTRAL_PLAYER_ID), transferredRooms);
    }

    @Test
    void defeatedKeepersImpsDieOnEveryTurnFromFiveTurnsAfterTheDefeat() {
        EntityId imp = creature(RIVAL, true);
        EntityId fighter = creature(RIVAL, false);
        EntityId humansImp = creature(HUMAN, true);
        EntityId prisoner = creature(RIVAL, true);
        entityData.setComponent(prisoner, new CreatureImprisoned(0, 0));
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        defeat(system, RIVAL);
        for (int turn = 1; turn < 5; turn++) {
            turn(system);
            assertNull(entityData.getComponent(imp, Damage.class), "turn " + turn);
        }
        turn(system);

        assertEquals(300, entityData.getComponent(imp, Damage.class).damage);
        assertNull(entityData.getComponent(fighter, Damage.class));
        assertNull(entityData.getComponent(humansImp, Damage.class));
        assertNull(entityData.getComponent(prisoner, Damage.class));

        // An imp the keeper gets later dies on the next turn
        EntityId newImp = creature(RIVAL, true);
        turn(system);
        assertEquals(300, entityData.getComponent(newImp, Damage.class).damage);
    }

    @Test
    void defeatedKeepersOtherCreaturesLeaveTheDungeon() {
        EntityId fighter = creature(RIVAL, false);
        EntityId walledIn = creature(RIVAL, false);
        noWayOut.add(walledIn);
        EntityId prisoner = creature(RIVAL, false);
        entityData.setComponent(prisoner, new CreatureImprisoned(0, 0));
        EntityId imp = creature(RIVAL, true);
        EntityId humans = creature(HUMAN, false);
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        turn(system);
        assertTrue(leaving.isEmpty());

        defeat(system, RIVAL);
        for (int turn = 1; turn < 5; turn++) {
            turn(system);
            assertTrue(leaving.isEmpty(), "turn " + turn);
        }
        turn(system);

        assertEquals(List.of(fighter), leaving);
        assertEquals(CreatureState.WORK, stateMachines.get(fighter).getCurrentState());

        // With no portal to get to, it walks about, and tries again when the walk ends
        assertEquals(CreatureState.WANDER, stateMachines.get(walledIn).getCurrentState());
        assertEquals(List.of(walledIn), walking);
        turn(system);
        assertEquals(List.of(walledIn), walking);
        walking.clear();
        noWayOut.clear();
        turn(system);
        assertEquals(List.of(fighter, walledIn), leaving);

        // Prisoners, workers and the other keepers' creatures are left alone
        assertNull(stateMachines.get(prisoner));
        assertNull(stateMachines.get(imp));
        assertNull(stateMachines.get(humans));
    }

    @Test
    void defeatedKeepersCreatureIsSentAwayAgainWhenItTurnsToSomethingElse() {
        EntityId fighter = creature(RIVAL, false);
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);
        defeat(system, RIVAL);
        for (int turn = 1; turn <= 5; turn++) {
            turn(system);
        }

        // In the middle of a swing it is left alone, back in the fight it is sent away again
        leaving.clear();
        stateMachines.get(fighter).changeState(CreatureState.MELEE_ATTACK);
        turn(system);
        assertTrue(leaving.isEmpty());

        stateMachines.get(fighter).changeState(CreatureState.FIGHT);
        turn(system);
        assertEquals(List.of(fighter), leaving);
        assertEquals(CreatureState.WORK, stateMachines.get(fighter).getCurrentState());
    }

    @Test
    void defeatedKeeperIsDoneWithOnceItsCreaturesAreGone() {
        EntityId fighter = creature(RIVAL, false);
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);
        defeat(system, RIVAL);
        for (int turn = 1; turn <= 5; turn++) {
            turn(system);
        }
        EntityId defeat = entityData.findEntity(null, KeeperDefeated.class);
        assertFalse(entityData.getComponent(defeat, KeeperDefeated.class).creaturesGone);

        // Once the creature has left, the keeper's creatures are no longer looked for
        entityData.removeEntity(fighter);
        turn(system);
        assertTrue(entityData.getComponent(defeat, KeeperDefeated.class).creaturesGone);
    }

    /**
     * Destroys the keeper's heart and plays the turn of the defeat
     */
    private void defeat(KeeperDefeatSystem system, short keeperId) {
        entityData.removeEntity(hearts.get(keeperId));
        turn(system);
    }

    private void turn(KeeperDefeatSystem system) {
        gameTime += 0.25;
        system.processTick(0.25f);
    }

    private EntityId creature(short ownerId, boolean worker) {
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.worker = worker;
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, creatureComponent, new Owner(ownerId, ownerId), new Health(300, 400));

        return entityId;
    }

    @Test
    void keeperWhoseHeartIsGoneIsDestroyed() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        gameTime = 60;
        defeat(system, RIVAL);

        assertTrue(keepers.get(RIVAL).isDestroyed());
        assertFalse(keepers.get(HUMAN).isDestroyed());

        // The time of the defeat is kept with the game
        List<KeeperDefeated> defeats = entityData.getEntities(KeeperDefeated.class).stream()
                .map(defeat -> defeat.get(KeeperDefeated.class)).toList();
        assertEquals(1, defeats.size());
        assertEquals(RIVAL, defeats.getFirst().playerId);
        assertEquals(60.25, defeats.getFirst().time, 0.0001);
    }

    @Test
    void heartWithNoHealthLeftDestroysTheKeeper() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        entityData.setComponent(hearts.get(RIVAL), new Health(0, 100_000));
        system.processTick(0.25f);

        assertTrue(keepers.get(RIVAL).isDestroyed());
    }

    @Test
    void heartAddedWithNoHealthDestroysTheKeeper() {
        KeeperDefeatSystem system = campaign(HUMAN);
        Keeper rival = new Keeper(true, RIVAL);
        players.put(RIVAL, rival);

        EntityId heart = entityData.createEntity();
        entityData.setComponents(heart,
                new ObjectComponent(FiveByFiveRotatedController.OBJECT_HEART_ID, null),
                new Owner(RIVAL, RIVAL),
                new Health(0, 100_000));
        system.processTick(0.25f);

        assertTrue(rival.isDestroyed());
        assertFalse(keepers.get(HUMAN).isDestroyed());
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
    void goldInTheRuinedHeartIsNoLongerTheKeepers() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);

        entityData.removeEntity(hearts.get(RIVAL));
        system.processTick(0.25f);

        // Only the gold in the treasury is left, and only the treasury stores gold
        assertEquals(3000, playerControllers.get(RIVAL).getGoldControl().getGold());
        assertEquals(TREASURY_CAPACITY, playerControllers.get(RIVAL).getGoldControl().getGoldMax());
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
    void lastAiKeeperWinsAnAiOnlySkirmish() {
        KeeperDefeatSystem system = skirmish(RIVAL, OTHER_RIVAL);

        entityData.removeEntity(hearts.get(OTHER_RIVAL));
        system.processTick(0.25f);

        assertEquals(List.of(RIVAL + " won"), endings);
    }

    @Test
    void alliedLastKeepersEachReceiveTheSkirmishWin() {
        KeeperDefeatSystem system = skirmish(HUMAN, RIVAL, OTHER_RIVAL, FOURTH_RIVAL);
        keepers.get(HUMAN).createAlliance(RIVAL);
        keepers.get(RIVAL).createAlliance(HUMAN);

        entityData.removeEntity(hearts.get(OTHER_RIVAL));
        system.processTick(0.25f);
        assertTrue(endings.isEmpty());

        entityData.removeEntity(hearts.get(FOURTH_RIVAL));
        system.processTick(0.25f);

        assertEquals(2, endings.size());
        assertEquals(Set.of(HUMAN + " won", RIVAL + " won"), new HashSet<>(endings));
    }

    @Test
    void oneSidedAllianceDoesNotEndTheMatch() {
        KeeperDefeatSystem system = skirmish(HUMAN, RIVAL, OTHER_RIVAL);
        keepers.get(HUMAN).createAlliance(RIVAL);

        entityData.removeEntity(hearts.get(OTHER_RIVAL));
        system.processTick(0.25f);

        assertTrue(endings.isEmpty());
    }

    @Test
    void keeperWhoWinsTheSkirmishMakesEveryKeeperButHisAlliesLose() {
        KeeperDefeatSystem system = skirmish(HUMAN, RIVAL, OTHER_RIVAL, FOURTH_RIVAL);
        keepers.get(HUMAN).createAlliance(RIVAL);
        keepers.get(RIVAL).createAlliance(HUMAN);
        keepers.get(HUMAN).createAlliance(OTHER_RIVAL); // one sided, so not an ally

        // The level script or a cheat ends the human's game with a win
        entityData.setComponent(entityData.findEntity(new FieldFilter<>(KeeperComponent.class, "playerId", HUMAN), KeeperComponent.class),
                new GameEnded(true));
        system.processTick(0.25f);

        // The ally doesn't win from it
        assertEquals(Set.of(OTHER_RIVAL + " lost", FOURTH_RIVAL + " lost"), new HashSet<>(endings));
        assertEquals(2, endings.size());
    }

    @Test
    void keeperWhoLosesTheSkirmishEndsNobodyElsesGameWhileRivalsAreLeft() {
        KeeperDefeatSystem system = skirmish(HUMAN, RIVAL, OTHER_RIVAL);
        entityData.setComponent(entityData.findEntity(new FieldFilter<>(KeeperComponent.class, "playerId", HUMAN), KeeperComponent.class),
                new GameEnded(false));
        system.processTick(0.25f);
        assertTrue(endings.isEmpty());
    }

    @Test
    void keeperWhoLosesTheSkirmishLeavesTheLastSideStandingTheWinners() {
        KeeperDefeatSystem system = skirmish(HUMAN, RIVAL, OTHER_RIVAL);
        keepers.get(RIVAL).createAlliance(OTHER_RIVAL);
        keepers.get(OTHER_RIVAL).createAlliance(RIVAL);

        // The level script ends the human's game with a loss, the two allies left win together (0x4BE630)
        entityData.setComponent(entityData.findEntity(new FieldFilter<>(KeeperComponent.class, "playerId", HUMAN), KeeperComponent.class),
                new GameEnded(false));
        system.processTick(0.25f);

        assertEquals(Set.of(RIVAL + " won", OTHER_RIVAL + " won"), new HashSet<>(endings));
        assertEquals(2, endings.size());
    }

    @Test
    void keeperWhoWinsTheCampaignLevelEndsNobodyElsesGame() {
        KeeperDefeatSystem system = campaign(HUMAN, RIVAL);
        entityData.setComponent(entityData.findEntity(new FieldFilter<>(KeeperComponent.class, "playerId", HUMAN), KeeperComponent.class),
                new GameEnded(true));
        system.processTick(0.25f);
        assertTrue(endings.isEmpty());
    }

    @Test
    void skirmishTellsAiFromHumanByTheLobbyNotTheMapRecord() {
        KeeperDefeatSystem system = skirmish(HUMAN, RIVAL);
        keepers.get(HUMAN).setPlayer(KeeperDefeatFixtures.player(HUMAN, true));
        keepers.get(RIVAL).setPlayer(KeeperDefeatFixtures.player(RIVAL, false));

        entityData.removeEntity(hearts.get(HUMAN));
        system.processTick(0.25f);

        // The human lost although his map record is an AI one, and the AI rival wins
        assertEquals(List.of(HUMAN + " lost", RIVAL + " won"), endings);
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
        for (short keeperId : keeperIds) {
            Keeper keeper = new Keeper(keeperId != HUMAN, keeperId);
            keeper.setPlayer(KeeperDefeatFixtures.player(keeperId, keeperId != HUMAN));
            keeper.setDungeonHeartLocation(heartTile(keeperId));
            keepers.put(keeperId, keeper);
            players.put(keeperId, keeper);

            entityData.setComponents(entityData.createEntity(), new KeeperComponent(keeperId), new Gold(0, Integer.MAX_VALUE));
            PlayerGoldControl goldControl = new PlayerGoldControl(keeper, entityData);
            goldControl.setGoldMax(HEART_CAPACITY + TREASURY_CAPACITY);
            goldControl.addGold(HEART_GOLD + 3000);
            playerControllers.put(keeperId, stub(IPlayerController.class, (name, args) -> "getGoldControl".equals(name) ? goldControl : null));

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
        ICreaturesController creaturesController = stub(ICreaturesController.class,
                (name, args) -> "createController".equals(name) ? creatureController((EntityId) args[0]) : null);
        IGameWorldController gameWorldController = stub(IGameWorldController.class,
                (name, args) -> "getCreaturesController".equals(name) ? creaturesController : null);
        ITaskManager taskManager = stub(ITaskManager.class, (name, args) -> {
            if (!"assignTask".equals(name) || args[1] != Creature.JobType.LEAVE
                    || noWayOut.contains(((ICreatureController) args[0]).getEntityId())) {
                return null;
            }
            leaving.add(((ICreatureController) args[0]).getEntityId());
            return true;
        });
        IGameController gameController = stub(IGameController.class, (name, args) -> switch (name) {
            case "endGame" -> {
                endings.add(args[0] + ((Boolean) args[1] ? " won" : " lost"));
                yield null;
            }
            case "getPlayerControllers" -> playerControllers;
            case "getGameWorldController" -> gameWorldController;
            case "getTaskManager" -> taskManager;
            case "getGameTimer" -> (IGameTimer) () -> gameTime;
            default -> null;
        });
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (name, args) -> "iterator".equals(name) ? tiles.iterator() : null);
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getRoomControllerByCoordinates" -> keepers.keySet().stream().anyMatch(k -> heartTile(k).equals(args[0]))
                    ? heartRoom((Point) args[0]) : rooms.get((Point) args[0]);
            case "getMapData" -> mapData;
            case "getTerrain" -> terrains.get(((IMapTileInformation) args[0]).getTerrainId());
            case "alterTerrain" -> {
                alteredTiles.put((Point) args[0], "terrain " + args[1] + " for " + args[2]);
                yield null;
            }
            case "transferRoom" -> {
                transferredRooms.add(args[0] + " to " + args[1]);
                yield null;
            }
            default -> null;
        });
        IObjectsController objectsController = stub(IObjectsController.class, (name, args) -> {
            if ("loadObject".equals(name)) {
                createdObjects.add(args[0] + " for " + args[1] + " at " + new Point((Integer) args[2], (Integer) args[3]));
            }
            return null;
        });

        return new KeeperDefeatSystem(entityData, levelInfo, gameController, mapController, objectsController, skirmish);
    }

    /**
     * A creature with a real state machine that is on the leave job once the
     * task manager gave it one, and walks about when it is told to
     */
    private ICreatureController creatureController(EntityId entity) {
        Task leave = stub(Task.class, (name, args) -> "getTaskType".equals(name) ? TaskType.LEAVE : null);
        ICreatureController[] creature = new ICreatureController[1];
        creature[0] = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getEntityId" -> entity;
            case "getStateMachine" -> stateMachines.computeIfAbsent(entity,
                    e -> new DefaultStateMachine<>(creature[0], CreatureState.IDLE));
            case "getAssignedTask" -> leaving.contains(entity) ? leave : null;
            case "isStopped" -> !walking.contains(entity);
            case "navigateToRandomPoint" -> {
                walking.add(entity);
                yield null;
            }
            default -> null;
        });
        return creature[0];
    }

    private IRoomController heartRoom(Point tile) {
        return stub(IRoomController.class, (name, args) -> switch (name) {
            case "isDungeonHeart" -> true;
            case "getUsedCapacity" -> HEART_GOLD;
            case "getMaxCapacity" -> HEART_CAPACITY;
            case "destroy" -> {
                ruinedHearts.add(tile);
                yield null;
            }
            default -> null;
        });
    }

    private void tile(int x, int y, short ownerId, short terrainId) {
        Point location = new Point(x, y);
        tiles.add(stub(IMapTileController.class, (name, args) -> switch (name) {
            case "getLocation" -> location;
            case "getOwnerId" -> ownerId;
            case "getTerrainId" -> terrainId;
            default -> null;
        }));
    }

    private void room(String roomName, int x, int y, short ownerId) {
        tile(x, y, ownerId, LAIR);
        rooms.put(new Point(x, y), stub(IRoomController.class, (name, args) -> "toString".equals(name) ? roomName : null));
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
            switch (method.getName()) {
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "equals" -> {
                    return proxy == args[0];
                }
            }
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
