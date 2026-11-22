/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.view;

import com.jme3.app.state.AbstractAppState;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.WatchedEntity;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.sound.GlobalType;
import toniarts.openkeeper.game.sound.SoundCategory;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/** The positional carrying cue, driven by replicated hauling relationships. */
public final class HaulingSoundState extends AbstractAppState {

    private static final Logger logger = System.getLogger(HaulingSoundState.class.getName());

    private final EntityData entityData;
    private final EntitySet loads;
    private final Map<EntityId, Hauler> haulers = new HashMap<>();
    private final Supplier<SoundEngine> soundEngine;
    private final Function<Short, SoundCategory> category;
    private final Predicate<Point> visible;
    private final float requestInterval;
    private double elapsed;

    public HaulingSoundState(IKwdFile level, EntityData entityData, IFogOfWarInformation fog) {
        this(entityData, 1f / Math.max(1, level.getGameLevel().getTicksPerSec()), SoundManager::getEngine,
                id -> SoundManager.loadCategory(level.getCreature(id).getSoundCategory()), fog::isVisible);
    }

    HaulingSoundState(EntityData entityData, float requestInterval, Supplier<SoundEngine> soundEngine,
            Function<Short, SoundCategory> category, Predicate<Point> visible) {
        this.entityData = entityData;
        this.loads = entityData.getEntities(HauledBy.class, Position.class);
        this.requestInterval = requestInterval;
        this.soundEngine = soundEngine;
        this.category = category;
        this.visible = visible;
    }

    @Override
    public void update(float tpf) {
        elapsed += tpf;
        loads.applyChanges();
        Set<EntityId> carrying = new HashSet<>();
        for (Entity load : loads) {
            EntityId id = load.get(HauledBy.class).entityId;
            if (id != null && carrying.add(id)) {
                haulers.computeIfAbsent(id, Hauler::new).update();
            }
        }
        haulers.entrySet().removeIf(entry -> {
            if (carrying.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().release();
            return true;
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            releaseHaulers();
        }
    }

    @Override
    public void cleanup() {
        releaseHaulers();
        loads.release();
        super.cleanup();
    }

    private void releaseHaulers() {
        haulers.values().forEach(Hauler::release);
        haulers.clear();
    }

    private final class Hauler {
        private final WatchedEntity entity;
        private SoundEngine engine;
        private int handle = SoundEngine.NO_HANDLE;
        private short creatureId;
        private double nextRequest;

        Hauler(EntityId id) {
            entity = entityData.watchEntity(id, Position.class, CreatureViewState.class,
                    InHand.class, Death.class, Unconscious.class);
        }

        void update() {
            entity.applyChanges();
            Position position = entity.get(Position.class);
            CreatureViewState creature = entity.get(CreatureViewState.class);
            if (position == null || creature == null || entity.get(InHand.class) != null
                    || entity.get(Death.class) != null || entity.get(Unconscious.class) != null) {
                stop();
                return;
            }
            Point tile = WorldUtils.vectorToPoint(position.position);
            if (!visible.test(tile)) {
                stop();
                return;
            }
            SoundEngine current = soundEngine.get();
            if (engine != current || creatureId != creature.creatureId) {
                stop();
                engine = current;
                creatureId = creature.creatureId;
                nextRequest = 0;
            }
            if (engine == null) {
                return;
            }
            // DKII's hauling updater (0x48B5F0) requests 0x10D at the hauler's position,
            // even if it has paused. The active handle is reused, not layered each tick.
            if (handle != SoundEngine.NO_HANDLE && engine.move(handle, tile)) {
                return;
            }
            handle = SoundEngine.NO_HANDLE;
            if (elapsed >= nextRequest) {
                nextRequest = elapsed + requestInterval;
                SoundCategory sounds = category.apply(creature.creatureId);
                if (sounds != null) {
                    handle = engine.play(sounds, GlobalType.CREATURE_DRAG.getId(), tile);
                    if (handle != SoundEngine.NO_HANDLE) {
                        logger.log(Level.DEBUG, "Hauling sound 0x10D for {0} at {1}, handle {2}", entity.getId(), tile, handle);
                    }
                }
            }
        }

        void stop() {
            if (engine != null && handle != SoundEngine.NO_HANDLE) {
                logger.log(Level.DEBUG, "Stop hauling sound for {0}, handle {1}", entity.getId(), handle);
                engine.stop(handle);
            }
            handle = SoundEngine.NO_HANDLE;
        }

        void release() {
            stop();
            entity.release();
        }
    }
}
