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
package toniarts.openkeeper.game.controller.player;

import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.PlayerSpellListener;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;

/**
 * Holds and manages player's spells
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerSpellControl extends AbstractPlayerControl<KeeperSpell, PlayerSpell> {

    private PlayerSpell currentResearch = null;
    private List<PlayerSpellListener> playerSpellListeners;

    public PlayerSpellControl(Keeper keeper) {
        super(keeper, keeper.getAvailableSpells());
    }

    @Override
    public void setTypeAvailable(KeeperSpell type, boolean available) {
        super.setTypeAvailable(type, available);

        // Add one to the stock
        PlayerSpell playerSpell;
        if (available) {
            playerSpell = new PlayerSpell(type);
            put(type, playerSpell);
            if (currentResearch == null) {
                currentResearch = playerSpell;
            }
        } else {
            playerSpell = types.remove(type);
        }

        // Listeners
        if (playerSpellListeners != null) {
            for (PlayerSpellListener playerSpellListener : playerSpellListeners) {
                if (available) {
                    playerSpellListener.onAdded(playerSpell);
                } else {
                    playerSpellListener.onRemoved(playerSpell);
                }
            }
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
        if (playerSpellListeners != null) {
            for (PlayerSpellListener playerSpellListener : playerSpellListeners) {
                playerSpellListener.onResearchStatusChanged(playerSpell);
            }
        }
    }

    /**
     * Researches current spell
     *
     * @param researchAmount the research amount
     * @return returns the spell if it is researched, it should be bound to a
     * world object
     */
    public PlayerSpell research(int researchAmount) {
        PlayerSpell spell = currentResearch;
        boolean advanceToNext = currentResearch.research(researchAmount);
        if (playerSpellListeners != null) {
            for (PlayerSpellListener playerSpellListener : playerSpellListeners) {
                playerSpellListener.onResearchStatusChanged(spell);
            }
        }
        if (advanceToNext) {
            setNextResearchTarget();
        }

        // If discovered by the player for the first time, tie the spell to a spell book
        if (advanceToNext && spell.isDiscovered() && spell.getSpellBookObjectControl() == null) {
            return spell;
        }
        return null;
    }

    /**
     * Determines can the player research any spells
     *
     * @return is there anything to research
     */
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

    /**
     * Listen to spell status changes
     *
     * @param listener the listener
     */
    public void addPlayerSpellListener(PlayerSpellListener listener) {
        if (playerSpellListeners == null) {
            playerSpellListeners = new ArrayList<>();
        }
        playerSpellListeners.add(listener);
    }

}
