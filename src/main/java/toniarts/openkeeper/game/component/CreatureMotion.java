/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;

/** Coherent authoritative presentation publication. Treat fields as read-only. */
public final class CreatureMotion implements EntityComponent {
    public double time, interval;
    public long epoch, sequence;
    public float x, y, z, yaw;
    public CreatureViewState request;
    public com.simsilica.es.EntityId hauler;
    public boolean immediate;

    public CreatureMotion() { }
    public CreatureMotion(double time, double interval, long sequence, Position position,
                          CreatureViewState request, boolean immediate) {
        this.time = time; this.interval = interval; this.sequence = sequence; this.epoch = position.motionEpoch;
        this.x = position.position.x; this.y = position.position.y; this.z = position.position.z; this.yaw = position.rotation;
        this.request = new CreatureViewState(request.creatureId, request.stateStartTime, request.state, request.sequence);
        this.immediate = immediate;
    }
}
