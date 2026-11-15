/*
 * Copyright (C) 2014-2019 OpenKeeper
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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.KeeperAi;
import toniarts.openkeeper.game.component.KeeperAiDropped;
import toniarts.openkeeper.game.component.KeeperAiEnemy;
import toniarts.openkeeper.game.component.KeeperAiNoPrisoner;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.controller.player.PlayerHandControl;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.player.PlayerSpellControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.AI;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.ai;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.battleSpell;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.creatureWithJobs;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.player;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withAttackResponse;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withBattleSpells;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withBuildAreaEndX;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withCallToArms;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withInvasion;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withNeutralRoomRange;
import static toniarts.openkeeper.tools.convert.map.AiFixtures.withRetreatHealth;
import toniarts.openkeeper.tools.convert.map.CombatExperienceFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import static toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures.variable;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import static toniarts.openkeeper.tools.convert.map.KeeperSpellFixtures.keeperSpell;
import static toniarts.openkeeper.tools.convert.map.PassiveExperienceFixtures.creature;
import static toniarts.openkeeper.tools.convert.map.PayDayFixtures.gameLevel;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The computer player on a small map: a 3 x 3 Dungeon Heart (H) with claimed
 * path (.) around it, in rock (#) with a gold seam (G), inside impenetrable
 * rock (X)
 */
class KeeperAiSystemTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final short CALL_TO_ARMS = 3;
    private static final short TREASURY = 1;
    private static final short LAIR = 2;
    private static final short PORTAL = 3;
    private static final short HATCHERY = 4;
    private static final short DUNGEON_HEART = 5;
    private static final short LIBRARY = 6;
    private static final short TRAINING_ROOM = 7;
    private static final short GUARD_ROOM = 9;
    private static final short PRISON = 11;
    private static final short TORTURE_CHAMBER = 12;
    private static final short TEMPLE = 13;
    private static final short COMBAT_PIT = 16;
    private static final short HORNY = 9;
    private static final short EATER = 8;
    private static final short THUNDERBOLT = 6;
    private static final short HEAL = 10;
    private static final int ROOM_COST = 10;
    private static final Point HEART = new Point(3, 6);
    private static final int NORTH = 0;
    private static final int WEST = 1;
    private static final int EAST = 3;

    private final char[][] map = {
        "XXXXXXXXXXXXXXXX".toCharArray(),
        "X##############X".toCharArray(),
        "X##############X".toCharArray(),
        "X##############X".toCharArray(),
        "X.....#########X".toCharArray(),
        "X.HHH.#########X".toCharArray(),
        "X.HHH.####GG###X".toCharArray(),
        "X.HHH.#########X".toCharArray(),
        "X.....#########X".toCharArray(),
        "X##############X".toCharArray(),
        "XXXXXXXXXXXXXXXX".toCharArray()};
    private final Map<Character, Terrain> terrains = Map.of(
            'X', terrain((short) 1, flags(TerrainFlag.SOLID, TerrainFlag.IMPENETRABLE), 0, 0, 0, (short) 0),
            '#', terrain((short) 2, flags(TerrainFlag.SOLID, TerrainFlag.TAGGABLE), 0, 0, 0, (short) 0),
            'G', terrain((short) 3, flags(TerrainFlag.SOLID, TerrainFlag.TAGGABLE), 100, 0, 0, (short) 0),
            '.', terrain((short) 4, flags(TerrainFlag.OWNABLE), 0, 0, 0, (short) 0),
            'H', terrain((short) 5, flags(TerrainFlag.OWNABLE, TerrainFlag.ROOM), 0, 0, 0, (short) 0),
            'P', terrain((short) 6, flags(TerrainFlag.OWNABLE, TerrainFlag.ROOM), 0, 0, 0, (short) 0),
            'E', terrain((short) 7, flags(TerrainFlag.OWNABLE), 0, 0, 0, (short) 0));
    private final Set<Point> tagged = new HashSet<>();
    private final List<Point> built = new ArrayList<>();
    private final List<Point> impSpells = new ArrayList<>();

    private final Map<Short, Room> rooms = Map.ofEntries(
            Map.entry(TREASURY, room(TREASURY, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE)),
            Map.entry(LAIR, room(LAIR, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE)),
            Map.entry(PORTAL, room(PORTAL, 0, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(PRISON, room(PRISON, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(HATCHERY, room(HATCHERY, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE)),
            Map.entry(DUNGEON_HEART, room(DUNGEON_HEART, 0, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(GUARD_ROOM, room(GUARD_ROOM, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.BUILDABLE)),
            Map.entry(LIBRARY, room(LIBRARY, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(TRAINING_ROOM, room(TRAINING_ROOM, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(TORTURE_CHAMBER, room(TORTURE_CHAMBER, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(TEMPLE, room(TEMPLE, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND)),
            Map.entry(COMBAT_PIT, room(COMBAT_PIT, ROOM_COST, Room.RoomFlag.PLACEABLE_ON_LAND)));
    private final KeeperSpell createImp = keeperSpell("Create Imp", 100, KeeperSpell.KeeperSpellFlag.CREATE_WORKER);
    private final Creature imp = creature((short) 1);
    private final EntityData entityData = new DefaultEntityData();
    private final IMapController mapController = createMapController();
    private Keeper keeper;
    private PlayerRoomControl roomControl;
    private PlayerCreatureControl creatureControl;
    private double gameTime;
    private boolean logDecisions;
    private AI.AIType lobbyType;
    private PlayerHandControl handControl;
    private PlayerSpellControl spellControl;
    private final KeeperSpell callToArms = keeperSpell(CALL_TO_ARMS, 200, 0, (short) 0, 0);
    private final List<Point> callToArmsCasts = new ArrayList<>();
    private int callToArmsCancels;
    private final Map<EntityId, Point> dropped = new HashMap<>();
    private final List<IRoomController> neutralRooms = new ArrayList<>();
    private final KeeperSpell thunderbolt = battleSpell(THUNDERBOLT, 100, KeeperSpell.KeeperSpellFlag.ATTACKING);
    private final KeeperSpell heal = battleSpell(HEAL, 100, KeeperSpell.KeeperSpellFlag.DEFENSIVE);
    private final Map<Short, EntityId> battleCasts = new HashMap<>();
    private PlayerRoomControl enemyRoomControl;
    private boolean reachable;

    @Test
    void digsToANeutralPortalNearItsRoomsWhenThereIsNoRoomToBuild() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        createNeutralPortal(new Point(10, 3));

        Set<Point> dug = new HashSet<>();
        for (int tick = 0; tick < 100 && (tick == 0 || getKeeperAi().task == KeeperAi.Task.DIG_PATH); tick++) {
            system.processTick(0.25f);
            if (tick == 0) {
                assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
                assertEquals(new Point(10, 3), getKeeperAi().target);
            }

            // The imps dig out and claim what is tagged
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            dug.addAll(tagged);
            tagged.clear();
        }

        // From the heart to the portal, and that is all
        assertEquals(Set.of(new Point(6, 6), new Point(7, 6), new Point(7, 5), new Point(8, 5), new Point(8, 4), new Point(9, 4), new Point(9, 3)), dug);
        assertEquals(KeeperAi.Task.FINISH, getKeeperAi().task);
    }

    @Test
    void plansAPortalAfterEachRoomSoARoomWithoutASiteDoesNotKeepItFromThePortal() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
        createNeutralPortal(new Point(10, 3));

        system.processTick(0.25f);
        assertEquals(KeeperAi.Task.FIND_ROOM_SITE, getKeeperAi().task);
        assertEquals(TREASURY, getKeeperAi().roomId);

        // The search for a site gave up
        planAgain();
        system.processTick(0.25f);
        assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertEquals(new Point(10, 3), getKeeperAi().target);

        planAgain();
        system.processTick(0.25f);
        assertEquals(KeeperAi.Task.FIND_ROOM_SITE, getKeeperAi().task);
        assertEquals(TREASURY, getKeeperAi().roomId);
    }

    @Test
    void digsToAPortalFarFromItsRoomsOnlyFromLandItHasNearThePortal() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 5, 1), 1000);
        createNeutralPortal(new Point(10, 3));

        system.processTick(0.25f);
        assertEquals(KeeperAi.Task.PLAN, getKeeperAi().task);

        // Land of its own two tiles from the portal
        map[3][8] = '.';
        system.processTick(0.25f);
        assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertEquals(new Point(8, 3), getKeeperAi().current);
    }

    @Test
    void leavesAPortalOutsideItsBuildAreaAlone() {
        KeeperAiSystem system = createSystem(withBuildAreaEndX(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 9), 1000);
        createNeutralPortal(new Point(10, 3));

        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.PLAN, getKeeperAi().task);
    }

    @Test
    void digsAPathRoundRockThatCanNotBeDug() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        createNeutralPortal(new Point(10, 6));
        map[5][8] = 'X';
        map[6][8] = 'X';
        map[7][8] = 'X';

        Set<Point> dug = new HashSet<>();
        for (int tick = 0; tick < 100 && (tick == 0 || getKeeperAi().task == KeeperAi.Task.DIG_PATH); tick++) {
            system.processTick(0.25f);
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            dug.addAll(tagged);
            tagged.clear();
        }

        assertEquals(Set.of(new Point(6, 6), new Point(7, 6), new Point(7, 7), new Point(7, 8), new Point(8, 8), new Point(9, 8), new Point(9, 7), new Point(9, 6)), dug);
        assertEquals(new Point(10, 6), getKeeperAi().current);
    }

    @Test
    void givesUpAPathToAPlaceWalledInByRockThatCanNotBeDug() {
        KeeperAiSystem system = createSystem(withNeutralRoomRange(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 15, 1), 1000);
        createNeutralPortal(new Point(10, 6));
        for (int y = 1; y < 10; y++) {
            map[y][8] = 'X';
        }

        for (int tick = 0; tick < 20; tick++) {
            system.processTick(0.25f);
            assertTrue(tagged.isEmpty());
        }
    }

    @Test
    void dropsRestedFightersOnAnAttackUntilTheirThreatMakesUpWhatItNeeds() {
        KeeperAiSystem system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 175, true, 0), 1000);
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT)));
        createCreature(ENEMY_ID, new Point(8, 4), 100, 100, CreatureState.IDLE);
        EntityId horny = createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);
        CreatureComponent hornyComponent = new CreatureComponent();
        hornyComponent.creatureId = HORNY;
        entityData.setComponent(horny, hornyComponent);
        EntityId worker = createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);
        CreatureComponent workerComponent = new CreatureComponent();
        workerComponent.worker = true;
        entityData.setComponent(worker, workerComponent);
        createCreature(PLAYER_ID, new Point(12, 8), 50, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.SLEEPING);
        EntityId first = createCreature(PLAYER_ID, new Point(12, 8), 51, 100, CreatureState.IDLE);
        EntityId second = createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.WORK);
        EntityId third = createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);

        // 175 % of the enemy's 200 less its own 100 near the fight, and the first one is dropped at once
        system.processTick(0.25f);
        assertEquals(1, getKeeperAi().attacks);
        assertEquals(150, getKeeperAi().attackNeeds[0]);
        assertEquals(ENEMY_ID, getKeeperAi().attackerId);
        for (int tick = 0; tick < 10; tick++) {
            system.processTick(0.25f);
        }

        // The sleeper counts as rested and goes first, then the next two
        assertEquals(3, dropped.size());
        assertEquals(new Point(5, 4), dropped.get(first));
        assertTrue(dropped.containsKey(second) || dropped.containsKey(third));
        assertTrue(!dropped.containsKey(horny) && !dropped.containsKey(worker) && !dropped.containsKey(fighter));
        assertEquals(0, getKeeperAi().attacks);
    }

    @Test
    void leavesItsCreaturesInAnEnemyPrisonAlone() {
        KeeperAiSystem system = createSystem(withRetreatHealth(ai(4, 10, 9, 1, 50, 0, List.of()), 20), 1000);
        EntityId prisoner = createCreature(PLAYER_ID, new Point(8, 3), 5, 100, CreatureState.IMPRISONED);
        entityData.setComponent(prisoner, new Owner(PLAYER_ID, ENEMY_ID));
        EntityId stunned = createCreature(PLAYER_ID, new Point(8, 3), 5, 100, CreatureState.STUNNED);

        system.processTick(0.25f);

        // The turn went to an imp
        assertTrue(dropped.isEmpty());
        assertEquals(1, impSpells.size());
        assertNull(entityData.getComponent(stunned, KeeperAiDropped.class));
    }

    @Test
    void healsAHurtCreatureNearAFightWhenItDefends() {
        KeeperAiSystem system = createSystem(withBattleSpells(ai(0, 0, 9, 1, 50, 0, List.of()), 0, 4, 0), 1000);
        spellControl.setTypeAvailable(heal, true, true);
        spellControl.setTypeAvailable(thunderbolt, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 60, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT)));
        createCreature(PLAYER_ID, new Point(12, 9), 10, 100, CreatureState.IDLE);
        EntityId hurt = createCreature(PLAYER_ID, new Point(4, 4), 49, 100, CreatureState.IDLE);

        system.processTick(0.25f);

        // Not the fighter with more than half its health, nor the one far from the fight
        assertEquals(Map.of(HEAL, hurt), battleCasts);
    }

    @Test
    void strikesAnEnemyNearAFightWhenItAttacksAndTheSpellMakesItsRoll() {
        KeeperAiSystem system = createSystem(withBattleSpells(ai(0, 0, 9, 1, 50, 0, List.of()), 0, 0, 4), 1000);
        spellControl.setTypeAvailable(heal, true, true);
        spellControl.setTypeAvailable(thunderbolt, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 40, 100, CreatureState.FIGHT);
        EntityId enemy = createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(enemy));
        createCreature(ENEMY_ID, new Point(12, 9), 100, 100, CreatureState.IDLE);

        system.processTick(0.25f);

        assertEquals(Map.of(THUNDERBOLT, enemy), battleCasts);
    }

    @Test
    void castsNoSpellInAFightWhenToldNotToOrWhenTheSpellIsTooCheapForIt() {
        KeeperAiSystem system = createSystem(withBattleSpells(ai(0, 0, 9, 1, 50, 0, List.of()), 4, 0, 4), 1000);
        spellControl.setTypeAvailable(thunderbolt, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 40, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT)));

        system.processTick(0.25f);
        assertTrue(battleCasts.isEmpty());

        // The die of a spell of 100 mana has two sides, that is never the 4 it wants
        entityData.removeEntity(entityData.findEntity(null, KeeperAi.class));
        system = createSystem(withBattleSpells(ai(0, 0, 9, 1, 50, 0, List.of()), 0, 0, 0), 1000);
        spellControl.setTypeAvailable(thunderbolt, true, true);
        system.processTick(0.25f);
        assertTrue(battleCasts.isEmpty());
    }

    @Test
    void dropsACreatureLeftInItsHandInTheRoomForAJobItLikes() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(HATCHERY, new Point(4, 8)));
        roomControl.onBuild(createRoom(LIBRARY, new Point(2, 4)));
        EntityId creature = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = EATER;
        entityData.setComponent(creature, creatureComponent);
        handControl.push(creature);

        system.processTick(0.25f);

        // DK II's computer player takes no creature to the Hatchery
        assertEquals(Map.of(creature, new Point(2, 4)), dropped);
    }

    @Test
    void dropsWhatElseIsLeftInItsHandAtTheDungeonHeart() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(LAIR, new Point(2, 4)));
        EntityId creature = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        handControl.push(creature);

        system.processTick(0.25f);

        assertEquals(Map.of(creature, HEART), dropped);
    }

    @Test
    void anAttackToAnswerTakesTheWholeTurn() {
        KeeperAiSystem system = createSystem(withAttackResponse(ai(4, 10, 9, 1, 50, 0, List.of()), 175, true, 0), 1000);
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 100, 100, CreatureState.WORK);
        createCreature(PLAYER_ID, new Point(12, 8), 100, 100, CreatureState.IDLE);
        entityData.setComponent(createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT), new AttackTarget(fighter));

        system.processTick(0.25f);

        assertEquals(1, dropped.size());
        assertTrue(impSpells.isEmpty());
    }

    @Test
    void carriesItsCreaturesHomeFromAFightItIsLosingAwayFromItsLand() {
        KeeperAiSystem system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 175, true, 0), 1000);
        EntityId fighter = createCreature(PLAYER_ID, new Point(12, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(13, 3), 100, 100, CreatureState.FIGHT)));
        createCreature(ENEMY_ID, new Point(13, 4), 100, 100, CreatureState.FIGHT);
        EntityId near = createCreature(PLAYER_ID, new Point(10, 3), 100, 100, CreatureState.IDLE);
        EntityId far = createCreature(PLAYER_ID, new Point(12, 9), 100, 100, CreatureState.IDLE);

        system.processTick(0.25f);
        assertEquals(-100, getKeeperAi().attackNeeds[0]);
        for (int tick = 0; tick < 10; tick++) {
            system.processTick(0.25f);
        }

        // Every creature it has within 5 tiles of the fight, the others stay
        assertEquals(Map.of(fighter, HEART, near, HEART), dropped);
        assertTrue(!dropped.containsKey(far));
        assertEquals(0, getKeeperAi().attacks);
    }

    @Test
    void leavesAFightAwayFromItsLandAloneWhenItIsNotOutnumberedOrDoesNotPullOut() {
        KeeperAiSystem system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 100, true, 0), 1000);
        EntityId fighter = createCreature(PLAYER_ID, new Point(12, 3), 100, 100, CreatureState.FIGHT);
        EntityId enemy = createCreature(ENEMY_ID, new Point(13, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(enemy));

        system.processTick(0.25f);
        assertEquals(0, getKeeperAi().attacks);

        // Outnumbered now, but told not to pull out
        entityData.removeEntity(entityData.findEntity(null, KeeperAi.class));
        system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 175, false, 0), 1000);
        system.processTick(0.25f);
        assertEquals(0, getKeeperAi().attacks);
        assertTrue(dropped.isEmpty());
    }

    @Test
    void decidesOnAnAttackForEachEnemyCreatureWhetherToTakeItPrisonerWhenItHasAPrison() {
        KeeperAiSystem system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 0, false, 0), 1000);
        EntityId fighter = createCreature(PLAYER_ID, new Point(5, 4), 100, 100, CreatureState.FIGHT);
        EntityId enemy = createCreature(ENEMY_ID, new Point(6, 4), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(enemy));

        // No Prison, no decision
        system.processTick(0.25f);
        assertNull(entityData.getComponent(enemy, KeeperAiNoPrisoner.class));

        // It never takes prisoners
        roomControl.onBuild(createRoom(PRISON, new Point(1, 4)));
        for (int tick = 0; tick < 41; tick++) {
            system.processTick(0.25f);
        }
        assertEquals(1 << PLAYER_ID, entityData.getComponent(enemy, KeeperAiNoPrisoner.class).players);

        // It always takes prisoners
        entityData.removeEntity(entityData.findEntity(null, KeeperAi.class));
        system = createSystem(withAttackResponse(ai(0, 0, 9, 1, 50, 0, List.of()), 0, false, 100), 1000);
        roomControl.onBuild(createRoom(PRISON, new Point(1, 4)));
        system.processTick(0.25f);
        assertEquals(0, entityData.getComponent(enemy, KeeperAiNoPrisoner.class).players);
    }

    private void createNeutralPortal(Point centre) {
        IRoomController portal = createRoom(PORTAL, centre);
        portal.getRoomInstance().setOwnerId(Player.NEUTRAL_PLAYER_ID);
        neutralRooms.add(portal);
        map[centre.y][centre.x] = 'P';
    }

    private void planAgain() {
        KeeperAi keeperAi = new KeeperAi(getKeeperAi());
        keeperAi.task = KeeperAi.Task.PLAN;
        entityData.setComponent(entityData.findEntity(null, KeeperAi.class), keeperAi);
    }

    @Test
    void carriesACreatureThatIsBadlyHurtInAFightToTheLair() {
        KeeperAiSystem system = createSystem(withRetreatHealth(ai(4, 10, 9, 1, 50, 0, List.of()), 20), 1000);
        roomControl.onBuild(createRoom(LAIR, new Point(2, 4)));
        EntityId hurt = createCreature(PLAYER_ID, new Point(8, 3), 19, 100, CreatureState.FIGHT);
        entityData.setComponent(hurt, new AttackTarget(new EntityId(99)));
        EntityId healthy = createCreature(PLAYER_ID, new Point(8, 3), 20, 100, CreatureState.FIGHT);
        entityData.setComponent(healthy, new AttackTarget(new EntityId(99)));

        system.processTick(0.25f);

        assertEquals(Map.of(hurt, new Point(2, 4)), dropped);

        // That was the action of this tick
        assertTrue(impSpells.isEmpty());
    }

    @Test
    void carriesAHurtCreatureThatIsNotFightingToTheHeartOnlyFromOutsideTheRooms() {
        KeeperAiSystem system = createSystem(withRetreatHealth(ai(0, 0, 9, 1, 50, 0, List.of()), 20), 1000);
        createCreature(PLAYER_ID, new Point(3, 6), 5, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(1, 4), 5, 100, CreatureState.SLEEPING);
        createCreature((short) 4, new Point(1, 8), 5, 100, CreatureState.IDLE);

        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());

        EntityId outside = createCreature(PLAYER_ID, new Point(1, 8), 5, 100, CreatureState.IDLE);
        system.processTick(0.25f);

        assertEquals(Map.of(outside, HEART), dropped);
    }

    @Test
    void leavesAHurtCreatureItJustDroppedAloneForAWhile() {
        KeeperAiSystem system = createSystem(withRetreatHealth(ai(0, 0, 9, 1, 50, 0, List.of()), 20), 1000);
        EntityId hurt = createCreature(PLAYER_ID, new Point(1, 8), 5, 100, CreatureState.IDLE);
        system.processTick(0.25f);
        dropped.clear();

        // It walks out of the heart again
        entityData.setComponent(hurt, new Position(0, WorldUtils.pointToVector3f(1, 8)));
        gameTime = 74;
        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());

        gameTime = 75;
        system.processTick(0.25f);

        assertEquals(Map.of(hurt, HEART), dropped);
    }

    @Test
    void takesACombatPitFighterOutOfThePitAtHalfItsHealth() {
        lobbyType = AI.AIType.MASTER_KEEPER;
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(50), imp);
        creatureControl.onCreatureAdded(new EntityId(51), imp);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 50, 100, CreatureState.COMBAT_PIT);
        entityData.setComponent(fighter, new CombatPitFighter(new EntityId(98)));
        entityData.setComponent(fighter, new CreatureExperience(3, 0, 0, 0, 0));

        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());

        entityData.setComponent(fighter, new Health(49, 100));
        system.processTick(0.25f);

        assertEquals(Map.of(fighter, HEART), dropped);
    }

    @Test
    void takesACombatPitFighterOutOfThePitAtTheLevelItTrainsTo() {
        lobbyType = AI.AIType.CONQUEROR;
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(50), imp);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.COMBAT_PIT);
        entityData.setComponent(fighter, new CombatPitFighter(new EntityId(98)));
        entityData.setComponent(fighter, new CreatureExperience(4, 0, 0, 0, 0));

        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());

        // A Conqueror trains its creatures to level 5
        entityData.setComponent(fighter, new CreatureExperience(5, 0, 0, 0, 0));
        system.processTick(0.25f);

        assertEquals(Map.of(fighter, HEART), dropped);
    }

    @Test
    void castsCallToArmsOnAFighterThatFacesMoreThreatThanItTakesOnWithoutTheFlag() {
        KeeperAiSystem system = createSystem(withCallToArms(ai(0, 0, 9, 1, 50, 0, List.of()), 150, 240, 1, 5, 1, 90), 1000);
        spellControl.setTypeAvailable(callToArms, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.FIGHT);
        EntityId enemy = createCreature(ENEMY_ID, new Point(9, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(enemy));

        // One enemy is a threat of 100
        gameTime = 10;
        system.processTick(0.25f);

        assertTrue(callToArmsCasts.isEmpty());

        // Only every 10 seconds
        createCreature(ENEMY_ID, new Point(8, 8), 100, 100, CreatureState.IDLE);
        gameTime = 10.25f;
        system.processTick(0.25f);

        assertTrue(callToArmsCasts.isEmpty());

        gameTime = 20;
        system.processTick(0.25f);

        assertEquals(List.of(new Point(8, 3)), callToArmsCasts);
        assertTrue(getKeeperAi().callToArms);
        assertEquals(240, getKeeperAi().callToArmsTicks);

        // One flag at a time
        gameTime = 30;
        system.processTick(0.25f);

        assertEquals(1, callToArmsCasts.size());
        assertEquals(0, callToArmsCancels);
    }

    @Test
    void liftsTheCallToArmsAfterItsTimeAndWaitsBeforeTheNext() {
        KeeperAiSystem system = createSystem(withCallToArms(ai(0, 0, 9, 1, 50, 0, List.of()), 50, 2, 1, 5, 1, 90), 1000);
        spellControl.setTypeAvailable(callToArms, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(9, 3), 100, 100, CreatureState.FIGHT)));
        gameTime = 10;
        system.processTick(0.25f);

        assertEquals(1, callToArmsCasts.size());

        system.processTick(0.25f);

        assertEquals(0, callToArmsCancels);

        system.processTick(0.25f);

        assertEquals(1, callToArmsCancels);
        assertEquals(360, getKeeperAi().cannotAttackTicks);

        gameTime = 20;
        system.processTick(0.25f);

        assertEquals(1, callToArmsCasts.size());
    }

    @Test
    void liftsTheCallToArmsWhenTheEnemiesAroundTheFlagAreGone() {
        KeeperAiSystem system = createSystem(withCallToArms(ai(0, 0, 9, 1, 50, 0, List.of()), 50, 240, 1, 5, 1, 90), 1000);
        spellControl.setTypeAvailable(callToArms, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.FIGHT);
        EntityId enemy = createCreature(ENEMY_ID, new Point(9, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(enemy));
        gameTime = 10;
        system.processTick(0.25f);
        system.processTick(0.25f);

        assertEquals(1, callToArmsCasts.size());
        assertEquals(0, callToArmsCancels);

        // 5 tiles away is out of range
        entityData.setComponent(enemy, new Position(0, WorldUtils.pointToVector3f(13, 3)));
        system.processTick(0.25f);

        assertEquals(1, callToArmsCancels);
    }

    @Test
    void liftsTheCallToArmsWhenItHasTooFewFightersLeft() {
        KeeperAiSystem system = createSystem(withCallToArms(ai(0, 0, 9, 1, 50, 0, List.of()), 50, 240, 1, 5, 2, 90), 1000);
        spellControl.setTypeAvailable(callToArms, true, true);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(9, 3), 100, 100, CreatureState.FIGHT)));
        EntityId second = createCreature(PLAYER_ID, new Point(3, 6), 100, 100, CreatureState.IDLE);
        gameTime = 10;
        system.processTick(0.25f);
        system.processTick(0.25f);

        assertEquals(0, callToArmsCancels);

        entityData.removeEntity(second);
        system.processTick(0.25f);

        assertEquals(1, callToArmsCancels);
    }

    @Test
    void createsNoImpsWhileItsCreaturesFight() {
        KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of()), 1000);
        EntityId fighter = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.FIGHT);
        entityData.setComponent(fighter, new AttackTarget(createCreature(ENEMY_ID, new Point(9, 3), 100, 100, CreatureState.FIGHT)));

        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());

        entityData.removeComponent(fighter, AttackTarget.class);
        system.processTick(0.25f);

        assertEquals(1, impSpells.size());
    }

    @Test
    void dropsItsCreaturesIntoTheTrainingRoomWhileItsArmyIsNotReady() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(TRAINING_ROOM, new Point(1, 4)));
        EntityId creature = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        EntityId trainee = createCreature(PLAYER_ID, new Point(8, 4), 100, 100, CreatureState.WORK);
        entityData.setComponent(trainee, new TaskComponent(1, null, new Point(1, 4), TaskType.TRAIN, 0, true));

        // One fighter is an army when it wants more than none
        for (int tick = 0; tick < 60; tick++) {
            system.processTick(0.25f);
        }
        assertTrue(dropped.isEmpty());

        entityData.removeEntity(entityData.findEntity(null, KeeperAi.class));
        system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 5, 1, AI.CallToArmsUsagePolicy.AT_HEART, 0), 1000);
        roomControl.onBuild(createRoom(TRAINING_ROOM, new Point(1, 4)));
        for (int tick = 0; tick < 60 && dropped.isEmpty(); tick++) {
            system.processTick(0.25f);
        }

        // The one in training is left to it
        assertEquals(Map.of(creature, new Point(1, 4)), dropped);
    }

    @Test
    void dropsACreatureNearItsNextLevelIntoTheCombatPitWhenItIsFit() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 5, 1, AI.CallToArmsUsagePolicy.AT_HEART, 0), 1000);
        roomControl.onBuild(createRoom(TRAINING_ROOM, new Point(1, 4)));
        roomControl.onBuild(createRoom(COMBAT_PIT, new Point(5, 4)));
        EntityId hurt = createCreature(PLAYER_ID, new Point(8, 3), 75, 100, CreatureState.IDLE);
        entityData.setComponent(hurt, new CreatureExperience(2, 751, 1000, 0, 0));
        EntityId fit = createCreature(PLAYER_ID, new Point(8, 4), 76, 100, CreatureState.IDLE);
        entityData.setComponent(fit, new CreatureExperience(2, 751, 1000, 0, 0));
        for (int tick = 0; tick < 60 && dropped.isEmpty(); tick++) {
            system.processTick(0.25f);
        }

        // Neither goes to the Training Room
        assertEquals(Map.of(fit, new Point(5, 4)), dropped);
        for (int tick = 0; tick < 60; tick++) {
            system.processTick(0.25f);
        }
        assertEquals(Map.of(fit, new Point(5, 4)), dropped);
    }

    @Test
    void dropsACreatureOnItsFullPortalToMakeRoomForOneThatResearches() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(LIBRARY, new Point(1, 4)));
        roomControl.onBuild(createRoom(PORTAL, new Point(5, 4)));
        spellControl.setTypeAvailable(heal, true, false);
        EntityId researcher = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = EATER;
        entityData.setComponent(researcher, creatureComponent);
        EntityId other = createCreature(PLAYER_ID, new Point(8, 4), 100, 100, CreatureState.IDLE);
        creatureControl.onCreatureAdded(researcher, creatureWithJobs(EATER, Creature.JobType.RESEARCH));
        system.processTick(0.25f);

        // There is room for one more
        assertTrue(dropped.isEmpty());

        creatureControl.onCreatureAdded(other, CombatExperienceFixtures.creature((short) 0));
        system.processTick(0.25f);

        assertEquals(Map.of(other, new Point(5, 4)), dropped);
    }

    @Test
    void makesNoRoomForAResearcherWithNothingToResearch() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(LIBRARY, new Point(1, 4)));
        roomControl.onBuild(createRoom(PORTAL, new Point(5, 4)));
        spellControl.setTypeAvailable(heal, true, true);
        EntityId one = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        EntityId other = createCreature(PLAYER_ID, new Point(8, 4), 100, 100, CreatureState.IDLE);
        creatureControl.onCreatureAdded(one, CombatExperienceFixtures.creature((short) 0));
        creatureControl.onCreatureAdded(other, CombatExperienceFixtures.creature((short) 0));
        system.processTick(0.25f);

        assertTrue(dropped.isEmpty());
    }

    @Test
    void picksUpAnIdleCreatureToResearchWhileThereIsResearchToDo() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
        roomControl.onBuild(createRoom(LIBRARY, new Point(1, 4)));
        spellControl.setTypeAvailable(heal, true, true);
        EntityId creature = createCreature(PLAYER_ID, new Point(8, 3), 100, 100, CreatureState.IDLE);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = EATER;
        entityData.setComponent(creature, creatureComponent);
        for (int tick = 0; tick < 200; tick++) {
            system.processTick(0.25f);
        }

        // Nothing to research
        assertTrue(handControl.isEmpty());
        assertTrue(dropped.isEmpty());

        spellControl.setTypeAvailable(thunderbolt, true, false);
        for (int tick = 0; tick < 200 && dropped.isEmpty(); tick++) {
            system.processTick(0.25f);
        }

        assertEquals(Map.of(creature, new Point(1, 4)), dropped);
    }

    private EntityId createCreature(short playerId, Point tile, int health, int maxHealth, CreatureState state) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new CreatureComponent(), new Owner(playerId, playerId), new Health(health, maxHealth),
                new CreatureAi(0, state, (short) 2), new Position(0, WorldUtils.pointToVector3f(tile.x, tile.y)),
                new Threat(100));

        return entityId;
    }

    @Test
    void aComputerPlayerSetUpInTheLobbyPlaysByItsTypeAndNotByTheLevel() {
        lobbyType = AI.AIType.MASTER_KEEPER;
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);

        // The level says no imps, a Master Keeper wants one for every 15 of its 25 tiles
        system.processTick(0.25f);

        assertEquals(1, impSpells.size());
    }

    @Test
    void aPsychoticWantsMoreLandForAnImpThanAMasterKeeper() {
        lobbyType = AI.AIType.PSYCHOTIC;
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);

        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());
    }

    @Test
    void aComputerPlayerTypeStartsWithSomeMana() {
        lobbyType = AI.AIType.PARANOID;
        createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);

        assertEquals(1100, keeper.getMana());
    }

    @Test
    void logsWhatItDecidesWithTheTickAndTheReasonWhenTheLogIsOn() {
        List<String> lines = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                lines.add(logRecord.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(KeeperAiSystem.class.getName());
        logger.addHandler(handler);
        try {
            logDecisions = true;
            gameTime = 30;
            KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of(TREASURY)), 1000);
            roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
            creatureControl.onCreatureAdded(new EntityId(1), imp);
            system.processTick(0.25f);

            // The second imp, at 4 ticks a second
            assertEquals(1, lines.size());
            assertTrue(lines.getFirst().matches("AI tick 120 player 3 PLAN: cast Create Imp at \\d+,\\d+ \\(1 imps, .*\\)"), lines.getFirst());

            creatureControl.onCreatureAdded(new EntityId(2), imp);
            system.processTick(0.25f);

            assertEquals(2, lines.size());
            assertEquals("AI tick 120 player 3 PLAN: next FIND_ROOM_SITE at 3,6 (room 1 is next, looking west)", lines.getLast());
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void logsWhyItPlansNothingWhenItStartsToAndNotOnEveryTick() {
        List<String> lines = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                lines.add(logRecord.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(KeeperAiSystem.class.getName());
        logger.addHandler(handler);
        try {
            logDecisions = true;
            gameTime = 30;
            KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of()), 1000);
            system.processTick(0.25f);
            system.processTick(0.25f);

            assertEquals(List.of("AI tick 120 player 3 PLAN: nothing to do (no room left to build, no neutral Portal to dig to, 1000 gold, not ready to invade)"), lines);
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void logsNothingWhenTheLogIsOff() {
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                records.add(logRecord);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(KeeperAiSystem.class.getName());
        logger.addHandler(handler);
        try {
            KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of(TREASURY)), 1000);
            creatureControl.onCreatureAdded(new EntityId(1), imp);
            system.processTick(0.25f);

            assertEquals(1, impSpells.size());
            assertTrue(records.isEmpty());
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void createsAnImpOnACornerOfTheHeartWhenItsLandAllowsMore() {
        KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(1), imp);

        // 25 owned tiles at 10 tiles an imp allow 2 imps
        system.processTick(0.25f);

        assertEquals(1, impSpells.size());
        assertEquals(2, Math.abs(impSpells.getFirst().x - HEART.x));
        assertEquals(2, Math.abs(impSpells.getFirst().y - HEART.y));
    }

    @Test
    void digsATunnelToTheEnemyHeartOnceItHasMoreFightersThanItsSettingsAskFor() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 1, 50, AI.CallToArmsUsagePolicy.AT_HEART, 0), 1000);
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(13, 6)));
        createCreature(PLAYER_ID, new Point(1, 4), 100, 100, CreatureState.IDLE);

        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.PLAN, getKeeperAi().task);

        createCreature(PLAYER_ID, new Point(1, 8), 100, 100, CreatureState.IDLE);
        system.processTick(0.25f);

        // From its own heart to the enemy's, by the settings
        assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertEquals(HEART, getKeeperAi().current);
        assertEquals(new Point(13, 6), getKeeperAi().target);
        assertEquals(ENEMY_ID, getKeeperAi().invasionTargetId);

        // DK II keeps 4 bits of the 50 tunnels, so 2, one of them dug now
        assertEquals(1, getEnemy().tunnelsLeft);
        assertTrue(getEnemy().invaded);
    }

    @Test
    void stopsTheTunnelAtTheEnemysLandAndBreaksThroughWhenAllTunnelsAreDug() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 0, 1, AI.CallToArmsUsagePolicy.AT_HEART, 5), 1000);
        for (int y = 4; y <= 8; y++) {
            for (int x = 12; x <= 14; x++) {
                map[y][x] = 'E';
            }
        }
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(13, 6)));
        createCreature(PLAYER_ID, new Point(1, 4), 100, 100, CreatureState.IDLE);

        for (int tick = 0; tick < 100 && (getEnemy() == null || getEnemy().breachTiles[0] == null); tick++) {
            system.processTick(0.25f);

            // The imps dig out what is tagged
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }

        // The tunnel stops next to the enemy's land, that tile is not dug
        assertEquals(new Point(11, 6), getEnemy().breachTiles[0]);
        assertEquals(new Point(13, 6), getEnemy().breachTargets[0]);
        assertEquals('G', map[6][11]);
        assertEquals(0, getKeeperAi().invasionTargetId);

        // That was its only tunnel, so it breaks through and then waits
        for (int tick = 0; tick < 10 && getEnemy().breachTiles[0] != null; tick++) {
            system.processTick(0.25f);
        }

        assertEquals(KeeperAi.Task.DIG_PATH, getKeeperAi().task);
        assertEquals(new Point(11, 6), getKeeperAi().current);
        assertEquals(new Point(13, 6), getKeeperAi().target);
        assertEquals(0, getKeeperAi().invasionTargetId);
        assertEquals(5 * 4, getKeeperAi().cannotAttackTicks);
    }

    @Test
    void callsItsCreaturesToArmsAtTheEnemyHeartOnceTheyCanWalkThereAndDropsHalfItsThreat() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 5, 1,
                AI.CallToArmsUsagePolicy.AT_HEART_WITH_CREATURES, 0), 1000);
        spellControl.setTypeAvailable(callToArms, true, true);
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(8, 6)));
        KeeperAiEnemy invaded = new KeeperAiEnemy(PLAYER_ID, ENEMY_ID, 0);
        invaded.invaded = true;
        entityData.setComponent(entityData.createEntity(), invaded);
        createCreature(PLAYER_ID, new Point(1, 4), 100, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(1, 8), 100, 100, CreatureState.IDLE);

        system.processTick(0.25f);

        assertTrue(callToArmsCasts.isEmpty());

        reachable = true;
        system.processTick(0.25f);

        // On a corner of the enemy's heart
        assertEquals(1, callToArmsCasts.size());
        assertEquals(2, Math.abs(callToArmsCasts.getFirst().x - 8));
        assertEquals(2, Math.abs(callToArmsCasts.getFirst().y - 6));
        assertTrue(getKeeperAi().callToArms);

        // Its land near the flag gets half of its threat of 200
        assertEquals(1, getKeeperAi().attacks);
        assertEquals(100, getKeeperAi().attackNeeds[0]);
    }

    @Test
    void goesOnPlanningRoomsWhileItInvadesWithoutCallToArms() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 5, 1,
                AI.CallToArmsUsagePolicy.AT_HEART_WITH_CREATURES, 0), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(8, 6)));
        KeeperAiEnemy invaded = new KeeperAiEnemy(PLAYER_ID, ENEMY_ID, 0);
        invaded.invaded = true;
        entityData.setComponent(entityData.createEntity(), invaded);
        createCreature(PLAYER_ID, new Point(1, 4), 100, 100, CreatureState.IDLE);
        reachable = true;

        system.processTick(0.25f);

        // Call to Arms is still to be researched, so the turn goes to the next room
        assertTrue(callToArmsCasts.isEmpty());
        assertEquals(TREASURY, getKeeperAi().roomId);
        assertEquals(KeeperAi.Task.FIND_ROOM_SITE, getKeeperAi().task);
    }

    @Test
    void movesTheFlagOnTowardsTheEnemyHeartOnceItsCreaturesHaveCome() {
        KeeperAiSystem system = createSystem(withInvasion(ai(0, 0, 9, 1, 50, 0, List.of()), 5, 1,
                AI.CallToArmsUsagePolicy.TOWARDS_HEART, 0), 1000);
        spellControl.setTypeAvailable(callToArms, true, true);
        enemyRoomControl.onBuild(createRoom(DUNGEON_HEART, new Point(13, 6)));
        KeeperAiEnemy invaded = new KeeperAiEnemy(PLAYER_ID, ENEMY_ID, 0);
        invaded.invaded = true;
        entityData.setComponent(entityData.createEntity(), invaded);
        createCreature(PLAYER_ID, new Point(4, 4), 100, 100, CreatureState.IDLE);
        createCreature(PLAYER_ID, new Point(4, 8), 100, 100, CreatureState.IDLE);
        reachable = true;
        system.processTick(0.25f);
        EntityId entityId = entityData.findEntity(null, KeeperAi.class);
        KeeperAi keeperAi = new KeeperAi(entityData.getComponent(entityId, KeeperAi.class));
        keeperAi.callToArms = true;
        keeperAi.callToArmsTile = new Point(5, 6);
        keeperAi.callToArmsTicks = 100;
        keeperAi.rallyTile = new Point(5, 6);
        entityData.setComponent(entityId, keeperAi);

        system.processTick(0.25f);

        // Both creatures are near the flag, so it goes 2 tiles on
        assertEquals(1, callToArmsCasts.size());
        Point moved = callToArmsCasts.getFirst();
        assertEquals(2, Math.abs(moved.x - 5) + Math.abs(moved.y - 6));
        assertEquals(moved, getKeeperAi().rallyTile);
    }

    @Test
    void doesNothingOnceItsDungeonHeartIsGone() {
        KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(1), imp);
        roomControl.getDungeonHeart().getRoomInstance().setDestroyed(true);

        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());

        roomControl.getDungeonHeart().getRoomInstance().setDestroyed(false);
        keeper.setDestroyed((short) 0);
        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());
    }

    @Test
    void createsNoImpWhenItsLandAllowsNoMore() {
        KeeperAiSystem system = createSystem(ai(4, 10, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(1), imp);
        creatureControl.onCreatureAdded(new EntityId(2), imp);

        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());
    }

    @Test
    void createsNoImpAtItsMaximum() {
        KeeperAiSystem system = createSystem(ai(1, 1, 9, 1, 50, 0, List.of()), 1000);
        creatureControl.onCreatureAdded(new EntityId(1), imp);

        system.processTick(0.25f);

        assertTrue(impSpells.isEmpty());
    }

    @Test
    void picksTheFirstRoomOfTheBuildOrderItMayBuildAndDoesNotHave() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY, LAIR, GUARD_ROOM, HATCHERY)), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);
        roomControl.onBuild(createRoom(TREASURY, new Point(1, 4)));
        roomControl.setTypeAvailable(rooms.get(LAIR), true, false); // Still to be researched
        roomControl.setTypeAvailable(rooms.get(GUARD_ROOM), true, true); // Never built by the computer player
        roomControl.setTypeAvailable(rooms.get(HATCHERY), true, true);

        system.processTick(0.25f);

        assertEquals(HATCHERY, getKeeperAi().roomId);
        assertEquals(KeeperAi.Task.FIND_ROOM_SITE, getKeeperAi().task);
    }

    @Test
    void headsForTheGoldWhenTheRoomCostsTooMuch() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 9 * ROOM_COST - 1);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);

        system.processTick(0.25f);

        assertEquals(KeeperAi.Task.GO_TO_GOLD, getKeeperAi().task);
        assertEquals(new Point(10, 6), getKeeperAi().target);
    }

    @Test
    void hasTheRoomSiteDugOutAndBuildsTheRoomOnIt() {
        KeeperAiSystem system = createSystem(ai(0, 0, 9, 1, 50, 0, List.of(TREASURY)), 1000);
        roomControl.setTypeAvailable(rooms.get(TREASURY), true, true);

        for (int tick = 0; tick < 100 && built.size() < 9; tick++) {
            system.processTick(0.25f);

            // The imps dig out and claim what is tagged
            for (Point p : tagged) {
                map[p.y][p.x] = '.';
            }
            tagged.clear();
        }

        // West is impenetrable and south too narrow, so it goes north of the heart with a tile in between
        Set<Point> site = new HashSet<>();
        for (int x = 2; x <= 4; x++) {
            for (int y = 1; y <= 3; y++) {
                site.add(new Point(x, y));
            }
        }
        assertEquals(site, new HashSet<>(built));
        assertEquals(9, built.size());
    }

    @Test
    void findsARoomSiteBehindTheGap() {
        KeeperAiRoomSiteFinder finder = new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 9, 1, 50, 0, List.of()));

        KeeperAiRoomSiteFinder.RoomSite site = finder.find(HEART, EAST, 9);

        assertEquals(new Point(6, 6), site.entrance());
        assertEquals(new Point(6, 7), site.corner());
        assertEquals(new Point(8, 5), site.oppositeCorner());
    }

    @Test
    void roomSiteGetsOneMoreRowWhenTheSizeIsNotSquare() {
        KeeperAiRoomSiteFinder finder = new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 12, 1, 50, 0, List.of()));

        KeeperAiRoomSiteFinder.RoomSite site = finder.find(HEART, EAST, 12);

        assertEquals(new Point(6, 7), site.corner());
        assertEquals(new Point(9, 5), site.oppositeCorner());

        // There is no room for a fourth row between the heart and the edge of the map
        assertNull(finder.find(HEART, NORTH, 12));
    }

    @Test
    void roomSiteStaysSquareWhenNoGapIsLeftBetweenRooms() {
        KeeperAiRoomSiteFinder finder = new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 12, 0, 50, 0, List.of()));

        KeeperAiRoomSiteFinder.RoomSite site = finder.find(HEART, EAST, 12);

        assertEquals(new Point(5, 7), site.corner());
        assertEquals(new Point(7, 5), site.oppositeCorner());
    }

    @Test
    void findsNoRoomSiteInImpenetrableRockOrOutsideTheBuildArea() {
        AI ai = ai(0, 0, 9, 1, 50, 0, List.of());

        assertNull(new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai).find(HEART, WEST, 9));
        assertNull(new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, withBuildAreaEndX(ai, 8)).find(HEART, EAST, 9));
    }

    @Test
    void findsNoRoomSiteWithTooMuchOpenSpaceAroundIt() {

        // 5 of the 16 tiles around the site are claimed path
        assertNull(new KeeperAiRoomSiteFinder(mapController, PLAYER_ID, ai(0, 0, 9, 1, 30, 0, List.of())).find(HEART, EAST, 9));
    }

    private KeeperAi getKeeperAi() {
        return entityData.getComponent(entityData.findEntity(null, KeeperAi.class), KeeperAi.class);
    }

    private KeeperAiSystem createSystem(AI ai, int gold) {
        keeper = new Keeper(player(PLAYER_ID, ai));
        if (lobbyType != null) {
            keeper = new Keeper(true, PLAYER_ID);
            keeper.setAiType(lobbyType);
        }
        Map<MiscType, Variable.MiscVariable> variables = Map.of(
                MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL, variable(MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL, 8),
                MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, variable(MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, 4),
                MiscType.CREATURES_SUPPORTED_BY_FIRST_PORTAL, variable(MiscType.CREATURES_SUPPORTED_BY_FIRST_PORTAL, 2),
                MiscType.CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL, variable(MiscType.CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL, 1),
                MiscType.MAXIMUM_MANA_THRESHOLD, variable(MiscType.MAXIMUM_MANA_THRESHOLD, 200000));
        PlayerManaControl manaControl = new PlayerManaControl(keeper, variables);
        handControl = new PlayerHandControl(keeper, 8, entityData);
        spellControl = new PlayerSpellControl(keeper, List.of(createImp, callToArms, thunderbolt, heal));
        Keeper enemyKeeper = new Keeper(false, ENEMY_ID);
        enemyKeeper.setPlayer(new Player());
        enemyRoomControl = new PlayerRoomControl(enemyKeeper, new ArrayList<>(rooms.values()));
        IPlayerController enemyController = (IPlayerController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" -> enemyKeeper;
            case "getRoomControl" -> enemyRoomControl;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (!"cancelCallToArms".equals(method.getName())) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    callToArmsCancels++;
                    return null;
                });
        keeper.setGold(gold);
        keeper.setMana(1000);
        roomControl = new PlayerRoomControl(keeper, new ArrayList<>(rooms.values()));
        roomControl.onBuild(createRoom(DUNGEON_HEART, HEART));
        creatureControl = new PlayerCreatureControl(keeper, imp, List.of(imp));
        IPlayerController playerController = (IPlayerController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" -> keeper;
            case "getRoomControl" -> roomControl;
            case "getCreatureControl" -> creatureControl;
            case "getManaControl" -> manaControl;
            case "getHandControl" -> handControl;
            case "getSpellControl" -> spellControl;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getGameLevel" -> gameLevel(4);
            case "getKeeperSpells" -> List.of(createImp, heal, thunderbolt);
            case "getCreature" -> ((Number) args[0]).shortValue() == EATER ? creatureWithJobs(EATER, Creature.JobType.RESEARCH, Creature.JobType.EAT)
                    : ((Number) args[0]).shortValue() == HORNY
                    ? CombatExperienceFixtures.creature(HORNY, Creature.CreatureFlag.IS_HORNY) : CombatExperienceFixtures.creature((short) 0);
            case "getRooms" -> new ArrayList<>(rooms.values());
            case "getVariables" -> variables;
            case "getRoomById" -> rooms.get(((Number) args[0]).shortValue());
            case "getKeeperSpellById" -> ((Number) args[0]).shortValue() == CALL_TO_ARMS ? callToArms : null;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        ICreatureController creatureController = (ICreatureController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                });
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
                    if (!"createController".equals(method.getName())) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return creatureController;
                });
        IGameWorldController gameWorldController = (IGameWorldController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IGameWorldController.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMapController" -> {
                            return mapController;
                        }
                        case "getObjectsController" -> {
                            return objectsController;
                        }
                        case "getCreaturesController" -> {
                            return creaturesController;
                        }
                        case "castKeeperSpell" -> {
                            if ((short) args[0] == HEAL || (short) args[0] == THUNDERBOLT) {
                                battleCasts.put((short) args[0], (EntityId) args[1]);
                                keeper.setMana(keeper.getMana() - 100);
                                return null;
                            }
                            boolean imp = (short) args[0] == createImp.getKeeperSpellId();
                            (imp ? impSpells : callToArmsCasts).add((Point) args[2]);
                            keeper.setMana(keeper.getMana() - (imp ? createImp : callToArms).getManaCost());
                        }
                        case "pickUp" ->
                            handControl.push((EntityId) args[0]);
                        case "drop" -> {
                            dropped.put(handControl.pop(), (Point) args[1]);
                            entityData.setComponent((EntityId) args[0], new Position(0, new Vector3f(((Vector2f) args[2]).x, 0, ((Vector2f) args[2]).y)));
                        }
                        case "build" -> {
                            Point p = new Point((int) ((Vector2f) args[0]).x, (int) ((Vector2f) args[0]).y);
                            built.add(p);
                            map[p.y][p.x] = 'H';
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
                    return null;
                });

        INavigationService navigationService = (INavigationService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{INavigationService.class}, (proxy, method, args) -> {
                    if (!"findPath".equals(method.getName())) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return findPath((Point) args[0], (Point) args[1]);
                });

        return new KeeperAiSystem(entityData, kwdFile, Map.of(PLAYER_ID, playerController, ENEMY_ID, enemyController), gameWorldController,
                navigationService, () -> gameTime, logDecisions);
    }

    private IRoomController createRoom(short roomId, Point centre) {
        RoomInstance roomInstance = new RoomInstance(rooms.get(roomId));
        roomInstance.addCoordinate(centre);

        return (IRoomController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getRoom" -> rooms.get(roomId);
            case "isDungeonHeart" -> roomId == DUNGEON_HEART;
            case "getRoomInstance" -> roomInstance;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private IMapController createMapController() {
        IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getWidth" -> map[0].length;
            case "getHeight" -> map.length;
            case "getTile" -> args.length == 1 ? createTile((Point) args[0]) : createTile(new Point((int) args[0], (int) args[1]));
            default -> throw new UnsupportedOperationException(method.getName());
        });

        return (IMapController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMapData" -> {
                            return mapData;
                        }
                        case "getTerrain" -> {
                            return getTerrain(((IMapTileController) args[0]).getLocation());
                        }
                        case "getRoomControllers" -> {
                            return neutralRooms;
                        }
                        case "getRoomControllerByCoordinates" -> {
                            Point p = (Point) args[0];
                            return map[p.y][p.x] == 'H' ? roomControl.getDungeonHeart() : null;
                        }
                        case "isSolid" -> {
                            return getTerrain((Point) args[0]).getFlags().contains(TerrainFlag.SOLID);
                        }
                        case "isTaggable" -> {
                            return getTerrain((Point) args[0]).getFlags().contains(TerrainFlag.TAGGABLE);
                        }
                        case "isBuildable" -> {
                            return map[((Point) args[0]).y][((Point) args[0]).x] == '.';
                        }
                        case "selectTiles" -> {
                            Vector2f start = (Vector2f) args[0];
                            Vector2f end = (Vector2f) args[1];
                            for (int x = (int) start.x; x <= end.x; x++) {
                                for (int y = (int) start.y; y <= end.y; y++) {
                                    if (getTerrain(new Point(x, y)).getFlags().contains(TerrainFlag.TAGGABLE)) {
                                        tagged.add(new Point(x, y));
                                    }
                                }
                            }
                            return null;
                        }
                        default ->
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    /**
     * A way along x and then along y, when the way is open
     */
    private GraphPath<IMapTileInformation> findPath(Point from, Point to) {
        if (!reachable) {
            return null;
        }
        DefaultGraphPath<IMapTileInformation> path = new DefaultGraphPath<>();
        Point p = from;
        path.add(createTile(p));
        while (!p.equals(to)) {
            p = p.x != to.x ? new Point(p.x + Integer.signum(to.x - p.x), p.y) : new Point(p.x, p.y + Integer.signum(to.y - p.y));
            path.add(createTile(p));
        }

        return path;
    }

    private KeeperAiEnemy getEnemy() {
        EntityId entityId = entityData.findEntity(null, KeeperAiEnemy.class);

        return entityId != null ? entityData.getComponent(entityId, KeeperAiEnemy.class) : null;
    }

    private Terrain getTerrain(Point p) {
        return terrains.get(map[p.y][p.x]);
    }

    private IMapTileController createTile(Point p) {
        if (p.x < 0 || p.y < 0 || p.x >= map[0].length || p.y >= map.length) {
            return null;
        }

        return (IMapTileController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapTileController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getLocation" -> p;
            case "getOwnerId" -> map[p.y][p.x] == '.' || map[p.y][p.x] == 'H' ? PLAYER_ID : map[p.y][p.x] == 'E' ? ENEMY_ID : (short) 0;
            case "getRoomId" -> map[p.y][p.x] == 'H' ? new EntityId(1) : null;
            case "isSelected" -> tagged.contains(p);
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }
}
