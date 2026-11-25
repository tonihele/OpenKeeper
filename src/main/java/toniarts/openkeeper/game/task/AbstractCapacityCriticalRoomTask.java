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
package toniarts.openkeeper.game.task;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A base class for tasks that we should keep track off
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractCapacityCriticalRoomTask extends AbstractRoomTask {

    protected final TaskManager taskManager;
    private Vector2f placeNextToFurniture;
    private IObjectController furniture;

    public AbstractCapacityCriticalRoomTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room, TaskManager taskManager) {
        super(navigationService, mapController, p, playerId, room);

        this.taskManager = taskManager;
    }

    /**
     * Counts a trainee and the like in the room's used capacity once it is
     * in the room, nothing is kept for it on the way. Like DK II does when
     * the creature enters the room (0x4E5A70), a room that has filled up by
     * then turns it away, as the task is no longer valid for it
     *
     * @param creature the creature doing the task
     * @param executionDuration the non-stop execution duration of the creature
     */
    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        if (getRoomObjectType().isOccupiedByCreatures()) {
            getRoomObjectControl().addItem(creature.getEntityId(), null);
        }
    }

    @Override
    public void unassign(ICreatureController creature) {
        super.unassign(creature);

        if (getRoomObjectType().isOccupiedByCreatures()) {
            getRoomObjectControl().removeItem(creature.getEntityId());
        }

        // Remove us
        taskManager.removeRoomTask(this);
    }

    /**
     * The middle of a free tile next to the furniture the task is at, the
     * furniture itself stands in the middle of its tile. Looked up once, the
     * target is asked every tick and the closest free tile changes as the
     * creature walks
     *
     * @param creature the creature doing the task
     * @return the tile next to the furniture, {@code null} if there is none
     */
    protected Vector2f getTargetNextToFurniture(ICreatureController creature) {
        if (placeNextToFurniture == null) {
            placeNextToFurniture = getAccessibleTargetNextToLocation(creature);
        }
        return placeNextToFurniture;
    }

    /**
     * The furniture on the task's tile, looked up once
     *
     * @param objectsController the objects controller
     * @return the furniture, {@code null} if the tile has none
     */
    protected IObjectController getFurniture(IObjectsController objectsController) {
        if (furniture == null) {
            List<EntityId> pieces = new ArrayList<>(getRoom().getFloorFurniture());
            pieces.addAll(getRoom().getWallFurniture());
            for (EntityId entityId : pieces) {
                IObjectController piece = objectsController.createController(entityId);
                if (getTaskLocation().equals(WorldUtils.vectorToPoint(piece.getPosition()))) {
                    furniture = piece;
                    break;
                }
            }
        }
        return furniture;
    }

    /**
     * Is the furniture against a wall of the room
     *
     * @param furniture the furniture
     * @return true for wall furniture
     */
    protected boolean isOnWall(IObjectController furniture) {
        return getRoom().getWallFurniture().contains(furniture.getEntityId());
    }

    /**
     * The angle from a piece of wall furniture into the room, from the piece
     * to the middle of its tile
     *
     * @param furniture the wall furniture
     * @return the angle, measured like DK II's
     * {@link #getPositionAt(Vector3f, float, float)}
     */
    protected float getAngleIntoRoom(IObjectController furniture) {
        Vector3f position = furniture.getPosition();
        return FastMath.atan2(getTaskLocation().x - position.x, position.z - getTaskLocation().y);
    }

    /**
     * At a piece of wall furniture the creature stands about the middle of the
     * tile, so it turns to the wall behind the piece
     */
    @Override
    public Point getFaceLocation() {
        if (furniture == null || !isOnWall(furniture)) {
            return getTaskLocation();
        }
        Vector3f position = furniture.getPosition();
        return new Point(getTaskLocation().x + (int) Math.signum(position.x - getTaskLocation().x),
                getTaskLocation().y + (int) Math.signum(position.z - getTaskLocation().y));
    }

    /**
     * How far DK II puts a creature from the middle of the furniture it uses:
     * half the furniture's width plus half the creature's width, so that the
     * two just touch (object +0xF8, creature +0x63A)
     *
     * @param furniture the furniture
     * @param creature the creature
     * @return the distance in tiles
     */
    protected static float getDistanceToFurniture(IObjectController furniture, ICreatureController creature) {
        return (furniture.getObject().getWidth() + creature.getCreature().getAttributes().getWidth()) / 2;
    }

    /**
     * A spot at a distance and an angle from a point. Like DK II's sine and
     * cosine tables (0x6C1488, 0x6C1C88) the angle 0 points north, a quarter
     * turn east
     *
     * @param from the point
     * @param angle the angle in radians
     * @param distance the distance in tiles
     * @return the spot
     */
    protected static Vector2f getPositionAt(Vector3f from, float angle, float distance) {
        return new Vector2f(from.x + FastMath.sin(angle) * distance, from.z - FastMath.cos(angle) * distance);
    }

}
