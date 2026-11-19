/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;

/**
 * Persistent presentation of one Heart assembly. Times use the server's
 * monotonic nanosecond clock (also used by Ethereal), not a client's load time.
 * Completed state survives furniture reconstruction so it cannot replay.
 */
public final class DungeonHeartIntroduction implements EntityComponent {

    // Native CRoom::tickDungeonHeart (DKII 1.7, 0x4D9A60), not clip durations.
    public static final double REVEAL_START = 7;
    public static final double COMPLETE_TIME = 9;
    public static final int WAIT_EFFECT = 223;
    public static final int START_EFFECT = 225;
    public static final int REVEAL_EFFECT = 314;

    public EntityId heart, arches, step0, step1, step2;
    public long startTime;
    public long waitingStartTime;
    public long pausedAt;
    public int ticksPerSecond;
    public boolean complete;

    public DungeonHeartIntroduction() {
        // Network serialization.
    }

    public DungeonHeartIntroduction(DungeonHeartIntroduction previous, EntityId heart,
            EntityId arches, EntityId step0, EntityId step1, EntityId step2) {
        if (previous != null) {
            startTime = previous.startTime;
            waitingStartTime = previous.waitingStartTime;
            pausedAt = previous.pausedAt;
            ticksPerSecond = previous.ticksPerSecond;
            complete = previous.complete;
        }
        this.heart = heart;
        this.arches = arches;
        this.step0 = step0;
        this.step1 = step1;
        this.step2 = step2;
    }

    public DungeonHeartIntroduction(DungeonHeartIntroduction previous) {
        this(previous, previous.heart, previous.arches, previous.step0, previous.step1, previous.step2);
    }

    public double age(long now) {
        return startTime == 0 ? Double.NEGATIVE_INFINITY
                : ((pausedAt == 0 ? now : pausedAt) - startTime) / 1_000_000_000.0;
    }

    public float opacity(long now) {
        if (complete) {
            return 1;
        }
        if (startTime == 0) {
            return 0;
        }
        double age = age(now);
        if (age >= COMPLETE_TIME) {
            return 1;
        }
        return (float) Math.clamp((age - REVEAL_START) * ticksPerSecond * 32 / 255.0, 0, 1);
    }
}
