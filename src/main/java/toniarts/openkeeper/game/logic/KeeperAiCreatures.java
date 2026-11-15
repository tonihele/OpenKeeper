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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.KeeperAi;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.CombatPitController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;

/**
 * How the computer player looks after its creatures away from the fights:
 * it drops them where they train. It uses the hand like a human player does.
 */
final class KeeperAiCreatures {

    private static final short ROOM_PORTAL = 3;
    private static final short ROOM_LIBRARY = 6;
    private static final short ROOM_TRAINING_ROOM = 7;
    private static final short ROOM_COMBAT_PIT = 16;
    private static final short ROOM_HERO_PORTAL = 25;

    /**
     * DK II makes room for a creature that researches while it has fewer
     * than this
     */
    private static final int RESEARCHERS_WANTED = 2;

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

    KeeperAiCreatures(EntityData entityData, IKwdFile kwdFile, KeeperAiBattles battles, KeeperAiBattles.DecisionLog decisionLog) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
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
