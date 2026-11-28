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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.event.EventBus;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CallToArms;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureHunger;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.KeeperAiMood;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.event.CreatureUpset;
import toniarts.openkeeper.game.event.ServerEvents;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * Updates the independent native DKII creature anger reasons.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @author Wietse
 */
public final class CreatureMoodSystem implements IGameLogicUpdatable {

    private static final Logger logger = System.getLogger(CreatureMoodSystem.class.getName());
    private static final short LAIR_ROOM_ID = 2;
    private static final short HATCHERY_ROOM_ID = 4;
    private static final int LOG_COUNTER_INTERVAL = 1000;
    private static final int SECONDS_IN_HAND_BEFORE_ANGER = 20;

    /**
     * Prison, torture and fight states, DKII adds no anger for the needs in
     * them (DKII.EXE 0x478D00 skips the states with state flag 2 and the fight
     * group 0x16, which also covers walking to a fight)
     */
    private static final Set<CreatureState> NEED_ANGER_HELD_STATES = EnumSet.of(CreatureState.FIGHT,
            CreatureState.MELEE_ATTACK, CreatureState.CAST_SPELL, CreatureState.BACK_OFF,
            CreatureState.IMPRISONED, CreatureState.TORTURED, CreatureState.ENTERING_DUNGEON);

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final Room lair;
    private final Room hatchery;
    private final IGameTimer gameTimer;
    private final int angerRecoveryInLairPerSecond;
    private final Map<Short, IPlayerController> playerControllersById;
    private final EntitySet entities;

    public CreatureMoodSystem(EntityData entityData, IKwdFile kwdFile,
            Collection<IPlayerController> playerControllers, IGameTimer gameTimer,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameTimer = gameTimer;
        angerRecoveryInLairPerSecond = (int) gameSettings.get(
                Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_CREATURE_IN_LAIR_PER_SECOND).getValue();
        lair = kwdFile.getRoomById(LAIR_ROOM_ID);
        hatchery = kwdFile.getRoomById(HATCHERY_ROOM_ID);
        playerControllersById = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController playerController : playerControllers) {
            playerControllersById.put(playerController.getKeeper().getId(), playerController);
        }
        entities = entityData.getEntities(CreatureMood.class, CreatureComponent.class,
                CreatureSleep.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        entities.applyChanges();
        for (Entity entity : entities.getChangedEntities()) {
            tellMood(entity);
        }
        for (Entity entity : entities) {
            CreatureMood mood = entity.get(CreatureMood.class);
            CreatureSleep sleep = entity.get(CreatureSleep.class);
            Owner owner = entity.get(Owner.class);

            // DKII only runs the anger reasons for keepers (player index above 2),
            // heroes and neutral creatures have no moods
            if (owner.ownerId <= Player.NEUTRAL_PLAYER_ID) {
                continue;
            }

            Creature creature = kwdFile.getCreature(entity.get(CreatureComponent.class).creatureId);
            if (creature == null) {
                continue;
            }
            mood = applyInHandAnger(entity, mood, creature, tpf);

            int unhappyThreshold = CreatureMood.toRuntimeThreshold(
                    creature.getAttributes().getUnhappyThreshold());
            boolean needAngerHeld = isNeedAngerHeld(entityData, entity.getId(), creature);
            if (!needAngerHeld) {
                mood = applyNoFoodAnger(entity, mood, creature, owner.ownerId);
            }

            // DKII clears reason 2 immediately after a bed is successfully assigned.
            if (hasAssignedLair(sleep)) {
                if (mood.noLair != 0) {
                    CreatureMood updatedMood = mood.clear(CreatureMood.REASON_NO_LAIR);
                    entityData.setComponent(entity.getId(), updatedMood);
                    logger.log(Level.INFO,
                            "Creature mood: {0} ({1}, player {2}) assigned bed {3}; "
                            + "cleared no-lair {4}->0, state {5}->{6}, counters {7}->{8}",
                            new Object[]{creature.getName(), entity.getId(), owner.ownerId,
                                sleep.lairObjectId, mood.noLair,
                                stateName(mood.getState(unhappyThreshold)),
                                stateName(updatedMood.getState(unhappyThreshold)),
                                counters(mood), counters(updatedMood)});
                    mood = updatedMood;
                }
                applyLairMoodRecovery(entity, mood);
                continue;
            }

            if (needAngerHeld || creature.getLairObjectId() == 0
                    || !isRoomAvailableOrOwned(owner.ownerId, lair)) {
                continue;
            }

            int anger = CreatureMood.toRuntimeValue(creature.getAttributes().getAngerNoLair());
            if (anger != 0) {
                CreatureMood updatedMood = mood.add(CreatureMood.REASON_NO_LAIR, anger);
                entityData.setComponent(entity.getId(), updatedMood);
                int oldState = mood.getState(unhappyThreshold);
                int newState = updatedMood.getState(unhappyThreshold);
                if (mood.noLair == 0
                        || mood.noLair / LOG_COUNTER_INTERVAL
                        != updatedMood.noLair / LOG_COUNTER_INTERVAL
                        || oldState != newState) {
                    logger.log(Level.INFO,
                            "Creature mood: {0} ({1}, player {2}) no-lair {3}->{4} "
                            + "(+{5}), threshold {6}, state {7}->{8}, counters {9}",
                            new Object[]{creature.getName(), entity.getId(), owner.ownerId,
                                mood.noLair, updatedMood.noLair, anger, unhappyThreshold,
                                stateName(oldState), stateName(newState), counters(updatedMood)});
                }
            }
        }
    }

    /**
     * Keeps the mood a keeper's creature is in, and tells the keeper when it
     * turns unhappy or angry. DK II posts its messages 0x11 and 0x12 then,
     * when the anger rises past a threshold (0x478B80, 0x478C30); calming
     * down is no news. An emotionless creature type (creature data +0x68A
     * bit 4) is never news, DK II changes no anger for it there
     */
    private void tellMood(Entity entity) {
        Owner owner = entity.get(Owner.class);
        Creature creature = kwdFile.getCreature(entity.get(CreatureComponent.class).creatureId);
        if (owner.ownerId <= Player.NEUTRAL_PLAYER_ID || creature == null
                || creature.getFlags2() != null && creature.getFlags2().contains(Creature.CreatureFlag2.IS_EMOTIONLESS)) {
            return;
        }

        int state = entity.get(CreatureMood.class).getState(CreatureMood.toRuntimeThreshold(creature.getAttributes().getUnhappyThreshold()));
        KeeperAiMood told = entityData.getComponent(entity.getId(), KeeperAiMood.class);
        int toldState = told != null ? told.state : 0;
        if (state == toldState) {
            return;
        }
        if (state == 0) {
            entityData.removeComponent(entity.getId(), KeeperAiMood.class);
        } else {
            entityData.setComponent(entity.getId(), new KeeperAiMood(state));
        }
        if (state > toldState) {
            EventBus.publish(ServerEvents.CREATURE_UPSET, new CreatureUpset(owner.ownerId, entity.getId(), state == 2));
        }
    }

    private void applyLairMoodRecovery(Entity entity, CreatureMood mood) {
        CreatureRecuperating recuperating = entityData.getComponent(
                entity.getId(), CreatureRecuperating.class);
        if (recuperating == null
                || gameTimer.getGameTime() - recuperating.moodCheckTime < 1) {
            return;
        }

        entityData.setComponent(entity.getId(), new CreatureRecuperating(
                recuperating.startTime, recuperating.healthCheckTime,
                recuperating.moodCheckTime + 1));
        if (angerRecoveryInLairPerSecond != 0) {
            entityData.setComponent(entity.getId(), recoverInLair(
                    mood, angerRecoveryInLairPerSecond));
        }
    }

    static CreatureMood recoverInLair(CreatureMood mood, int angerChange) {
        return mood.add(CreatureMood.REASON_OTHER, angerChange);
    }

    /**
     * Whether DKII adds no anger for the creature's needs, no lair, no food
     * and no pay, this tick. The creature tick (DKII.EXE 0x476FC0) only adds
     * it (0x478D00) for the keepers' creatures that aren't workers, and a
     * possessed creature doesn't get the tick at all (0x488930). 0x478D00 then
     * adds none to a Horned Reaper (creature flag 0x4000), a creature held
     * back by the Call to Arms (0x4888C0), a prisoner (creature +0x3CB), and a
     * creature in a prison, torture or fight state.
     *
     * @param entityData the entity data
     * @param entityId the creature
     * @param creature the creature's type
     * @return true when the creature gets no anger for its needs
     */
    public static boolean isNeedAngerHeld(EntityData entityData, EntityId entityId, Creature creature) {
        CreatureAi creatureAi = entityData.getComponent(entityId, CreatureAi.class);
        return creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER)
                || creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)
                || entityData.getComponent(entityId, Possessed.class) != null
                || entityData.getComponent(entityId, CreatureImprisoned.class) != null
                || entityData.getComponent(entityId, CreatureTortured.class) != null
                || creatureAi != null && NEED_ANGER_HELD_STATES.contains(creatureAi.getCreatureState())
                || isTorturingItself(entityData, entityId)
                || isHeldByCallToArms(entityData, entityId);
    }

    /**
     * Whether the Call to Arms holds the creature back (DKII.EXE 0x4888C0):
     * while its keeper's banner is up (CPlayer +0x76 bit 2, set by 0x4BDDA0
     * when the banner goes up and cleared by 0x4BDFC0 when it goes), and while
     * the creature answers the call. Such a creature gets no wage on pay day
     * (0x4BE1A0), no anger for its needs (0x478D00) and picks no mood job
     * (0x476FC0).
     *
     * @param entityData the entity data
     * @param entityId the creature
     * @return true while the Call to Arms holds the creature back
     */
    public static boolean isHeldByCallToArms(EntityData entityData, EntityId entityId) {
        CreatureAi creatureAi = entityData.getComponent(entityId, CreatureAi.class);
        if (creatureAi != null && creatureAi.getCreatureState() == CreatureState.CALL_TO_ARMS) {
            return true;
        }
        Owner owner = entityData.getComponent(entityId, Owner.class);
        for (EntityId banner : entityData.findEntities(null, CallToArms.class)) {
            Owner bannerOwner = entityData.getComponent(banner, Owner.class);
            if (owner != null && bannerOwner != null && bannerOwner.ownerId == owner.ownerId
                    && entityData.getComponent(banner, CallToArms.class).isUp()) {
                return true;
            }
        }
        return false;
    }

    /**
     * A creature that is on a torture device because it likes it. DKII's on
     * device state 0x83 has state flag 2.
     */
    private static boolean isTorturingItself(EntityData entityData, EntityId entityId) {
        TaskComponent task = entityData.getComponent(entityId, TaskComponent.class);
        return task != null && task.taskType == TaskType.TORTURE && task.taskStarted
                && (task.targetEntity == null || entityData.getComponent(task.targetEntity, CreatureComponent.class) == null);
    }

    /**
     * DKII changes the shared "other" anger of a creature held in the hand for
     * over 20 seconds once every game second. The 20 seconds are fixed, the
     * level's TIME_IN_HAND_BEFORE_CREATURES_BECOME_ANGRY_SECONDS isn't read.
     * Most creatures calm down in the hand, a few get angrier.
     */
    private CreatureMood applyInHandAnger(Entity entity, CreatureMood mood, Creature creature, float tpf) {
        InHand inHand = entityData.getComponent(entity.getId(), InHand.class);
        double gameTime = gameTimer.getGameTime();
        if (inHand == null || gameTime - inHand.startTime <= SECONDS_IN_HAND_BEFORE_ANGER
                || Math.floor(gameTime) == Math.floor(gameTime - tpf)) {
            return mood;
        }

        CreatureMood updatedMood = mood.add(CreatureMood.REASON_OTHER,
                CreatureMood.toRuntimeValue(creature.getAttributes().getAngerInHand()));
        if (updatedMood.other == mood.other) {
            return mood;
        }
        entityData.setComponent(entity.getId(), updatedMood);
        return updatedMood;
    }

    private static String stateName(int state) {
        return switch (state) {
            case 0 -> "normal";
            case 1 -> "unhappy";
            case 2 -> "angry";
            default -> Integer.toString(state);
        };
    }

    private static String counters(CreatureMood mood) {
        return "[general=" + mood.general
                + ", food=" + mood.noFood
                + ", lair=" + mood.noLair
                + ", work=" + mood.noWork
                + ", pay=" + mood.noPay
                + ", other=" + mood.other + ']';
    }

    private boolean hasAssignedLair(CreatureSleep sleep) {
        return sleep.lairObjectId != null
                && entityData.getComponent(sleep.lairObjectId, Position.class) != null;
    }

    /**
     * DKII adds no-food anger every tick while a creature still waits for its
     * whole fill of chickens and its keeper can build or has a Hatchery.
     */
    private CreatureMood applyNoFoodAnger(Entity entity, CreatureMood mood, Creature creature, short ownerId) {
        CreatureHunger hunger = entityData.getComponent(entity.getId(), CreatureHunger.class);
        int hungerFill = entity.get(CreatureComponent.class).hungerFill;
        if (hunger == null || hungerFill == 0 || hunger.amountNeeded < hungerFill
                || !isRoomAvailableOrOwned(ownerId, hatchery)) {
            return mood;
        }

        CreatureMood updatedMood = mood.add(CreatureMood.REASON_NO_FOOD,
                CreatureMood.toRuntimeValue(creature.getAttributes().getAngerNoFood()));
        if (updatedMood.noFood == mood.noFood) {
            return mood;
        }
        entityData.setComponent(entity.getId(), updatedMood);
        return updatedMood;
    }

    private boolean isRoomAvailableOrOwned(short playerId, Room room) {
        IPlayerController playerController = playerControllersById.get(playerId);
        return playerController != null && room != null
                && (playerController.getRoomControl().getTypesAvailable().contains(room)
                || playerController.getRoomControl().getTypeCount(room) > 0);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        entities.release();
    }
}
