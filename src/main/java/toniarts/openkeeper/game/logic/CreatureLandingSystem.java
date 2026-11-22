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
import com.simsilica.es.EntityId;
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
 * Lands the creatures that the {@link CreatureFallSystem} has brought down to the floor. The fall is animated
 * on the animation loop, but the landing changes the AI state of the creature and damages a thrown one, so it
 * is done here on the game logic thread, like everything else that touches the AI and the damage.<br>
 * A thrown creature flies here too, like in Dungeon Keeper II a whole turn on every tick, with its velocity
 * and the gravity of the level, and bounces off the walls with half its speed (0x4B2030). It lands on the
 * tick it comes down (0x4833C0).
 */
public final class CreatureLandingSystem implements IGameLogicUpdatable {

    /**
     * A thrown creature raises dust as it lands (0x4833C0)
     */
    private static final int FOOT_DUST_EFFECT_ID = 117;

    /**
     * The level's gravity constant is in 1/256 tiles per turn per turn
     */
    private static final float GRAVITY_CONSTANT_SCALE = 1 / 256f;

    private final EntityData entityData;
    private final IMapInformation<?> mapInformation;
    private final float gravity;
    private final IGameTimer gameTimer;
    private final GameEventService gameEventService;
    private final EntitySet fallEntities;

    /**
     * Creates the system
     *
     * @param entityData the entity data
     * @param mapInformation the map, the thrown creatures bounce off its walls
     * @param gravityConstant the level's gravity constant
     * @param gameTimer the game timer
     * @param gameEventService the game events, for the landing and its dust
     */
    public CreatureLandingSystem(EntityData entityData, IMapInformation<?> mapInformation, float gravityConstant,
            IGameTimer gameTimer, GameEventService gameEventService) {
        this.entityData = entityData;
        this.mapInformation = mapInformation;
        this.gravity = gravityConstant * GRAVITY_CONSTANT_SCALE;
        this.gameTimer = gameTimer;
        this.gameEventService = gameEventService;

        fallEntities = entityData.getEntities(CreatureFall.class, Position.class, CreatureComponent.class);
    }

    @Override
    public void processTick(float tpf) {
        fallEntities.applyChanges();

        for (Entity entity : fallEntities) {
            Position position = entity.get(Position.class);
            CreatureFall creatureFall = entity.get(CreatureFall.class);
            if (creatureFall.velocity != null && position.position.y > WorldUtils.FLOOR_HEIGHT) {
                position = fly(entity.getId(), position, creatureFall);
            }
            if (position.position.y > WorldUtils.FLOOR_HEIGHT) {
                continue;
            }

            // We'll just remove this and add the AI
            entityData.removeComponent(entity.getId(), CreatureFall.class);
            if (creatureFall.damage > 0) {
                EntityController.setDamage(entityData, entity.getId(), creatureFall.damage);
            }
            if (creatureFall.velocity != null) {
                gameEventService.emitEffect(FOOT_DUST_EFFECT_ID, position.position, 0);
            }

            // The landing, on touchdown (hand drops and conjured creatures alike)
            gameEventService.emit(GameEventType.CREATURE_LAND, entity.getId(), position.position);

            // Preserve any requested AI target through landing and get-up.
            // The state, depending on the landing
            CreatureComponent creatureComponent = entity.get(CreatureComponent.class);
            CreatureAi ai = entityData.getComponent(entity.getId(), CreatureAi.class);
            entityData.setComponent(entity.getId(), new CreatureAi(gameTimer.getGameTime(), !creatureFall.conjured && creatureComponent.stunDuration > 0f ? CreatureState.FALLEN : CreatureState.IDLE, creatureComponent.creatureId,
                    ai == null ? null : ai.getTargetState()));
        }
    }

    /**
     * Moves a thrown creature for a turn, the gravity pulls it down and a wall
     * in the way turns it back with half its speed (0x4B2030)
     *
     * @return where it is now
     */
    private Position fly(EntityId entityId, Position position, CreatureFall creatureFall) {
        Vector3f newPosition = position.position.clone();
        Vector3f velocity = creatureFall.velocity.clone();
        velocity.y -= gravity;
        if (isSolid(newPosition.x + velocity.x, newPosition.z)) {
            velocity.x = -velocity.x / 2;
        } else {
            newPosition.x += velocity.x;
        }
        if (isSolid(newPosition.x, newPosition.z + velocity.z)) {
            velocity.z = -velocity.z / 2;
        } else {
            newPosition.z += velocity.z;
        }
        newPosition.y = Math.max(newPosition.y + velocity.y, WorldUtils.FLOOR_HEIGHT);

        Position result = new Position(position.rotation, newPosition);
        entityData.setComponents(entityId, result, new CreatureFall(velocity, creatureFall.damage));
        return result;
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
