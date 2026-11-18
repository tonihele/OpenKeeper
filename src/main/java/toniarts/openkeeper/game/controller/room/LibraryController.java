/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.controller.room;

import com.jme3.math.FastMath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.ObjectViewState.GameObjectAnimState;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.storage.RoomResearcherControl;
import toniarts.openkeeper.game.controller.room.storage.RoomSpecialControl;
import toniarts.openkeeper.game.controller.room.storage.RoomSpellBookControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;

/**
 * The library
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class LibraryController extends NormalRoomController {

    private static final short BOOK_CASE = 64;
    private static final GameObjectAnimState[] BOOK_CASE_MODELS = {
        GameObjectAnimState.ADDITIONAL_RESOURCE_1,
        GameObjectAnimState.ADDITIONAL_RESOURCE_2,
        GameObjectAnimState.ADDITIONAL_RESOURCE_3
    };

    private final IGameTimer gameTimer;

    public LibraryController(EntityId entityId, EntityData entityData, IKwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController,
            IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, ObjectType.RESEARCHER);

        this.gameTimer = gameTimer;
    }

    @Override
    protected EntityId constructFloorObject(short objectId, Point p) {
        if (objectId != BOOK_CASE) {
            return super.constructFloorObject(objectId, p);
        }

        // DK II's bookcase case in 0x4EDFC0 uses additional resources 1-3,
        // chosen from the absolute tile, and turns alternate rows half a turn.
        float rotation = (p.y & 1) * FastMath.PI;
        EntityId object = objectsController.loadObject(objectId, (short) 0, p.x, p.y, rotation);
        ObjectViewState view = entityData.getComponent(object, ObjectViewState.class);
        entityData.setComponent(object, new ObjectViewState(view.objectId, view.state,
                BOOK_CASE_MODELS[getTileRandom(p, BOOK_CASE_MODELS.length)], view.visible, view.lairCreatureTypeId));
        return object;
    }

    @Override
    public void construct() {
        super.construct();

        addObjectControl(new RoomResearcherControl(kwdFile, this, entityData, gameTimer) {

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getFurnitureCount();
            }
        });
        addObjectControl(new RoomSpellBookControl(kwdFile, this, entityData, gameTimer) {

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getFloorFurnitureCount();
            }
        });
        addObjectControl(new RoomSpecialControl(kwdFile, this, entityData, gameTimer) {

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getCoordinates().size();
            }
        });
    }
}
