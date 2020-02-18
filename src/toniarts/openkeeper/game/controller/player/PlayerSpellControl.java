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

import java.util.List;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.PlayerSpell;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.listener.PlayerSpellListener;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;

/**
 * Holds and manages player's spells. In the original game the research status
 * is persisted even if all libraries are removed in midst of a research. So we
 * hold here the current research status. Only when the research is done, a
 * spellbook is created to the world.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerSpellControl extends AbstractResearchablePlayerControl<KeeperSpell, ResearchableEntity, PlayerSpellListener> {

    public PlayerSpellControl(Keeper keeper, List<KeeperSpell> keeperSpells) {
        super(keeper, keeper.getAvailableSpells(), keeperSpells);
    }

    @Override
    protected PlayerSpell createDataType(KeeperSpell type) {
        return new PlayerSpell(type.getId());
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
        ResearchableEntity playerSpell = get(spell);
        playerSpell.setDiscovered(discovered);
        if (playerListeners != null) {
            for (PlayerSpellListener playerSpellListener : playerListeners) {
                playerSpellListener.onEntityAdded(keeper.getId(), playerSpell);
            }
        }
    }

    @Override
    public void onResearchResultsAdded(KeeperSpell keeperSpell) {
        ResearchableEntity playerSpell = get(keeperSpell);
        if (playerSpell != null) {
            if (!playerSpell.isDiscovered()) {
                playerSpell.setDiscovered(true);
            } else {
                playerSpell.setUpgraded(true);
            }

            // Notify listeners
            if (playerListeners != null) {
                for (PlayerSpellListener playerSpellListener : playerListeners) {
                    playerSpellListener.onEntityAdded(keeper.getId(), playerSpell);
                }
            }
        }
    }

    @Override
    public void onResearchResultsRemoved(KeeperSpell keeperSpell) {
        ResearchableEntity playerSpell = get(keeperSpell);
        if (playerSpell != null) {
            if (playerSpell.isUpgraded()) {
                playerSpell.setUpgraded(false);
            } else {
                playerSpell.setDiscovered(false);
            }

            // Notify listeners
            if (playerListeners != null) {
                for (PlayerSpellListener playerSpellListener : playerListeners) {
                    playerSpellListener.onEntityRemoved(keeper.getId(), playerSpell);
                }
            }
        }
    }

}
