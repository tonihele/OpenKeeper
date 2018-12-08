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

import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.Trap;

/**
 * Provides text parsing services for users
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface TextParser {

    String parseText(String text, EntityId entityId, Creature creature);

    String parseText(String text, EntityId entityId, Trap trap);

    String parseText(String text, EntityId entityId, Door door);

    String parseText(String text, EntityId entityId, GameObject gameObject);

    String parseText(String text, MapTile mapTile);

}
