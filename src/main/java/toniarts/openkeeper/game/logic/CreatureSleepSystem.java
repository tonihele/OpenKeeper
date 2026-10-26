/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * Maintains DKII's sleep-debt clock on the authoritative game loop. Creature
 * state code remains responsible for travelling to and leaving the bed.
 *
 * @author Wietse
 */
public final class CreatureSleepSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IGameTimer gameTimer;
    private final EntitySet entities;

    /**
     * Watches sleeping-capable creatures on the authoritative game loop.
     *
     * @param entityData authoritative creature components
     * @param kwdFile creature definitions containing awake and sleep durations
     * @param gameTimer shared game clock used for fatigue and rest timestamps
     */
    public CreatureSleepSystem(EntityData entityData, IKwdFile kwdFile,
            IGameTimer gameTimer) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameTimer = gameTimer;
        entities = entityData.getEntities(CreatureSleep.class,
                CreatureComponent.class, CreatureAi.class);
    }

    /**
     * Updates fatigue once per game tick, consuming one debt unit during rest.
     *
     * @param tpf elapsed game time for this tick; timestamps use the shared clock
     */
    @Override
    public void processTick(float tpf) {
        entities.applyChanges();
        double gameTime = gameTimer.getGameTime();
        for (Entity entity : entities) {
            CreatureSleep sleep = entity.get(CreatureSleep.class);
            CreatureAi ai = entity.get(CreatureAi.class);
            Creature creature = kwdFile.getCreature(
                    entity.get(CreatureComponent.class).creatureId);
            if (creature == null) {
                continue;
            }

            TaskComponent task = entityData.getComponent(entity.getId(), TaskComponent.class);
            if (ai.getCreatureState() == CreatureState.WORK && task != null
                    && task.taskType == TaskType.CLAIM_LAIR && task.taskStarted) {
                // Native lair activity skips needs accumulation (also applies to no-bed sleepers).
                continue;
            }
            CreatureSleep updated = updateSleep(sleep, ai.getCreatureState(), gameTime,
                    creature.getAttributes().getTimeAwake(),
                    creature.getAttributes().getTimeSleep());
            if (updated != sleep) {
                entityData.setComponent(entity.getId(), updated);
            }
        }
    }

    static CreatureSleep updateSleep(CreatureSleep sleep, CreatureState state,
            double gameTime, int timeAwake, int timeSleep) {
        double lastSleepTime = sleep.lastSleepTime;
        int sleepNeed = sleep.sleepNeed;
        if (timeSleep <= 0) {
            return sleep;
        }
        if (state == CreatureState.SLEEPING || state == CreatureState.RECUPERATING) {
            // Native 0x46fa60 refreshes this even after debt is zero, until health recovers.
            lastSleepTime = gameTime;
            sleepNeed = Math.max(0, sleepNeed - 1);
        } else if (state != CreatureState.EATING && state != CreatureState.IMPRISONED
                && state != CreatureState.TORTURED && state != CreatureState.UNCONSCIOUS
                && state != CreatureState.DEAD && gameTime - lastSleepTime >= timeAwake) {
            lastSleepTime = gameTime;
            sleepNeed = CreatureSleep.toRuntimeSleepNeed(timeSleep);
        }

        if (lastSleepTime == sleep.lastSleepTime && sleepNeed == sleep.sleepNeed) {
            return sleep;
        }
        return new CreatureSleep(sleep.lairObjectId, lastSleepTime,
                sleep.sleepStartTime, sleepNeed);
    }

    /** Starts the system; its entity subscription is established on construction. */
    @Override
    public void start() {
    }

    /** Releases the creature subscription when the game loop stops. */
    @Override
    public void stop() {
        entities.release();
    }
}
