/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.view.text;

import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;

/**
 * A kind of facade to the different kind of parsers. Many of them share the
 * same parameters
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TextParserService implements TextParser {

    private final CreatureTextParser creatureTextParser;
    private final TrapTextParser trapTextParser;
    private final DoorTextParser doorTextParser;
    private final ObjectTextParser objectTextParser;
    private final MapTileTextParser mapTileTextParser;
    private final RoomTextParser roomTextParser;

    public TextParserService(IMapInformation mapInformation, IRoomsInformation roomsInformation) {
        this.creatureTextParser = new CreatureTextParser(mapInformation);
        this.trapTextParser = new TrapTextParser();
        this.doorTextParser = new DoorTextParser();
        this.objectTextParser = new ObjectTextParser();
        this.mapTileTextParser = new MapTileTextParser();
        this.roomTextParser = new RoomTextParser(roomsInformation);
    }

    @Override
    public CreatureTextParser getCreatureTextParser() {
        return creatureTextParser;
    }

    @Override
    public TrapTextParser getTrapTextParser() {
        return trapTextParser;
    }

    @Override
    public DoorTextParser getDoorTextParser() {
        return doorTextParser;
    }

    @Override
    public ObjectTextParser getObjectTextParser() {
        return objectTextParser;
    }

    @Override
    public MapTileTextParser getMapTileTextParser() {
        return mapTileTextParser;
    }

    @Override
    public RoomTextParser getRoomTextParser() {
        return roomTextParser;
    }
}
