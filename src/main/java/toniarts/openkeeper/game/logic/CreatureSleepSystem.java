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
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.creature.CreatureState;
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

    public CreatureSleepSystem(EntityData entityData, IKwdFile kwdFile,
            IGameTimer gameTimer) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameTimer = gameTimer;
        entities = entityData.getEntities(CreatureSleep.class,
                CreatureComponent.class, CreatureAi.class);
    }

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
        if (state != CreatureState.SLEEPING
                && gameTime - lastSleepTime >= timeAwake) {
            lastSleepTime = gameTime;
            sleepNeed = CreatureSleep.toRuntimeSleepNeed(timeSleep);
        } else if (state == CreatureState.SLEEPING && sleepNeed != 0) {
            sleepNeed--;
        }

        if (lastSleepTime == sleep.lastSleepTime && sleepNeed == sleep.sleepNeed) {
            return sleep;
        }
        return new CreatureSleep(sleep.lairObjectId, lastSleepTime,
                sleep.sleepStartTime, sleepNeed);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        entities.release();
    }
}
