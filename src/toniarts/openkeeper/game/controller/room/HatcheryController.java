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
package toniarts.openkeeper.game.controller.room;

import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * The hatchery
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class HatcheryController extends NormalRoomController {

    public HatcheryController(KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController) {
        super(kwdFile, roomInstance, objectsController);
    }

    @Override
    protected RoomObjectLayout getRoomObjectLayout() {
        return RoomObjectLayout.ISOLATED;
    }

}
