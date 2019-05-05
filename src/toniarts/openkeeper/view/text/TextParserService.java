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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    public TextParserService(IMapInformation mapInformation) {
        this.creatureTextParser = new CreatureTextParser(mapInformation);
        this.trapTextParser = new TrapTextParser();
        this.doorTextParser = new DoorTextParser();
        this.objectTextParser = new ObjectTextParser();
        this.mapTileTextParser = new MapTileTextParser();
    }

    @Override
    public String parseText(String text, Entity entity, Creature creature) {
        return creatureTextParser.parseText(text, entity, creature);
    }

    @Override
    public String parseText(String text, Entity entity, Trap trap) {
        return trapTextParser.parseText(text, entity, trap);
    }

    @Override
    public String parseText(String text, Entity entity, Door door) {
        return doorTextParser.parseText(text, entity, door);
    }

    @Override
    public String parseText(String text, Entity entity, GameObject gameObject) {
        return objectTextParser.parseText(text, entity, gameObject);
    }

    @Override
    public String parseText(String text, MapTile mapTile) {
        return mapTileTextParser.parseText(text, mapTile);
    }

    @Override
    public Collection<Class<? extends EntityComponent>> getWatchedComponents() {
        List<Class<? extends EntityComponent>> components = new ArrayList<>();

        components.addAll(creatureTextParser.getWatchedComponents());

        return components;
    }
    
}
