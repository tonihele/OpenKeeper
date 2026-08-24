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
import toniarts.openkeeper.utils.TextParameter;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.Utils;

/**
 * Parses text where the entity is based on a Trap data object
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TrapTextParser extends EntityTextParser<Trap> {

    public TrapTextParser() {
        super();
    }

    @Override
    protected String getReplacement(TextParameter parameter, Entity entity, Trap trap) {
        switch (parameter) {
            case ENTITY_NAME:
                return Utils.getMainTextResourceBundle().getString(Integer.toString(trap.getNameStringId()));
            case MANA_USAGE:
                return Integer.toString(trap.getManaUsage());
            case MANA_COST_TO_FIRE:
                return Integer.toString(trap.getManaCostToFire());
            case STATE:
                return trap.getFlags().contains(Trap.TrapFlag.REVEAL_WHEN_FIRED) ? Utils.getMainTextResourceBundle().getString("2514") : Utils.getMainTextResourceBundle().getString("2513"); // This is not entirely true if you compare to original, see Fear trap
            default:
                return super.getReplacement(parameter, entity, trap);
        }
    }


}
