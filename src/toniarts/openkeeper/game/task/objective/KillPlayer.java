/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.task.objective;

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.Vector2f;
import java.util.Iterator;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Kill player objective for those goodly heroes. Can create a complex set of
 * tasks
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KillPlayer extends AbstractObjectiveTask {

    protected final short targetPlayerId;
    protected final CreatureControl creature;

    public KillPlayer(WorldState worldState, short targetPlayerId, CreatureControl creature) {
        super(worldState, 0, 0 /*worldState.getGameState().getPlayer(targetPlayerId).getRoomControl().getDungeonHeart().getRoomInstance().getCoordinates().get(0).x, worldState.getGameState().getPlayer(targetPlayerId).getRoomControl().getDungeonHeart().getRoomInstance().getCoordinates().get(0).y*/, creature.getOwnerId());

        this.targetPlayerId = targetPlayerId;
        this.creature = creature;
        createSubTasks();
    }

    @Override
    public boolean isValid(CreatureControl creature) {
        if (!isPlayerDestroyed() && creature != null) {

            // Check that the objectives are still the same
            return Thing.HeroParty.Objective.KILL_PLAYER.equals(creature.getObjective()) && Short.valueOf(targetPlayerId).equals(creature.getObjectiveTargetPlayerId());
        }
        return !isPlayerDestroyed();
    }

    @Override
    public Vector2f getTarget(CreatureControl creature) {
        return WorldUtils.pointToVector2f(getTaskLocation()); // FIXME 0.5f not needed?
    }

    @Override
    protected String getStringId() {
        return "2645";
    }

    @Override
    public void executeTask(CreatureControl creature) {

    }

    @Override
    public ArtResource getTaskAnimation(CreatureControl creature) {
        return null;
    }

    @Override
    public String getTaskIcon() {
        return null;
    }

    private boolean isPlayerDestroyed() {
        return worldState.getGameState().getPlayer(targetPlayerId).isDestroyed();
    }

    private void createSubTasks() {

        // See if we can navigate there
        GraphPath<TileData> outPath = worldState.findPath(creature.getCreatureCoordinates(), getTaskLocation(), creature.getParty() != null ? creature.getParty() : creature);
        if (outPath != null) {
            Iterator<TileData> iter = outPath.iterator();
            TileData lastPoint = null;
            boolean first = true;
            int i = 0;
            while (iter.hasNext()) {
                TileData tile = iter.next();
                if (!worldState.isAccessible(tile, creature)) {

                    // Add task to last accessible point
                    if (i != 1 && first && lastPoint != null) {
                        addSubTask(new ObjectiveTaskDecorator(new GoToTask(worldState, lastPoint.getX(), lastPoint.getY(), playerId)));
                        first = false;
                    }

                    // See if we should dig
                    if (tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.SOLID) && (creature.isWorker() || (creature.getParty() != null && creature.getParty().isWorkersAvailable())) && (tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.DWARF_CAN_DIG_THROUGH) || tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.ATTACKABLE))) {
                        addSubTask(new ObjectiveTaskDecorator(new ObjectiveDigTileTask(worldState, tile.getX(), tile.getY(), playerId)) {

                            @Override
                            public boolean isWorkerPartyTask() {
                                return true;
                            }

                        });
                        addSubTask(new ObjectiveTaskDecorator(new GoToTask(worldState, tile.getX(), tile.getY(), playerId)));
                    } else {

                        // Hmm, this is it, should we have like attack target type tasks? Or let the AI just figure out itself
                        return;
                    }
                } else {
                    first = true;
                }
                lastPoint = tile;
                i++;
            }
        }
    }

}
