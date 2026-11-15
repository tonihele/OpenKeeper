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
package toniarts.openkeeper.game.controller;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.random.RandomGenerator;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/** Runs the map's scripted effect generators on the simulation thread. */
public final class EffectGeneratorsController implements IGameLogicUpdatable {
    private final Map<Short, Generator> generators = new LinkedHashMap<>();
    private final IMapInformation<?> map;
    private final IGameTimer timer;
    private final GameEventService events;
    private final RandomGenerator random;

    public EffectGeneratorsController(Collection<Thing.EffectGenerator> things,
            IMapInformation<?> map, IGameTimer timer, GameEventService events) {
        this(things, map, timer, events, RandomGenerator.getDefault());
    }

    EffectGeneratorsController(Collection<Thing.EffectGenerator> things,
            IMapInformation<?> map, IGameTimer timer, GameEventService events, RandomGenerator random) {
        this.map = map;
        this.timer = timer;
        this.events = events;
        this.random = random;
        for (Thing.EffectGenerator thing : things) {
            generators.putIfAbsent(thing.getId(), new Generator(thing));
        }
    }

    /** Uses the stored generator ID directly; toggling does not restart its timer. */
    public void setEnabled(short id, boolean enabled) {
        Generator generator = generators.get(id);
        if (generator != null) {
            generator.enabled = enabled;
        }
    }

    @Override
    public void processTick(float tpf) {
        double now = timer.getGameTime();
        for (Generator generator : generators.values()) {
            if (!generator.enabled || now <= generator.nextEmission) {
                continue;
            }
            Thing.EffectGenerator thing = generator.thing;
            int slot = random.nextInt(4);
            int effect = slot < thing.getEffectIds().size() ? thing.getEffectIds().get(slot) : 0;
            if (effect == 0) {
                continue;
            }
            // Native placement uses 1/4096-tile steps in the configured rectangle.
            float x = thing.getPosX() + offset(thing.getWidth());
            float z = thing.getPosY() + offset(thing.getHeight());
            Point tile = WorldUtils.vectorToPoint(x, z);
            if (map.getMapData().getTile(tile) == null || map.isSolid(tile)) {
                continue;
            }
            float yaw = random.nextInt(2048) * FastMath.TWO_PI / 2048f;
            events.emitEffect(effect, new Vector3f(x, WorldUtils.FLOOR_HEIGHT, z), yaw);
            // Empty slots and rejected positions retry next tick, as in DKII.
            generator.nextEmission = now + thing.getFrequency();
        }
    }

    private float offset(int tiles) {
        return tiles == 0 ? 0f : random.nextInt(tiles * 4096) / 4096f;
    }

    @Override
    public void start() {
        // State is loaded with the map; a start notification must not undo script toggles.
    }

    @Override
    public void stop() {
        generators.clear();
    }

    private static final class Generator {
        private final Thing.EffectGenerator thing;
        private boolean enabled = true;
        private double nextEmission;

        private Generator(Thing.EffectGenerator thing) {
            this.thing = thing;
        }
    }
}
