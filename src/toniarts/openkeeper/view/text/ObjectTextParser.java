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
import toniarts.openkeeper.tools.convert.map.GameObject;

/**
 * Parses text where the entity is based on a Object data object
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectTextParser extends EntityTextParser<GameObject> {

    public ObjectTextParser(EntityData entityData) {
        super(entityData);
    }

    @Override
    protected String getReplacement(int index, EntityId entityId, GameObject gameObject) {
        return super.getReplacement(index, entityId, gameObject);
    }


}
