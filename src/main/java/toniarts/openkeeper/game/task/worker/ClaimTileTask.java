/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.task.worker;

import toniarts.openkeeper.game.event.GameEventType;
import com.jme3.math.Vector2f;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.TileWork;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Claim a tile task, for workers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ClaimTileTask extends AbstractTileTask {

    public ClaimTileTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId) {
        super(navigationService, mapController, p, playerId);
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation()); // FIXME 0.5f not needed?
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return mapController.isClaimableTile(getTaskLocation(), playerId) || getExecutionDuration(creature) < 0;
    }

    @Override
    public String toString() {
        return "Claim tile at " + getTaskLocation();
    }

    /**
     * Claims the tile. Ground nobody owns is claimed the way DK II's claim
     * action does it (0x496AA0, 0x4974C0): from the tick after the worker
     * starts, every tick adds a tick's worth of the claim rate until the tile
     * turns, and then the worker plays its claim animation to the end of the
     * loop before it moves on. While it does, the stored execution duration
     * is the time on the task when the animation ends, negated to keep it
     * apart from the once a second steps of the other claims.
     *
     * @param creature the worker
     * @param executionDuration the time the worker has spent on the task
     */
    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        float animationEnd = -getExecutionDuration(creature);
        if (animationEnd > 0) {
            if (executionDuration >= animationEnd) {
                setExecutionDuration(creature, 0);
            }
            return;
        }
        if (mapController.isClaimableTile(getTaskLocation(), playerId) && isGround()) {
            if (executionDuration > 0) {
                mapController.applyClaimTile(getTaskLocation(), playerId, creature);
                if (!isValid(creature)) {
                    creature.fireEvent(GameEventType.CREATURE_CLAIM_TILE, getTaskLocation());
                    animationEnd = getAnimationEnd(creature.getCreature(), executionDuration);
                    if (animationEnd > executionDuration) {
                        setExecutionDuration(creature, -animationEnd);
                    }
                }
            }
            return;
        }

        // TODO: is this a general case or even smart to do this like this...?
        if (executionDuration - getExecutionDuration(creature) >= 1.0f) {
            setExecutionDuration(creature, executionDuration - getExecutionDuration(creature));

            mapController.applyClaimTile(getTaskLocation(), playerId, creature);

            // The work is done, let it be heard
            if (!isValid(creature)) {
                creature.fireEvent(GameEventType.CREATURE_CLAIM_TILE, getTaskLocation());
            }
        }
    }

    private boolean isGround() {
        Terrain terrain = mapController.getTerrain(mapController.getMapData().getTile(getTaskLocation()));
        return !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && TileWork.convertsAtFullHealth(terrain);
    }

    /**
     * When the loop of the claim animation that is playing ends. DK II's claim
     * action plays the worker's eating animation (the imp's is called
     * Imp_ClaimFloor) over and over from the start of the work.
     *
     * @param creature the worker
     * @param executionDuration the time the worker has spent on the task
     * @return the time on the task when the loop ends
     */
    private static float getAnimationEnd(Creature creature, float executionDuration) {
        ArtResource animation = creature.getAnimation(Creature.AnimationType.EATING);
        Number fps = animation != null ? animation.getData(ArtResource.KEY_FPS) : null;
        if (fps == null || fps.intValue() <= 0) {
            return executionDuration;
        }
        float loop = animation.<Number>getData(ArtResource.KEY_FRAMES).floatValue() / fps.intValue();
        return (float) Math.ceil(executionDuration / loop) * loop;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CLAIM_TILE;
    }

}
