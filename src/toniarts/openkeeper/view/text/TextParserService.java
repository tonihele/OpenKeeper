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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.Trap;

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

    public TextParserService(EntityData entityData, IMapInformation mapInformation) {
        this.creatureTextParser = new CreatureTextParser(entityData, mapInformation);
        this.trapTextParser = new TrapTextParser(entityData);
        this.doorTextParser = new DoorTextParser(entityData);
        this.objectTextParser = new ObjectTextParser(entityData);
        this.mapTileTextParser = new MapTileTextParser();
    }

    @Override
    public String parseText(String text, EntityId entityId, Creature creature) {
        return creatureTextParser.parseText(text, entityId, creature);
    }

    @Override
    public String parseText(String text, EntityId entityId, Trap trap) {
        return trapTextParser.parseText(text, entityId, trap);
    }

    @Override
    public String parseText(String text, EntityId entityId, Door door) {
        return doorTextParser.parseText(text, entityId, door);
    }

    @Override
    public String parseText(String text, EntityId entityId, GameObject gameObject) {
        return objectTextParser.parseText(text, entityId, gameObject);
    }

    @Override
    public String parseText(String text, MapTile mapTile) {
        return mapTileTextParser.parseText(text, mapTile);
    }
    
}
