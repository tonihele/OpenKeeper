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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.KeeperAi;
import toniarts.openkeeper.game.component.KeeperAiMood;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.CombatPitController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.TempleController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
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

    private static final short ROOM_TREASURY = 1;
    private static final short ROOM_LAIR = 2;
    private static final short ROOM_PORTAL = 3;
    private static final short ROOM_HATCHERY = 4;
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
    private static final int FALLBACK_DROP_TILES = 5;

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
     * @return whether the held thing was successfully put down and took the turn
     */
    boolean emptyHand(KeeperAi keeperAi, IPlayerController playerController) {
        EntityId held = playerController.getHandControl().peek();
        if (held == null) {
            return false;
        }

        Point tile = KeeperAiBattles.getHeartDropTile(playerController.getKeeper());
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
        gameWorldController.drop(held, tile, WorldUtils.pointToVector2f(tile), null, keeperAi.playerId);
        if (held.equals(playerController.getHandControl().peek())) {
            Point fallback = battles.getNearestOwnOpenTile(keeperAi.playerId, tile, FALLBACK_DROP_TILES);
            if (fallback != null && !fallback.equals(tile)) {
                gameWorldController.drop(held, fallback, WorldUtils.pointToVector2f(fallback), null, keeperAi.playerId);
                if (!held.equals(playerController.getHandControl().peek())) {
                    decisionLog.log(keeperAi, "drop " + held.getId() + " from its hand", fallback,
                            reason + "; its preferred tile would not accept it");
                    return true;
                }
            }
        }
        if (!held.equals(playerController.getHandControl().peek())) {
            decisionLog.log(keeperAi, "drop " + held.getId() + " from its hand", tile, reason);
            return true;
        }

        return false;
    }

    /**
     * Sees to each of its creatures whose mood has just turned for the
     * worse, once, as DK II's computer player does when the message about
     * it comes in. An unhappy creature is made happy when its settings say
     * so. An angry one is got rid of when they say that, or else made happy
     * when they say that. None of it takes an action.
     *
     * @param keeperAi the computer player
     * @param playerController its player
     * @param ai its settings
     */
    void answerMoods(KeeperAi keeperAi, IPlayerController playerController, AI ai) {
        for (Entity creature : battles.getCreatures(keeperAi.playerId)) {
            CreatureMood mood = entityData.getComponent(creature.getId(), CreatureMood.class);
            if (mood == null) {
                continue;
            }

            int state = getMoodState(creature, mood);
            KeeperAiMood seen = entityData.getComponent(creature.getId(), KeeperAiMood.class);
            int seenState = seen != null ? seen.state : 0;
            if (state == seenState) {
                continue;
            }
            if (state == 0) {
                entityData.removeComponent(creature.getId(), KeeperAiMood.class);
            } else {
                entityData.setComponent(creature.getId(), new KeeperAiMood(state));
            }

            // A creature that calms down is no news
            if (state < seenState) {
                continue;
            }
            if (state == 2 && ai.isDisposeOfAngryCreatures()) {
                disposeOf(keeperAi, playerController, ai, creature);
            } else if (state == 2 ? ai.isTryToMakeAngryOnesHappy() : ai.isTryToMakeUnhappyOnesHappy()) {
                makeHappy(keeperAi, playerController, creature, mood.getDominantReason());
            }
        }
    }

    /**
     * Answers the biggest complaint of a creature: its pay in gold from the
     * Treasury or a chicken from the Hatchery dropped on it, the creature
     * itself dropped in the Lair, or in the first room for one of its jobs
     * unless the hand dropped it a short while ago
     */
    private void makeHappy(KeeperAi keeperAi, IPlayerController playerController, Entity creature, int reason) {
        EntityId creatureId = creature.getId();
        EntityId held = playerController.getHandControl().peek();
        switch (reason) {
            case CreatureMood.REASON_NO_FOOD -> {
                IRoomController hatchery = getRoom(playerController, ROOM_HATCHERY);
                EntityId chicken = null;
                if (hatchery != null && hatchery.hasObjectControl(ObjectType.FOOD)) {
                    IRoomObjectControl<?> food = hatchery.getObjectControl(ObjectType.FOOD);
                    for (Point tile : hatchery.getRoomInstance().getCoordinates()) {
                        Collection<EntityId> items = food.getItems(tile);
                        if (items != null && !items.isEmpty()) {
                            chicken = items.iterator().next();
                            break;
                        }
                    }
                }
                if (chicken == null) {
                    return;
                }
                gameWorldController.pickUp(chicken, keeperAi.playerId);
            }
            case CreatureMood.REASON_NO_LAIR -> {
                IRoomController lair = getRoom(playerController, ROOM_LAIR);
                if (lair != null) {
                    Point tile = lair.getRoomInstance().getCenter();
                    decisionLog.log(keeperAi, "carry creature " + creatureId.getId() + " to the Lair", tile, "unhappy without a lair");
                    battles.carry(creatureId, tile, playerController);
                }
                return;
            }
            case CreatureMood.REASON_NO_WORK -> {
                Creature.JobPreference[] jobs = kwdFile.getCreature(creature.get(CreatureComponent.class).creatureId).getHappyJobs();
                if (jobs == null || battles.isLeftAlone(creature)) {
                    return;
                }
                for (Creature.JobPreference job : jobs) {
                    Short roomId = JOB_ROOMS.get(job.getJobType());
                    IRoomController room = roomId != null ? getRoom(playerController, roomId) : null;
                    if (room != null) {
                        Point tile = room.getRoomInstance().getCenter();
                        decisionLog.log(keeperAi, "carry creature " + creatureId.getId() + " to room " + roomId, tile,
                                "unhappy without work, it likes to " + job.getJobType().name().toLowerCase());
                        battles.carry(creatureId, tile, playerController);
                        return;
                    }
                }
                return;
            }
            default -> {
                if (getRoom(playerController, ROOM_TREASURY) == null) {
                    return;
                }
                gameWorldController.getGold(creature.get(CreatureComponent.class).pay, keeperAi.playerId);
            }
        }

        // The gold or the chicken the hand has taken is dropped on the creature
        EntityId gift = playerController.getHandControl().peek();
        if (gift != null && !gift.equals(held)) {
            Vector3f position = creature.get(Position.class).position;
            Point tile = WorldUtils.vectorToPoint(position);
            decisionLog.log(keeperAi, "drop " + (reason == CreatureMood.REASON_NO_FOOD ? "a chicken" : "its pay")
                    + " on creature " + creatureId.getId(), tile, "unhappy");
            gameWorldController.drop(gift, tile, new Vector2f(position.x, position.z), creatureId, keeperAi.playerId);
        }
    }

    /**
     * Gets rid of an angry creature. A computer player that sacrifices drops
     * it on a random tile of the pool of its Temple. Any other, and one
     * whose hand could not do that, drops it on the middle of its Temple,
     * with no Temple of a Portal, with no Portal of a Hero Portal
     */
    private void disposeOf(KeeperAi keeperAi, IPlayerController playerController, AI ai, Entity creature) {
        IRoomController room = getRoom(playerController, ROOM_TEMPLE);
        if (ai.getDisposalMethod() == AI.CreatureDisposalPolicy.SACRIFICE && room instanceof TempleController temple) {
            List<Point> pool = new ArrayList<>();
            for (Point tile : room.getRoomInstance().getCoordinates()) {
                if (temple.isSacrificePool(tile)) {
                    pool.add(tile);
                }
            }
            if (!pool.isEmpty()) {
                Point tile = Utils.getRandomItem(pool);
                decisionLog.log(keeperAi, "sacrifice creature " + creature.getId().getId(), tile, "angry");
                if (battles.carry(creature.getId(), tile, playerController)) {
                    return;
                }
            }
        }
        if (room == null) {
            room = getRoom(playerController, ROOM_PORTAL);
        }
        if (room == null) {
            room = getRoom(playerController, ROOM_HERO_PORTAL);
        }
        if (room != null) {
            Point tile = room.getRoomInstance().getCenter();
            decisionLog.log(keeperAi, "carry creature " + creature.getId().getId() + " to room " + room.getRoomInstance().getRoom().getRoomId(),
                    tile, "angry");
            battles.carry(creature.getId(), tile, playerController);
        }
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
     * A creature with nothing to do, status 5 of DK II (0x478A20). An
     * unhappy or angry creature has a status of its own there, so it is
     * never idle. DK II also counts in some states OpenKeeper doesn't have,
     * such as sulking, being hypnotised or reporting danger
     */
    private boolean isIdle(Entity creature) {
        return creature.get(CreatureAi.class).getCreatureState() == CreatureState.IDLE
                && getMoodState(creature, entityData.getComponent(creature.getId(), CreatureMood.class)) == 0;
    }

    /**
     * @return 0 when the creature is content, 1 when unhappy, 2 when angry
     */
    private int getMoodState(Entity creature, CreatureMood mood) {
        return mood == null ? 0 : mood.getState(CreatureMood.toRuntimeThreshold(
                kwdFile.getCreature(creature.get(CreatureComponent.class).creatureId).getAttributes().getUnhappyThreshold()));
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
     * A creature the hand may take to put it to work: not resting, not in
     * a fight or fleeing one, not helpless and not dropped a short while ago
     */
    private boolean isFree(Entity creature) {
        CreatureState state = creature.get(CreatureAi.class).getCreatureState();

        return !battles.isLeftAlone(creature) && !battles.isResting(creature) && !KeeperAiBattles.HELPLESS.contains(state)
                && !KeeperAiBattles.FIGHTING.contains(state);
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
