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

import com.jme3.math.Vector2f;
import java.awt.Point;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.ai.ICreatureController;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Dig a tile task, for workers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DigTileTask extends AbstractTileTask {

    public DigTileTask(final IGameWorldController gameWorldController, final IMapController mapController, int x, int y, short playerId) {
        super(gameWorldController, mapController, x, y, playerId);
    }

    @Override
    public int getMaxAllowedNumberOfAsignees() {
        // TODO: I think it is 3 per accessible side, but we would need the map data for this
        return 3;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {

        // Find an accessible target
        // TODO: entity's location?
        for (Point taskPerformLocation : WorldUtils.getSurroundingTiles(mapController.getMapData(), getTaskLocation(), false)) {
            for (Point p : WorldUtils.getSurroundingTiles(mapController.getMapData(), getTaskLocation(), false)) {
                if (gameWorldController.isAccessible(mapController.getMapData().getTile(p), mapController.getMapData().getTile(taskPerformLocation), creature)) {

                    // TODO: intelligent coordinates?
                    Vector2f target = new Vector2f(p.x, p.y);
                    if (isReachable(creature, target)) {
                        return target;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public boolean isReachable(ICreatureController creature) {
        return (getTarget(creature) != null); // To avoid multiple path finds
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        MapTile tile = mapController.getMapData().getTile(getTaskLocation());
        return tile.isSelected(playerId);
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public String toString() {
        return "Dig tile at " + getTaskLocation();
    }

    @Override
    protected String getStringId() {
        MapTile tile = mapController.getMapData().getTile(getTaskLocation());
        return (tile.getGold() > 0 ? "2605" : "2600");
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // TODO: is this a general case or even smart to do this like this...?
        if (executionDuration - getExecutionDuration(creature) >= 1.0f) {
            setExecutionDuration(creature, executionDuration - getExecutionDuration(creature));

            creature.addGold(mapController.damageTile(getTaskLocation(), playerId, creature));
        }
    }

    @Override
    public ArtResource getTaskAnimation(ICreatureController creature) {
        return null;
        // return creature.getCreature().getAnimMelee1Resource();
    }

    @Override
    public String getTaskIcon() {
        return "Textures/GUI/moods/SJ-Dig.png";
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

}
