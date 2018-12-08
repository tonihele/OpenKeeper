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
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.utils.TextUtils;

/**
 * Parses text and fills the replacements from Entity data
 *
 * @param <T> The object type the entity basically represents
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class EntityTextParser<T> {

    protected final EntityData entityData;

    public EntityTextParser(EntityData entityData) {
        this.entityData = entityData;
    }

    public String parseText(String text, EntityId entityId, T dataObject) {
        return TextUtils.parseText(text, (index) -> {
            return getReplacement(index, entityId, dataObject);
        });
    }
    
    protected String getReplacement(int index, EntityId entityId, T dataObject) {
        switch (index) {
            case 37:
                Health health = entityData.getComponent(entityId, Health.class);
                if (health != null) {
                    return Integer.toString((int) (health.health / health.maxHealth * 100f));
                }
                return "";
            case 73:
                Gold gold = entityData.getComponent(entityId, Gold.class);
                if (gold != null) {
                    return Integer.toString(gold.gold);
                }
                return "";
        }

        return "Parameter " + index + " not implemented!";
    }

}
