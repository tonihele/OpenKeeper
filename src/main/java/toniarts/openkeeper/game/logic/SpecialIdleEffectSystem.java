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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.utils.WorldUtils;

/** Emits special crates' ambient stars independently of mouse interaction. */
public final class SpecialIdleEffectSystem implements IGameLogicUpdatable {
    private static final int IDLE_EFFECT = 204;
    private final EntityData entityData;
    private final EntitySet objects;
    private final IGameTimer timer;
    private final int ticksPerSecond;
    private final IMapInformation<?> map;
    private final GameEventService events;
    private long lastTick = -1;

    public SpecialIdleEffectSystem(EntityData entityData, IGameTimer timer, int ticksPerSecond,
            IMapInformation<?> map, GameEventService events) {
        this.entityData = entityData;
        this.timer = timer;
        this.ticksPerSecond = ticksPerSecond;
        this.map = map;
        this.events = events;
        objects = entityData.getEntities(ObjectViewState.class, Position.class);
    }

    @Override
    public void processTick(float tpf) {
        objects.applyChanges();
        long tick = Math.round(timer.getGameTime() * ticksPerSecond);
        if (tick == lastTick) {
            return;
        }
        lastTick = tick;
        for (Entity object : objects) {
            ObjectViewState view = object.get(ObjectViewState.class);
            // Native SPECIAL (0x4A2C00) emits 0xCC when (worldTick + thingIndex) & 15 == 0.
            // There is no hover, ownership, room-storage or creature-carrier test here.
            if (view.state != GameObject.State.SPECIAL || !view.visible
                    || ((tick + object.getId().getId()) & 15) != 0
                    || entityData.getComponent(object.getId(), InHand.class) != null) {
                continue;
            }
            Position position = object.get(Position.class);
            if (!map.isLava(WorldUtils.vectorToPoint(position.position))) {
                // Emitted effects stay at their birth position and expire independently of the crate.
                events.emitEffect(IDLE_EFFECT, position.position.clone(), 0);
            }
        }
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        objects.release();
    }
}
