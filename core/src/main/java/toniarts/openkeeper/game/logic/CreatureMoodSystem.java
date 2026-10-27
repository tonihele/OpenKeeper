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
import com.simsilica.es.EntitySet;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;

/**
 * Updates the independent native DKII creature anger reasons.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @author Wietse
 */
public final class CreatureMoodSystem implements IGameLogicUpdatable {

    private static final Logger logger = System.getLogger(CreatureMoodSystem.class.getName());
    private static final short LAIR_ROOM_ID = 2;
    private static final int LOG_COUNTER_INTERVAL = 1000;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final Room lair;
    private final Map<Short, IPlayerController> playerControllersById;
    private final EntitySet entities;

    public CreatureMoodSystem(EntityData entityData, IKwdFile kwdFile,
            Collection<IPlayerController> playerControllers) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        lair = kwdFile.getRoomById(LAIR_ROOM_ID);
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
        for (Entity entity : entities) {
            CreatureMood mood = entity.get(CreatureMood.class);
            CreatureSleep sleep = entity.get(CreatureSleep.class);
            Owner owner = entity.get(Owner.class);
            Creature creature = kwdFile.getCreature(entity.get(CreatureComponent.class).creatureId);
            if (creature == null) {
                continue;
            }

            int unhappyThreshold = CreatureMood.toRuntimeThreshold(
                    creature.getAttributes().getUnhappyThreshold());

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
                }
                continue;
            }

            if (creature.getLairObjectId() == 0
                    || !isLairAvailableOrOwned(owner.ownerId)) {
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

    private boolean isLairAvailableOrOwned(short playerId) {
        IPlayerController playerController = playerControllersById.get(playerId);
        return playerController != null && lair != null
                && (playerController.getRoomControl().getTypesAvailable().contains(lair)
                || playerController.getRoomControl().getTypeCount(lair) > 0);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        entities.release();
    }
}
