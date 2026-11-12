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
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Handles creature falling (dropped from hand). In the future maybe all these would be handled by a physics
 * thingie?<br>
 * A thrown creature flies with its velocity and the gravity of the level, and
 * bounces off the walls with half its speed, as in Dungeon Keeper II (0x4B2030).
 * This only brings the creature down, the {@link CreatureLandingSystem} lands it.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureFallSystem implements IGameLogicUpdatable {

    private static final float GRAVITY = 2.0f;

    /**
     * The level's gravity constant is in 1/256 tiles per turn per turn
     */
    private static final float GRAVITY_CONSTANT_SCALE = 1 / 256f;

    private final IMapInformation<?> mapInformation;
    private final int ticksPerSecond;
    private final float gravity;
    private final EntitySet fallEntities;

    /**
     * Creates the system
     *
     * @param entityData the entity data
     * @param mapInformation the map, the thrown creatures bounce off its walls
     * @param ticksPerSecond the turns in a second
     * @param gravityConstant the level's gravity constant
     */
    public CreatureFallSystem(EntityData entityData, IMapInformation<?> mapInformation, int ticksPerSecond,
            float gravityConstant) {
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
            if (position.position.y <= WorldUtils.FLOOR_HEIGHT) {

                // Down already, waiting for the game logic to land it
                continue;
            }
            CreatureFall creatureFall = entity.get(CreatureFall.class);

            // A vector of its own: the game logic lands the creature when it sees it on the floor, and that
            // must not be before the last velocity is written
            Position newPosition = new Position(position.rotation, position.position.clone());
            if (creatureFall.velocity != null) {
                fly(entity, creatureFall, newPosition.position, tpf * ticksPerSecond);
            } else {
                newPosition.position.y = Math.max(newPosition.position.y - tpf * GRAVITY, WorldUtils.FLOOR_HEIGHT);
            }
            entity.set(newPosition);
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
