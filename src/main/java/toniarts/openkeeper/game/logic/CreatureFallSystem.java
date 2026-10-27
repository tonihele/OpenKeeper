/*
 * Copyright (C) 2014-2018 OpenKeeper
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

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Handles creature falling (dropped from hand). In the future maybe all these would be handled by a physics
 * thingie?<br>
 * A thrown creature flies with its velocity and the gravity of the level, and
 * bounces off the walls with half its speed, as in Dungeon Keeper II (0x4B2030).
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureFallSystem implements IGameLogicUpdatable {

    private static final float GRAVITY = 2.0f;

    /**
     * The level's gravity constant is in 1/256 tiles per turn per turn
     */
    private static final float GRAVITY_CONSTANT_SCALE = 1 / 256f;

    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final GameEventService gameEventService;
    private final IMapInformation<?> mapInformation;
    private final int ticksPerSecond;
    private final float gravity;
    private final EntitySet fallEntities;

    /**
     * Creates the system
     *
     * @param entityData the entity data
     * @param gameTimer the game timer
     * @param gameEventService the game events, the landing is emitted
     * @param mapInformation the map, the thrown creatures bounce off its walls
     * @param ticksPerSecond the turns in a second
     * @param gravityConstant the level's gravity constant
     */
    public CreatureFallSystem(EntityData entityData, IGameTimer gameTimer, GameEventService gameEventService,
            IMapInformation<?> mapInformation, int ticksPerSecond, float gravityConstant) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;
        this.gameEventService = gameEventService;
        this.mapInformation = mapInformation;
        this.ticksPerSecond = ticksPerSecond;
        this.gravity = gravityConstant * GRAVITY_CONSTANT_SCALE;

        fallEntities = entityData.getEntities(CreatureFall.class, Position.class, CreatureComponent.class);
    }

    @Override
    public void processTick(float tpf) {
        // Add new & remove old
        fallEntities.applyChanges();

        // Process ticks
        for (Entity entity : fallEntities) {
            Position position = entity.get(Position.class);
            CreatureFall creatureFall = entity.get(CreatureFall.class);
            Position newPosition = new Position(position.rotation, position.position);
            if (creatureFall.velocity != null) {
                fly(entity, creatureFall, newPosition.position, tpf * ticksPerSecond);
            } else {
                newPosition.position.y = Math.max(newPosition.position.y - tpf * GRAVITY, WorldUtils.FLOOR_HEIGHT);
            }
            entity.set(newPosition);
            if (newPosition.position.y == WorldUtils.FLOOR_HEIGHT) {

                // We'll just remove this and add the AI
                entityData.removeComponent(entity.getId(), CreatureFall.class);
                if (creatureFall.damage > 0) {
                    EntityController.setDamage(entityData, entity.getId(), creatureFall.damage);
                }

                // The landing, on touchdown (hand drops and conjured creatures alike)
                gameEventService.emit(GameEventType.CREATURE_LAND, entity.getId(), newPosition.position);

                // Preserve any requested AI target through landing and get-up.
                // The state, depending on the landing
                CreatureComponent creatureComponent = entity.get(CreatureComponent.class);
                CreatureAi ai = entityData.getComponent(entity.getId(), CreatureAi.class);
                entityData.setComponent(entity.getId(), new CreatureAi(gameTimer.getGameTime(), creatureComponent.stunDuration > 0f ? CreatureState.FALLEN : CreatureState.IDLE, creatureComponent.creatureId,
                        ai == null ? null : ai.getTargetState()));
            }
        }
    }

    /**
     * Moves a thrown creature, the gravity pulls it down and a wall in the way
     * turns it back with half its speed (0x4B2030)
     *
     * @param position the position, moved in place
     * @param turns the turns passed
     */
    private void fly(Entity entity, CreatureFall creatureFall, Vector3f position, float turns) {
        Vector3f velocity = creatureFall.velocity.clone();
        velocity.y -= gravity * turns;
        if (isSolid(position.x + velocity.x * turns, position.z)) {
            velocity.x = -velocity.x / 2;
        } else {
            position.x += velocity.x * turns;
        }
        if (isSolid(position.x, position.z + velocity.z * turns)) {
            velocity.z = -velocity.z / 2;
        } else {
            position.z += velocity.z * turns;
        }
        position.y = Math.max(position.y + velocity.y * turns, WorldUtils.FLOOR_HEIGHT);
        entity.set(new CreatureFall(velocity, creatureFall.damage));
    }

    private boolean isSolid(float x, float z) {
        Point tile = WorldUtils.vectorToPoint(x, z);
        return mapInformation.getMapData().getTile(tile) == null || mapInformation.isSolid(tile);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        fallEntities.release();
    }

}
