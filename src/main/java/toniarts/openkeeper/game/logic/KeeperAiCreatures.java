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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.KeeperAi;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.CombatPitController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.TempleController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.AI;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * How the computer player looks after its creatures away from the fights:
 * it drops them where they train, makes room for creatures that research
 * puts idle creatures to work, slaps idle ones and sees to its prisoners. It uses the hand like a human player
 * does.
 */
final class KeeperAiCreatures {

    private static final short ROOM_LAIR = 2;
    private static final short ROOM_PORTAL = 3;
    private static final short ROOM_LIBRARY = 6;
    private static final short ROOM_TRAINING_ROOM = 7;
    private static final short ROOM_WORKSHOP = 10;
    private static final short ROOM_PRISON = 11;
    private static final short ROOM_TORTURE_CHAMBER = 12;
    private static final short ROOM_TEMPLE = 13;
    private static final short ROOM_COMBAT_PIT = 16;
    private static final short ROOM_HERO_PORTAL = 25;

    /**
     * DK II makes room for a creature that researches while it has fewer
     * than this
     */
    private static final int RESEARCHERS_WANTED = 2;

    /**
     * The room for each job DK II's computer player puts a creature to (the
     * table at 0x6ED490). It never picks a creature up for torture, but
     * puts one down in the Torture Chamber
     */
    private static final Map<Creature.JobType, Short> JOB_ROOMS = Map.of(
            Creature.JobType.RESEARCH, ROOM_LIBRARY,
            Creature.JobType.TRAIN, ROOM_TRAINING_ROOM,
            Creature.JobType.MANUFACTURE, ROOM_WORKSHOP,
            Creature.JobType.TORTURE, ROOM_TORTURE_CHAMBER);

    /**
     * DK II picks idle creatures up to put them to work on one tick in ten
     */
    private static final int PICK_UP_IDLE_CHANCE_PERCENTAGE = 10;

    /**
     * DK II slaps an idle creature on one tick in a hundred, when it has more
     * than half its health
     */
    private static final int SLAP_CHANCE = 100;
    private static final int SLAP_HEALTH_PERCENTAGE = 50;

    /**
     * DK II takes each enemy prisoner in turn with five chances in a
     * hundred
     */
    private static final int PRISONER_CHANCE_PERCENTAGE = 5;

    /**
     * DK II sends a creature to the Combat Pit when it has more than three
     * quarters of the experience for its next level, and only with more than
     * three quarters of its health
     */
    private static final int PIT_SHARE_NUMERATOR = 3;
    private static final int PIT_SHARE_DENOMINATOR = 4;

    /**
     * The states of a creature that sleeps, eats or goes to do so, DK II
     * leaves those alone
     */
    private static final Set<CreatureState> RESTING = Set.of(CreatureState.SLEEPING, CreatureState.GO_TO_SLEEP,
            CreatureState.EATING, CreatureState.RECUPERATING);
    private static final Set<TaskType> RESTING_TASKS = Set.of(TaskType.CLAIM_LAIR, TaskType.GO_TO_SLEEP, TaskType.GO_TO_EAT);

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IGameWorldController gameWorldController;
    private final KeeperAiInvasions invasions;
    private final KeeperAiBattles battles;
    private final KeeperAiBattles.DecisionLog decisionLog;

    /**
     * The highest level the Training Room trains a creature to
     */
    private final int trainingRoomMaxLevel;

    /**
     * The creatures the first Portal and each one after it take
     */
    private final int firstPortalCapacity;
    private final int additionalPortalCapacity;

    KeeperAiCreatures(EntityData entityData, IKwdFile kwdFile, IGameWorldController gameWorldController, KeeperAiBattles battles,
            KeeperAiInvasions invasions, KeeperAiBattles.DecisionLog decisionLog) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameWorldController = gameWorldController;
        this.invasions = invasions;
        this.battles = battles;
        this.decisionLog = decisionLog;
        trainingRoomMaxLevel = (int) kwdFile.getVariables().get(Variable.MiscVariable.MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL).getValue();
        firstPortalCapacity = (int) kwdFile.getVariables().get(Variable.MiscVariable.MiscType.CREATURES_SUPPORTED_BY_FIRST_PORTAL).getValue();
        additionalPortalCapacity = (int) kwdFile.getVariables().get(Variable.MiscVariable.MiscType.CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL).getValue();
    }

    /**
     * Drops creatures where they train, while its army is not ready. A
     * creature near its next level, or past what the Training Room teaches
     * and below the level the computer player wants, goes to the Combat Pit
     * when it is fit. Any other goes to the Training Room. Not into a room
     * it is building, not one it dropped a short while ago, nor one that
     * trains, sleeps, eats or fights. Each creature it tries takes an action.
     *
     * @param keeperAi the computer player
     * @param playerController its player
     * @param creatureLevel the level it wants its creatures at
     * @param actions the actions it has left
     * @return the actions it has left after this
     */
    int dropIntoTraining(KeeperAi keeperAi, IPlayerController playerController, int creatureLevel, int actions) {
        IRoomController trainingRoom = keeperAi.roomId != ROOM_TRAINING_ROOM ? getRoom(playerController, ROOM_TRAINING_ROOM) : null;
        IRoomController combatPit = keeperAi.roomId != ROOM_COMBAT_PIT ? getRoom(playerController, ROOM_COMBAT_PIT) : null;
        if (trainingRoom == null && combatPit == null) {
            return actions;
        }

        for (Entity creature : battles.getCreatures(keeperAi.playerId)) {
            if (actions == 0) {
                break;
            }
            if (creature.get(CreatureComponent.class).worker || !isFree(creature)) {
                continue;
            }

            CreatureExperience experience = entityData.getComponent(creature.getId(), CreatureExperience.class);
            int level = experience != null ? experience.level : 1;
            boolean nearNextLevel = experience != null
                    && experience.experience > experience.experienceToNextLevel * PIT_SHARE_NUMERATOR / PIT_SHARE_DENOMINATOR;
            if (nearNextLevel || (level >= trainingRoomMaxLevel && level < creatureLevel)) {
                Health health = creature.get(Health.class);
                if (combatPit == null || health.health <= health.maxHealth * PIT_SHARE_NUMERATOR / PIT_SHARE_DENOMINATOR
                        || entityData.getComponent(creature.getId(), CombatPitFighter.class) != null) {
                    continue;
                }
                actions--;
                Point tile = getRandomTile(combatPit, true);
                if (tile != null) {
                    decisionLog.log(keeperAi, "carry creature " + creature.getId().getId() + " to the Combat Pit", tile,
                            nearNextLevel ? "near its next level" : "at level " + level + ", past the Training Room");
                    battles.carry(creature.getId(), tile, playerController);
                }
            } else if (trainingRoom != null && !isTraining(creature)) {
                actions--;
                Point tile = getRandomTile(trainingRoom, false);
                decisionLog.log(keeperAi, "carry creature " + creature.getId().getId() + " to the Training Room", tile,
                        "its army is not ready");
                battles.carry(creature.getId(), tile, playerController);
            }
        }

        return actions;
    }

    /**
     * While a spell is still to research and it has a Library, but fewer
     * than two creatures that like to research, it drops its weakest other
     * creature on its Portal when its Portals take no more creatures, to
     * make room for one that researches. It takes no action.
     *
     * @param keeperAi the computer player
     * @param playerController its player
     */
    void makeRoomForResearcher(KeeperAi keeperAi, IPlayerController playerController) {
        if (getRoom(playerController, ROOM_LIBRARY) == null) {
            return;
        }

        int researchers = 0;
        Entity weakest = null;
        int weakestThreat = Integer.MAX_VALUE;
        for (Entity creature : battles.getCreatures(keeperAi.playerId)) {
            if (creature.get(CreatureComponent.class).worker) {
                continue;
            }
            Creature type = kwdFile.getCreature(creature.get(CreatureComponent.class).creatureId);
            if (likesJob(type, Creature.JobType.RESEARCH)) {
                researchers++;
            } else if (type.getAttributes().getThreat() <= weakestThreat) {
                weakest = creature;
                weakestThreat = type.getAttributes().getThreat();
            }
        }
        IRoomController portal = getRoom(playerController, ROOM_PORTAL);
        if (portal == null) {
            portal = getRoom(playerController, ROOM_HERO_PORTAL);
        }
        if (researchers >= RESEARCHERS_WANTED || weakest == null || portal == null
                || playerController.getCreatureControl().getTypeCount() < getPortalCapacity(playerController)
                || battles.isLeftAlone(weakest) || KeeperAiBattles.HELPLESS.contains(weakest.get(CreatureAi.class).getCreatureState())) {
            return;
        }

        Point tile = portal.getRoomInstance().getCenter();
        decisionLog.log(keeperAi, "carry creature " + weakest.getId().getId() + " to its Portal", tile,
                "its Portals are full and it has " + researchers + " creatures that research");
        battles.carry(weakest.getId(), tile, playerController);
    }

    /**
     * Puts down what is on top of its hand, which is there when it picked a
     * creature up to put it to work, or when a drop did not go through. A
     * creature goes to the first room for one of the jobs it likes that the
     * computer player has, anything else and a creature without such a room
     * goes to the Dungeon Heart.
     *
     * @param keeperAi the computer player
     * @param playerController its player
     * @return true if there was something in the hand, that takes the turn
     */
    boolean emptyHand(KeeperAi keeperAi, IPlayerController playerController) {
        EntityId held = playerController.getHandControl().peek();
        if (held == null) {
            return false;
        }

        Point tile = playerController.getKeeper().getDungeonHeartLocation();
        String reason = "its hand was not empty";
        CreatureComponent creatureComponent = entityData.getComponent(held, CreatureComponent.class);
        Creature.JobPreference[] jobs = creatureComponent != null ? kwdFile.getCreature(creatureComponent.creatureId).getHappyJobs() : null;
        if (jobs != null) {
            for (Creature.JobPreference job : jobs) {
                Short roomId = JOB_ROOMS.get(job.getJobType());
                IRoomController room = roomId != null ? getRoom(playerController, roomId) : null;
                if (room != null) {
                    tile = room.getRoomInstance().getCenter();
                    reason = "the creature likes to " + job.getJobType().name().toLowerCase() + " there";
                    break;
                }
            }
        }
        decisionLog.log(keeperAi, "drop " + held.getId() + " from its hand", tile, reason);
        if (creatureComponent != null) {
            battles.leaveAlone(held);
        }
        gameWorldController.drop(held, tile, WorldUtils.pointToVector2f(tile), null, keeperAi.playerId);

        return true;
    }

    /**
     * On one tick in ten it picks up idle creatures to put them to work, as
     * long as its hand has room: a creature that likes a job it has the room
     * for, and the job is wanted. Research while a spell is still to
     * research, training while its army is not ready, manufacture always.
     * The next turns put them down in the room. Each creature picked up
     * takes an action.
     *
     * @param keeperAi the computer player
     * @param playerController its player
     * @param ai its settings
     * @param creatureLevel the level it wants its creatures at
     * @param actions the actions it has left
     * @return the actions it has left after this
     */
    int pickUpIdleCreatures(KeeperAi keeperAi, IPlayerController playerController, AI ai, int creatureLevel, int actions) {
        if (Utils.getRandom().nextInt(100) >= PICK_UP_IDLE_CHANCE_PERCENTAGE || actions == 0 || playerController.getHandControl().isFull()) {
            return actions;
        }

        for (Entity creature : battles.getCreatures(keeperAi.playerId)) {
            Creature.JobPreference[] jobs = kwdFile.getCreature(creature.get(CreatureComponent.class).creatureId).getHappyJobs();
            if (!isIdle(creature) || battles.isLeftAlone(creature) || jobs == null) {
                continue;
            }
            for (Creature.JobPreference job : jobs) {
                Short roomId = JOB_ROOMS.get(job.getJobType());
                if (roomId == null || getRoom(playerController, roomId) == null
                        || !isJobWanted(job.getJobType(), keeperAi, playerController, ai, creatureLevel)) {
                    continue;
                }
                gameWorldController.pickUp(creature.getId(), keeperAi.playerId);
                if (creature.getId().equals(playerController.getHandControl().peek())) {
                    decisionLog.log(keeperAi, "pick up creature " + creature.getId().getId(), null,
                            "idle, it likes to " + job.getJobType().name().toLowerCase());
                    actions--;
                    break;
                }
            }
            if (actions == 0 || playerController.getHandControl().isFull()) {
                break;
            }
        }

        return actions;
    }

    /**
     * Slaps its idle creatures, each one with more than half its health and
     * not still quick from a slap has one chance in a hundred. Each slap
     * takes an action.
     *
     * @param keeperAi the computer player
     * @param actions the actions it has left
     * @return the actions it has left after this
     */
    int slapIdleCreatures(KeeperAi keeperAi, int actions) {
        for (Entity creature : battles.getCreatures(keeperAi.playerId)) {
            if (actions == 0) {
                break;
            }
            Health health = creature.get(Health.class);
            if (isIdle(creature) && health.health > health.maxHealth * SLAP_HEALTH_PERCENTAGE / 100
                    && entityData.getComponent(creature.getId(), Slapped.class) == null && Utils.getRandom().nextInt(SLAP_CHANCE) == 0) {
                decisionLog.log(keeperAi, "slap creature " + creature.getId().getId(), null, "idle");
                gameWorldController.interact(creature.getId(), keeperAi.playerId);
                actions--;
            }
        }

        return actions;
    }

    /**
     * Sees to a prisoner in its Prison. It goes through them in turn and
     * takes an enemy with five chances in a hundred, any other at once.
     * That takes an action, whatever follows. A creature of its own side
     * goes to the Lair. An enemy meets the fate its settings give it: the
     * Torture Chamber, the Combat Pit or the Temple, or it is left to die.
     * Not one it dropped a short while ago.
     *
     * @param keeperAi the computer player
     * @param playerController its player
     * @param ai its settings
     * @param actions the actions it has left
     * @return the actions it has left after this
     */
    int seeToPrisoners(KeeperAi keeperAi, IPlayerController playerController, AI ai, int actions) {
        IRoomController prison = getRoom(playerController, ROOM_PRISON);
        if (prison == null) {
            return actions;
        }

        Entity prisoner = null;
        boolean enemy = false;
        for (Entity creature : battles.getPrisoners(keeperAi.playerId)) {
            if (entityData.getComponent(creature.getId(), CreatureImprisoned.class) == null
                    || !prison.getRoomInstance().hasCoordinate(WorldUtils.vectorToPoint(creature.get(Position.class).position))) {
                continue;
            }
            enemy = playerController.getKeeper().isEnemy(creature.get(Owner.class).ownerId);
            if (!enemy || Utils.getRandom().nextInt(100) < PRISONER_CHANCE_PERCENTAGE) {
                prisoner = creature;
                break;
            }
        }
        if (prisoner == null) {
            return actions;
        }

        actions--;
        AI.ImprisonedCreatureFatePolicy fate = ai.getFateOfImprisonedCreatures();
        if (!enemy) {
            carryPrisoner(keeperAi, playerController, prisoner, getRoom(playerController, ROOM_LAIR), false, "its own side");
        } else if (fate == AI.ImprisonedCreatureFatePolicy.TORTURE) {
            carryPrisoner(keeperAi, playerController, prisoner, getRoom(playerController, ROOM_TORTURE_CHAMBER), true, "to torture");
        } else if (fate == AI.ImprisonedCreatureFatePolicy.PLACE_IN_PIT) {
            carryPrisoner(keeperAi, playerController, prisoner, getRoom(playerController, ROOM_COMBAT_PIT), false, "to fight in the pit");
        } else if (fate == AI.ImprisonedCreatureFatePolicy.SACRIFICE) {
            carryPrisoner(keeperAi, playerController, prisoner, getRoom(playerController, ROOM_TEMPLE), false, "to sacrifice");
        }

        return actions;
    }

    /**
     * Carries a prisoner to a room: to its middle, to a free tile of a
     * Torture Chamber, into the pool of a Temple, or to a tile of a Combat
     * Pit the creatures fight on
     */
    private void carryPrisoner(KeeperAi keeperAi, IPlayerController playerController, Entity prisoner, IRoomController room,
            boolean freeTile, String reason) {
        if (room == null || battles.isLeftAlone(prisoner) || prisoner.get(CreatureAi.class).getCreatureState() == CreatureState.DEAD) {
            return;
        }

        Point tile = room.getRoomInstance().getCenter();
        if (freeTile) {
            tile = getFreeTile(room);
        } else if (room instanceof CombatPitController) {
            tile = getRandomTile(room, true);
        } else if (room instanceof TempleController temple) {
            for (Point p : room.getRoomInstance().getCoordinates()) {
                if (temple.isSacrificePool(p)) {
                    tile = p;
                    break;
                }
            }
        }
        if (tile == null) {
            return;
        }
        decisionLog.log(keeperAi, "carry prisoner " + prisoner.getId().getId() + " to room " + room.getRoomInstance().getRoom().getRoomId(),
                tile, reason);
        battles.carry(prisoner.getId(), tile, playerController);
    }

    /**
     * A random tile of a room that no creature is being tortured on
     */
    private Point getFreeTile(IRoomController room) {
        List<Point> tiles = new ArrayList<>(room.getRoomInstance().getCoordinates());
        for (EntityId tortured : entityData.findEntities(null, CreatureTortured.class, Position.class)) {
            tiles.remove(WorldUtils.vectorToPoint(entityData.getComponent(tortured, Position.class).position));
        }

        return tiles.isEmpty() ? null : Utils.getRandomItem(tiles);
    }

    /**
     * Whether a job is wanted. Looking at the research also makes room for a
     * creature that researches, as in DK II
     */
    private boolean isJobWanted(Creature.JobType jobType, KeeperAi keeperAi, IPlayerController playerController, AI ai, int creatureLevel) {
        return switch (jobType) {
            case RESEARCH -> {
                boolean research = invasions.hasSpellsToResearch(playerController);
                if (research) {
                    makeRoomForResearcher(keeperAi, playerController);
                }
                yield research;
            }
            case TRAIN ->
                !invasions.isArmyReady(keeperAi.playerId, ai, creatureLevel);
            case MANUFACTURE ->
                true;
            default ->
                false;
        };
    }

    /**
     * A creature with nothing to do. DK II counts a fleeing fighter in too
     */
    private static boolean isIdle(Entity creature) {
        CreatureState state = creature.get(CreatureAi.class).getCreatureState();

        return state == CreatureState.IDLE || (state == CreatureState.FLEE && !creature.get(CreatureComponent.class).worker);
    }

    private int getPortalCapacity(IPlayerController playerController) {
        int portals = 0;
        for (short roomId : new short[]{ROOM_PORTAL, ROOM_HERO_PORTAL}) {
            for (IRoomController room : playerController.getRoomControl().getRoomControllers().getOrDefault(kwdFile.getRoomById(roomId), Set.of())) {
                portals += room.getRoomInstance().isDestroyed() ? 0 : 1;
            }
        }

        return portals == 0 ? 0 : firstPortalCapacity + (portals - 1) * additionalPortalCapacity;
    }

    private static boolean likesJob(Creature creature, Creature.JobType jobType) {
        if (creature.getHappyJobs() != null) {
            for (Creature.JobPreference job : creature.getHappyJobs()) {
                if (job.getJobType() == jobType) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * A creature the hand may take to put it to work: awake, not eating,
     * not in a fight, not helpless and not dropped a short while ago
     */
    private boolean isFree(Entity creature) {
        CreatureState state = creature.get(CreatureAi.class).getCreatureState();
        TaskComponent task = entityData.getComponent(creature.getId(), TaskComponent.class);

        return !battles.isLeftAlone(creature) && !RESTING.contains(state) && !KeeperAiBattles.HELPLESS.contains(state)
                && !KeeperAiBattles.FIGHTING.contains(state) && (task == null || !RESTING_TASKS.contains(task.taskType));
    }

    private boolean isTraining(Entity creature) {
        TaskComponent task = entityData.getComponent(creature.getId(), TaskComponent.class);

        return task != null && task.taskType == TaskType.TRAIN;
    }

    private IRoomController getRoom(IPlayerController playerController, short roomId) {
        for (IRoomController room : playerController.getRoomControl().getRoomControllers().getOrDefault(kwdFile.getRoomById(roomId), Set.of())) {
            if (!room.getRoomInstance().isDestroyed()) {
                return room;
            }
        }

        return null;
    }

    /**
     * A random tile of a room, of a Combat Pit one the creatures fight on
     */
    private static Point getRandomTile(IRoomController room, boolean fightingTile) {
        List<Point> tiles = new ArrayList<>();
        for (Point tile : room.getRoomInstance().getCoordinates()) {
            if (!fightingTile || !(room instanceof CombatPitController combatPit) || combatPit.isFightingTile(tile)) {
                tiles.add(tile);
            }
        }

        return tiles.isEmpty() ? null : Utils.getRandomItem(tiles);
    }

}
