/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.game.controller.player;

import java.util.List;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.listener.PlayerDoorListener;
import toniarts.openkeeper.tools.convert.map.Door;

/**
 * Holds a list of player doors and functionality related to them
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerDoorControl extends AbstractResearchablePlayerControl<Door, ResearchableEntity, PlayerDoorListener> /*implements RoomListener*/ {

    private int doorCount = 0;

    public PlayerDoorControl(Keeper keeper, List<Door> doors) {
        super(keeper, keeper.getAvailableDoors(), doors);
    }

    @Override
    protected ResearchableEntity createDataType(Door type) {
        return new ResearchableEntity(type.getDoorId(), ResearchableType.DOOR);
    }

    /**
     * Get player door count
     *
     * @return the door count
     */
    @Override
    public int getTypeCount() {
        return doorCount;
    }

}
