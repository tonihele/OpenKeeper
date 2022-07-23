/*
 * Copyright (C) 2014-2022 OpenKeeper
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

import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.utils.Utils;

/**
 * Parses texts for Keeper Spell icons
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class SpellIconTextParser extends IconTextParser<KeeperSpell, ResearchableEntity> {

    @Override
    protected String getReplacement(int index, KeeperSpell keeperSpell, ResearchableEntity spell) {
        switch (index) {
            case 1:
                return Utils.getMainTextResourceBundle().getString(Integer.toString(keeperSpell.getNameStringId()));
            case 2:
                return Integer.toString(keeperSpell.getManaCost());
            case 3:
                return spell.isUpgraded() ? "2" : "1";
        }

        return "Parameter " + index + " not implemented!";
    }

}
