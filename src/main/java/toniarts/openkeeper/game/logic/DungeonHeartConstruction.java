/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.function.LongSupplier;
import toniarts.openkeeper.game.component.DungeonHeartIntroduction;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.RoomComponent;

/** Schedules Heart presentation; meshes, effects and sound belong to the client. */
public final class DungeonHeartConstruction implements IGameLogicUpdatable {
    private final EntityData entityData;
    private final EntitySet hearts;
    private final LongSupplier clock;
    private final long delayNanos;
    private final int ticksPerSecond;
    private long startTime;
    private long pausedAt;

    public DungeonHeartConstruction(EntityData entityData, float delay, int ticksPerSecond) {
        this(entityData, delay, ticksPerSecond, System::nanoTime);
    }

    DungeonHeartConstruction(EntityData entityData, float delay, int ticksPerSecond, LongSupplier clock) {
        if (ticksPerSecond <= 0 || !Float.isFinite(delay) || delay < 0) {
            throw new IllegalArgumentException("Invalid Heart construction clock");
        }
        this.entityData = entityData;
        this.clock = clock;
        this.ticksPerSecond = ticksPerSecond;
        // The native start tick is delay * gameTicksPerSecond + 1.
        delayNanos = Math.round((Math.round(delay * ticksPerSecond) + 1.0)
                / ticksPerSecond * 1_000_000_000.0);
        hearts = entityData.getEntities(DungeonHeartIntroduction.class, RoomComponent.class);
    }

    @Override
    public synchronized void start() {
        startTime = clock.getAsLong() + delayNanos;
        processTick(0);
    }

    @Override
    public synchronized void processTick(float tpf) {
        hearts.applyChanges();
        if (startTime == 0) {
            return;
        }
        long now = clock.getAsLong();
        for (Entity entity : hearts) {
            DungeonHeartIntroduction intro = entity.get(DungeonHeartIntroduction.class);
            Health health = entityData.getComponent(intro.heart, Health.class);
            // Missing furniture can be a reconstruction in progress. Room
            // destruction is authoritative; absence alone must not erase the
            // epoch and replay a completed introduction when new parts arrive.
            if (entity.get(RoomComponent.class).destroyed || health != null && health.health <= 0) {
                entityData.removeComponent(entity.getId(), DungeonHeartIntroduction.class);
            } else if (intro.startTime == 0) {
                DungeonHeartIntroduction scheduled = new DungeonHeartIntroduction(intro);
                scheduled.startTime = Math.max(startTime, now);
                scheduled.waitingStartTime = Math.max(startTime - delayNanos, now);
                scheduled.ticksPerSecond = ticksPerSecond;
                scheduled.pausedAt = pausedAt;
                entityData.setComponent(entity.getId(), scheduled);
            } else if (!intro.complete && intro.age(now) >= DungeonHeartIntroduction.COMPLETE_TIME) {
                DungeonHeartIntroduction completed = new DungeonHeartIntroduction(intro);
                completed.complete = true;
                entityData.setComponent(entity.getId(), completed);
            }
        }
    }

    /** Called after the game loops have acknowledged pause, including room construction. */
    public synchronized void pause() {
        if (pausedAt != 0) {
            return;
        }
        pausedAt = clock.getAsLong();
        hearts.applyChanges();
        for (Entity entity : hearts) {
            DungeonHeartIntroduction paused = new DungeonHeartIntroduction(entity.get(DungeonHeartIntroduction.class));
            paused.pausedAt = pausedAt;
            entityData.setComponent(entity.getId(), paused);
        }
    }

    public synchronized void resume() {
        if (pausedAt == 0) {
            return;
        }
        long pauseDuration = clock.getAsLong() - pausedAt;
        startTime += pauseDuration;
        hearts.applyChanges();
        for (Entity entity : hearts) {
            DungeonHeartIntroduction resumed = new DungeonHeartIntroduction(entity.get(DungeonHeartIntroduction.class));
            if (resumed.startTime != 0) {
                resumed.startTime += pauseDuration;
                resumed.waitingStartTime += pauseDuration;
            }
            resumed.pausedAt = 0;
            entityData.setComponent(entity.getId(), resumed);
        }
        pausedAt = 0;
    }

    @Override
    public synchronized void stop() {
        hearts.release();
    }
}
