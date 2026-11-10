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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.OnFire;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * Keeps the creatures that are on fire burning until the fire goes out. As in
 * the original (DKII.EXE 0x48E160) a burning creature gives off three Fire
 * Flame effects every game tick, at random spots within its width and height.
 * The fire itself doesn't hurt.
 */
public final class OnFireSystem implements IGameLogicUpdatable {

    /**
     * The "Fire Flame Effect", the original has it hard coded
     */
    private static final int FIRE_FLAME_EFFECT_ID = 88;
    private static final int FLAMES_PER_TICK = 3;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IGameTimer gameTimer;
    private final GameEventService gameEventService;
    private final EntitySet burningCreatures;

    public OnFireSystem(EntityData entityData, IKwdFile kwdFile, IGameTimer gameTimer, GameEventService gameEventService) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameTimer = gameTimer;
        this.gameEventService = gameEventService;
        burningCreatures = entityData.getEntities(OnFire.class, CreatureComponent.class, Position.class);
    }

    @Override
    public void processTick(float tpf) {
        double gameTime = gameTimer.getGameTime();
        burningCreatures.applyChanges();

        for (Entity entity : burningCreatures) {
            if (entity.get(OnFire.class).endTime <= gameTime) {
                entityData.removeComponent(entity.getId(), OnFire.class);
            } else {
                burn(entity.get(CreatureComponent.class), entity.get(Position.class).position);
            }
        }
    }

    private void burn(CreatureComponent creatureComponent, Vector3f position) {
        Creature creature = kwdFile.getCreature(creatureComponent.creatureId);
        float width = creature != null ? creature.getAttributes().getWidth() : 0;
        for (int i = 0; i < FLAMES_PER_TICK; i++) {
            Vector3f flame = new Vector3f(position.x + (FastMath.nextRandomFloat() - 0.5f) * width,
                    position.y + FastMath.nextRandomFloat() * creatureComponent.height,
                    position.z + (FastMath.nextRandomFloat() - 0.5f) * width);
            gameEventService.emitEffect(FIRE_FLAME_EFFECT_ID, flame, 0);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        burningCreatures.release();
    }

}
