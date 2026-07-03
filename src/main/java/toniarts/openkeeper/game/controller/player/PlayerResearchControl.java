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
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.listener.PlayerResearchableEntityListener;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Manages player research
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PlayerResearchControl {

    private final Keeper keeper;
    private final KwdFile kwdFile;
    protected List<PlayerResearchableEntityListener> researchListeners;

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

    private static boolean research(ResearchableEntity researchableEntity, int researchTime, Integer upgradeResearchTime, int researchAmount) {
        researchableEntity.setResearch(researchableEntity.getResearch() + researchAmount);
        if (researchableEntity.isDiscovered() && researchableEntity.isUpgradedable() && researchableEntity.getResearch() >= upgradeResearchTime) {
            //spell.setUpgraded(true); - added with delay by the spellbook actually materializing
            researchableEntity.setResearch(0);
            return true;
        } else if (!researchableEntity.isDiscovered() && researchableEntity.getResearch() >= researchTime) {
            //spell.setDiscovered(true); - added with delay by the spellbook actually materializing
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
                advanceToNext = research(researchableEntity, kwdFile.getDoorById(researchableEntity.getId()).getResearchTime(), null, researchAmount);
                break;
            }
            case TRAP: {
                advanceToNext = research(researchableEntity, kwdFile.getTrapById(researchableEntity.getId()).getResearchTime(), null, researchAmount);
                break;
            }
            case ROOM: {
                advanceToNext = research(researchableEntity, kwdFile.getRoomById(researchableEntity.getId()).getResearchTime(), null, researchAmount);
                break;
            }
            case SPELL: {
                KeeperSpell keeperSpell = kwdFile.getKeeperSpellById(researchableEntity.getId());
                advanceToNext = research(researchableEntity, keeperSpell.getResearchTime(), keeperSpell.getBonusRTime(), researchAmount);
                break;
            }
        }

        if (!advanceToNext) {
            if (researchListeners != null) {
                for (PlayerResearchableEntityListener researchableEntityListener : researchListeners) {
                    researchableEntityListener.onResearchStatusChanged(keeper.getId(), researchableEntity);
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

        // Process all in order
        // Always check that it is not the current one, because they'll status will be updated with delay
        List<ResearchableEntity> researchables = new ArrayList<>(keeper.getAvailableSpells().size() + keeper.getAvailableDoors().size() + keeper.getAvailableTraps().size() + keeper.getAvailableRooms().size());
        researchables.addAll(keeper.getAvailableSpells());
        researchables.addAll(keeper.getAvailableDoors());
        researchables.addAll(keeper.getAvailableTraps());
        researchables.addAll(keeper.getAvailableRooms());
        for (ResearchableEntity researchableEntity : researchables) {
            if (!isEntityDiscovered(researchableEntity) && nextToReseach == null) {
                nextToReseach = researchableEntity;
            } else if (researchableEntity.isUpgradedable() && isEntityDiscovered(researchableEntity) && !isEntityUpgraded(researchableEntity) && nextToUpgrade == null) {
                nextToUpgrade = researchableEntity;
            }

            // Always prioritize discovering new over upgrading
            // See if we found a match
            if (nextToReseach != null) {
                keeper.setCurrentResearch(nextToReseach);
                return;
            }
        }

        // See if anything to upgrade
        keeper.setCurrentResearch(nextToUpgrade);
    }

    private boolean isEntityDiscovered(ResearchableEntity researchableEntity) {

        // Basically it is discovered if it... well, is discovered, or it is the one we are currently researching (the status will update with delay)
        return researchableEntity.isDiscovered() || researchableEntity.equals(keeper.getCurrentResearch());
    }

    private boolean isEntityUpgraded(ResearchableEntity researchableEntity) {

        // Basically it is upgraded if it... well, is upgraded, or it is the one we are currently researching AND marked as discovered already (the status will update with delay)
        return researchableEntity.isUpgraded() || (researchableEntity.isDiscovered() && researchableEntity.equals(keeper.getCurrentResearch()));
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

}
