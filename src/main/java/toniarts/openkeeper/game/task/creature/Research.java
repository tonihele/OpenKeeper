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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.player.PlayerResearchControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Research researchable entities for the keeper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class Research extends AbstractCapacityCriticalRoomTask {

    private final PlayerResearchControl researchControl;
    private final IObjectsController objectsController;
    private final int researchRounds = 7 + Utils.getRandom().nextInt(7);
    private Vector2f workPosition;

    public Research(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room,
            TaskManager taskManager, PlayerResearchControl researchControl, IObjectsController objectsController) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.researchControl = researchControl;
        this.objectsController = objectsController;
    }

    @Override
    public boolean isValid(ICreatureController creature) {

        // A researcher that shares the Library with the other side moves to another one (0x475E70, 0x476060)
        return super.isValid(creature) && researchControl.isAnythingToReaseach()
                && (creature == null || !taskManager.isLibraryWithoutHatedCompanyInReach(creature, getRoom()));
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {

        // The tile the researcher stands on
        return WorldUtils.pointToVector2f(WorldUtils.vectorToPoint(getWorkPosition(creature)));
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

    @Override
    public Vector2f getWorkPosition(ICreatureController creature) {

        // DK II puts the researcher in front of a bookcase, which faces north on an even row and south on an odd one
        // (0x4EDFC0). The third kind of bookcase is read from behind half the time. At a book on the wall the
        // researcher stands a quarter of a tile from the middle of the tile towards the wall (0x475A20). Picked
        // once, the position is asked every tick
        if (workPosition == null) {
            IObjectController furniture = getFurniture(objectsController);
            if (furniture == null) {
                workPosition = WorldUtils.pointToVector2f(getTaskLocation());
            } else if (isOnWall(furniture)) {
                workPosition = getPositionAt(WorldUtils.pointToVector3f(getTaskLocation()), getAngleIntoRoom(furniture) + FastMath.PI, 0.25f);
            } else {
                float angle = (getTaskLocation().y & 1) * FastMath.PI;
                if (AbstractRoomController.getTileRandom(getTaskLocation(), 3) == 2 && Utils.getRandom().nextBoolean()) {
                    angle += FastMath.PI;
                }
                workPosition = getPositionAt(furniture.getPosition(), angle, getDistanceToFurniture(furniture, creature));
            }
        }
        return workPosition;
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.SPELL_BOOK;
    }

    @Override
    public boolean moveToOtherSpot(ICreatureController creature, float executionDuration) {

        // DK II researches for 7 to 13 rounds of the research animation at a
        // bookcase, then goes to the closest bookcase nobody is at if there is
        // one. Without the animation the creature stays where it is
        double roundTime = CreatureController.getAnimationTime(creature.getCreature(), Creature.AnimationType.RESEARCHING);
        return roundTime > 0 && executionDuration >= researchRounds * roundTime
                && taskManager.assignFreeRoomTask(creature, getRoom(), ObjectType.RESEARCHER);
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // Advance players spell research, DK II adds the creature's research
        // points per tick on every game tick
        ResearchableEntity researchableEntity = researchControl.research(creature.getResearchPerTick());
        if (researchableEntity != null) {

            // Wait if the room filled up meanwhile, the research stays complete
            if (getRoom().getObjectControl(getRoomObjectType()).isFullCapacity()) {
                return;
            }

            // Create a spell book
            EntityId entityId = objectsController.addRoomSpellBook((short) 0, getTaskLocation().x, getTaskLocation().y, researchableEntity);
            entityId = (EntityId) getRoomObjectControl().addItem(entityId, null);
            if (entityId != null) {

                // No room, the research stays complete and the book is tried again later
                objectsController.createController(entityId).remove();
            } else {
                researchControl.completeResearch();
            }
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.RESEARCH;
    }

}
