/*
 * Copyright (C) 2014-2019 OpenKeeper
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
import toniarts.openkeeper.game.data.PlayerSpell;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.listener.PlayerResearchableEntityListener;
import toniarts.openkeeper.game.listener.PlayerSpellListener;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Manages player research
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerResearchControl {

    private final Keeper keeper;
    private final KwdFile kwdFile;
    protected List<PlayerResearchableEntityListener> researchListeners;
    protected List<PlayerSpellListener> spellResearchListeners;

    public PlayerResearchControl(Keeper keeper, KwdFile kwdFile) {
        this.keeper = keeper;
        this.kwdFile = kwdFile;
    }

    /**
     * Initializes the research control for the player, call after the player is
     * all set up
     */
    public void initialize() {
        setNextResearchTarget();
    }

    /**
     * Determines can the player research anything
     *
     * @return is there anything to research
     */
    public boolean isAnythingToReaseach() {
        return keeper.getCurrentResearch() != null;
    }

    private static boolean researchSpell(PlayerSpell spell, KeeperSpell keeperSpell, int researchAmount) {
        spell.setResearch(spell.getResearch() + researchAmount);
        if (spell.isDiscovered() && spell.getResearch() >= keeperSpell.getBonusRTime()) {
            spell.setUpgraded(true);
            spell.setResearch(0);
            return true;
        } else if (!spell.isDiscovered() && spell.getResearch() >= keeperSpell.getResearchTime()) {
            spell.setDiscovered(true);
            spell.setResearch(0);
            return true;
        }
        return false;
    }

    private static boolean research(ResearchableEntity researchableEntity, int researchTime, int researchAmount) {
        researchableEntity.setResearch(researchableEntity.getResearch() + researchAmount);
        if (researchableEntity.getResearch() >= researchTime) {
            researchableEntity.setDiscovered(true);
            researchableEntity.setResearch(0);
            return true;
        }
        return false;
    }

    /**
     * Researches current item
     *
     * @param researchAmount the research amount
     * @return returns the type if it is researched completely
     */
    public ResearchableEntity research(int researchAmount) {
        ResearchableEntity researchableEntity = keeper.getCurrentResearch();
        boolean advanceToNext = false;
        switch (researchableEntity.getResearchableType()) {
            case DOOR: {
                advanceToNext = research(researchableEntity, kwdFile.getDoorById(researchableEntity.getId()).getResearchTime(), researchAmount);
                break;
            }
            case TRAP: {
                advanceToNext = research(researchableEntity, kwdFile.getTrapById(researchableEntity.getId()).getResearchTime(), researchAmount);
                break;
            }
            case ROOM: {
                advanceToNext = research(researchableEntity, kwdFile.getRoomById(researchableEntity.getId()).getResearchTime(), researchAmount);
                break;
            }
            case SPELL: {
                advanceToNext = researchSpell((PlayerSpell) researchableEntity, kwdFile.getKeeperSpellById(researchableEntity.getId()), researchAmount);
                break;
            }
        }

        if (!advanceToNext) {
            switch (researchableEntity.getResearchableType()) {
                case DOOR:
                case TRAP:
                case ROOM: {
                    if (researchListeners != null) {
                        for (PlayerResearchableEntityListener researchableEntityListener : researchListeners) {
                            researchableEntityListener.onResearchStatusChanged(keeper.getId(), researchableEntity);
                        }
                    }
                    break;
                }
                case SPELL: {
                    if (spellResearchListeners != null) {
                        PlayerSpell playerSpell = (PlayerSpell) researchableEntity;
                        for (PlayerSpellListener playerSpellListener : spellResearchListeners) {
                            playerSpellListener.onPlayerSpellResearchStatusChanged(keeper.getId(), playerSpell);
                        }
                    }
                    break;
                }
            }
        }

        // If research complete, return the entity
        if (advanceToNext) {
            setNextResearchTarget();
            return researchableEntity;
        }
        return null;
    }

    private void setNextResearchTarget() {

        /**
         * Here is the order in which we research:
         *
         * Spells discover no upgrade<br>
         * Doors<br>
         * Traps<br>
         * Rooms<br>
         * Spells upgrade
         */
        ResearchableEntity nextToUpgrade = null;
        ResearchableEntity nextToReseach = null;

        // Spells
        for (PlayerSpell spell : keeper.getAvailableSpells()) {
            if (!spell.isDiscovered() && nextToReseach == null) {
                nextToReseach = spell;
            } else if (spell.isDiscovered() && !spell.isUpgraded() && nextToUpgrade == null) {
                nextToUpgrade = spell;
            }

            // Always prioritize discovering new over upgrading
            if (nextToReseach != null) {
                keeper.setCurrentResearch(nextToReseach);
                return;
            }
        }

        // All the rest
        List<ResearchableEntity> researchables = new ArrayList<>(keeper.getAvailableDoors().size() + keeper.getAvailableTraps().size() + keeper.getAvailableRooms().size());
        researchables.addAll(keeper.getAvailableDoors());
        researchables.addAll(keeper.getAvailableTraps());
        researchables.addAll(keeper.getAvailableRooms());
        for (ResearchableEntity researchableEntity : researchables) {
            if (!researchableEntity.isDiscovered() && nextToReseach == null) {
                nextToReseach = researchableEntity;
            }

            // See if we found a match
            if (nextToReseach != null) {
                keeper.setCurrentResearch(nextToReseach);
                return;
            }
        }

        // See if anything to upgrade
        keeper.setCurrentResearch(nextToUpgrade);
    }

    /**
     * Listen to type research changes
     *
     * @param listener the listener
     */
    public void addListener(PlayerResearchableEntityListener listener) {
        if (researchListeners == null) {
            researchListeners = new ArrayList<>();
        }
        researchListeners.add(listener);
    }

    /**
     * No longer listen to research changes
     *
     * @param listener the listener
     */
    public void removeListener(PlayerResearchableEntityListener listener) {
        if (researchListeners != null) {
            researchListeners.remove(listener);
        }
    }

    /**
     * Listen to type research changes
     *
     * @param listener the listener
     */
    public void addListener(PlayerSpellListener listener) {
        if (spellResearchListeners == null) {
            spellResearchListeners = new ArrayList<>();
        }
        spellResearchListeners.add(listener);
    }

    /**
     * No longer listen to research changes
     *
     * @param listener the listener
     */
    public void removeListener(PlayerSpellListener listener) {
        if (spellResearchListeners != null) {
            spellResearchListeners.remove(listener);
        }
    }

}
