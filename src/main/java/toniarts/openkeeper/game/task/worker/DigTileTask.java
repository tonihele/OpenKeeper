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
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * Dig a tile task, for workers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DigTileTask extends AbstractTileTask {

    public DigTileTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId) {
        super(navigationService, mapController, p, playerId);
    }

    @Override
    public int getMaxAllowedNumberOfAsignees() {
        // TODO: I think it is 3 per accessible side, but we would need the map data for this
        return 3;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return getAccessibleTargetNextToLocation(creature);
    }

    @Override
    public boolean isReachable(ICreatureController creature) {
        return (getTarget(creature) != null); // To avoid multiple path finds
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return isTileToDig() || getExecutionDuration(creature) > 0;
    }

    /**
     * Whether the tile still stands to be dug. A digger that has started
     * swinging keeps the task after the tile gives way, until its swing has
     * played out.
     *
     * @return true if the tile is still there to dig
     */
    protected boolean isTileToDig() {
        IMapTileInformation tile = mapController.getMapData().getTile(getTaskLocation());
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

    /**
     * Digs in swings, as DK II's dig action does (0x496AA0, 0x496E00). Each
     * swing hits the tile once, at its start, and then plays out. The first
     * swing also plays the start of the animation. The swing that brings the
     * tile down also plays the end of the animation, and only then does the
     * digger move on. The stored execution duration is the time on the task
     * when the current swing ends, 0 before the first swing and once the
     * digger is done.
     *
     * @param creature the digger
     * @param executionDuration the time the digger has spent on the task
     */
    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        float swingEnd = getExecutionDuration(creature);
        if (executionDuration < swingEnd) {
            return;
        }
        if (!isTileToDig()) {
            setExecutionDuration(creature, 0);
            return;
        }

        creature.addGold(mapController.damageTile(getTaskLocation(), playerId, creature));
        setExecutionDuration(creature, executionDuration + getSwingDuration(creature.getCreature(), swingEnd == 0, !isTileToDig()));
    }

    /**
     * The length of one dig swing in seconds. DK II's dig action plays the
     * creature's melee animation (the imp's is called Imp_Dig), frames / fps.
     *
     * @param creature the digger
     * @param first whether this is the first swing, which plays the start of
     * the animation
     * @param last whether this swing brought the tile down and plays the end of
     * the animation
     * @return the swing length in seconds
     */
    private static float getSwingDuration(Creature creature, boolean first, boolean last) {
        ArtResource animation = creature.getAnimation(Creature.AnimationType.MELEE_ATTACK);
        Number fps = animation != null ? animation.getData(ArtResource.KEY_FPS) : null;
        if (fps == null || fps.intValue() <= 0) {
            return 1f;
        }
        int frames = animation.<Number>getData(ArtResource.KEY_FRAMES).intValue();
        if (first && animation.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) {
            frames += animation.<Number>getData(ArtResource.KEY_START_AF).intValue();
        }
        if (last && animation.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {
            frames += animation.<Number>getData(ArtResource.KEY_END_AF).intValue();
        }
        return (float) frames / fps.intValue();
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.DIG_TILE;
    }

}
