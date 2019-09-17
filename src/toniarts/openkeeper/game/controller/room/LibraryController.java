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

import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.storage.RoomResearcherControl;
import toniarts.openkeeper.game.controller.room.storage.RoomSpellBookControl;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * The library
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LibraryController extends NormalRoomController {

    public LibraryController(KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController,
            IGameTimer gameTimer) {
        super(kwdFile, roomInstance, objectsController);

        addObjectControl(new RoomResearcherControl(kwdFile, this, objectsController, gameTimer) {

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getFurnitureCount();
            }
        });
        addObjectControl(new RoomSpellBookControl(kwdFile, this, objectsController, gameTimer) {

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getFloorFurnitureCount();
            }
        });
    }

}
