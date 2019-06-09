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

import java.awt.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.Utils;

/**
 * The hatchery
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class HatcheryController extends NormalRoomController implements IChickenGenerator {

    private double lastSpawnTime;
    private final IGameTimer gameTimer;

    public HatcheryController(KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController, IGameTimer gameTimer) {
        super(kwdFile, roomInstance, objectsController);

        this.gameTimer = gameTimer;
        lastSpawnTime = gameTimer.getGameTime();
    }

    @Override
    protected RoomObjectLayout getRoomObjectLayout() {
        return RoomObjectLayout.ISOLATED;
    }

    @Override
    public Point getEntranceCoordinate() {
        // TODO: Should be maybe random available point, where there are no coops
        return Utils.getRandomItem(roomInstance.getCoordinates());
    }

    @Override
    public double getLastSpawnTime() {
        return lastSpawnTime;
    }

    @Override
    public void onSpawn(double time) {
        this.lastSpawnTime = time;
    }

    @Override
    public void captured(short playerId) {
        super.captured(playerId);
        lastSpawnTime = gameTimer.getGameTime();
    }

    @Override
    protected int getUsedCapacity() {
        // TODO: General problem how do we tie objects to room, with component of course but how does the room know
        return super.getUsedCapacity();
    }

    @Override
    protected int getMaxCapacity() {
        return roomInstance.getCoordinates().size();
    }

}
