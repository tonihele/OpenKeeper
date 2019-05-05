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

    public EntityTextParser() {
    }

    public String parseText(String text, Entity entity, T dataObject) {
        return TextUtils.parseText(text, (index) -> {
            return getReplacement(index, entity, dataObject);
        });
    }
    
    protected String getReplacement(int index, Entity entity, T dataObject) {
        switch (index) {
            case 37:
                Health health = entity.get(Health.class);
                if (health != null) {
                    return Integer.toString((int) (health.health / health.maxHealth * 100f));
                }
                return "";
            case 73:
                Gold gold = entity.get(Gold.class);
                if (gold != null) {
                    return Integer.toString(gold.gold);
                }
                return "";
        }

        return "Parameter " + index + " not implemented!";
    }

    protected Collection<Class<? extends EntityComponent>> getWatchedComponents() {
        List<Class<? extends EntityComponent>> components = new ArrayList<>();
        components.add(Health.class);
        components.add(Gold.class);
        return components;
    }

}
