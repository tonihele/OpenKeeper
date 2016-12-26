/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.player;

import com.jme3.app.Application;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;

/**
 * Holds and manages player's spells
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerSpellControl extends AbstractPlayerControl<KeeperSpell, PlayerSpell> {

    private PlayerSpell currentResearch = null;

    public PlayerSpellControl(Application application) {
        super(application);
    }

    @Override
    public void setTypeAvailable(KeeperSpell type, boolean available) {
        super.setTypeAvailable(type, available);

        // Add one to the stock
        if (available) {
            PlayerSpell playerSpell = new PlayerSpell(type);
            put(type, playerSpell);
            if (currentResearch == null) {
                currentResearch = playerSpell;
            }
        } else {
            types.remove(type);
        }
    }

    @Override
    public int getTypeCount() {
        return 0; // We don't really have anything to count
    }

    /**
     * Set spell as discovered
     *
     * @param spell the spell
     * @param discovered discovery state
     */
    public void setSpellDiscovered(KeeperSpell spell, boolean discovered) {
        PlayerSpell playerSpell = get(spell);
        playerSpell.setDiscovered(discovered);
        if (discovered && playerSpell == currentResearch) {
            setNextResearchTarget();
        }
    }

    public void research(int researchAmount) {
        if (currentResearch.research(researchAmount)) {
            setNextResearchTarget();
        }
    }

    public boolean isAnythingToReaseach() {
        return currentResearch != null;
    }

    private void setNextResearchTarget() {
        PlayerSpell nextToUpgrade = null;
        PlayerSpell nextToReseach = null;
        for (PlayerSpell spell : types.values()) {
            if (!spell.isDiscovered() && nextToReseach == null) {
                nextToReseach = spell;
            } else if (spell.isDiscovered() && !spell.isUpgraded() && nextToUpgrade == null) {
                nextToUpgrade = spell;
            }

            // Always prioritize discovering new over upgrading
            if (nextToReseach != null) {
                currentResearch = nextToReseach;
                return;
            }
        }

        // See if anything to upgrade
        currentResearch = nextToUpgrade;
    }

}
