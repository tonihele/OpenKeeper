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
import java.util.function.Predicate;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.listener.PlayerResearchableEntityListener;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable.ResearchOrder;

/**
 * Manages player research
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PlayerResearchControl {

    private final Keeper keeper;
    private final IKwdFile kwdFile;
    protected List<PlayerResearchableEntityListener> researchListeners;

    public PlayerResearchControl(Keeper keeper, IKwdFile kwdFile) {
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

    private static boolean research(ResearchableEntity researchableEntity, int researchPoints, int researchAmount) {
        researchableEntity.setResearch(researchableEntity.getResearch() + researchAmount);
        if (researchableEntity.getResearch() >= researchPoints) {
            // The discovery or the upgrade is added with delay by the spellbook actually materializing
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
        boolean advanceToNext = research(researchableEntity, getResearchPoints(kwdFile, researchableEntity), researchAmount);

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

    /**
     * Get the research points it takes to discover or upgrade the given
     * entity. Like in DK II, they come from the research order, the research
     * times of the rooms, doors, traps and spells are not used
     *
     * @param kwdFile the level
     * @param researchableEntity the entity
     * @return the research points, {@link Integer#MAX_VALUE} if the entity is
     * not in the research order and so can't be researched
     */
    public static int getResearchPoints(IKwdFile kwdFile, ResearchableEntity researchableEntity) {
        for (ResearchOrder researchOrder : kwdFile.getResearchOrder()) {
            if (researchOrder.getTypeId() == researchableEntity.getId()
                    && getResearchableType(researchOrder) == researchableEntity.getResearchableType()) {
                return researchOrder.getResearchPoints();
            }
        }
        return Integer.MAX_VALUE;
    }

    private static ResearchableType getResearchableType(ResearchOrder researchOrder) {
        if (researchOrder.getType() == null) {
            return null;
        }
        return switch (researchOrder.getType()) {
            case ROOM ->
                ResearchableType.ROOM;
            case DOOR ->
                ResearchableType.DOOR;
            case TRAP ->
                ResearchableType.TRAP;
            case SPELL ->
                ResearchableType.SPELL;
        };
    }

    private void setNextResearchTarget() {

        // Like DK II, go through the research order and research everything
        // that can be discovered first, and only then upgrade the spells, in
        // the same order. What is not in the research order is not researched
        // Always check that it is not the current one, because they'll status will be updated with delay
        ResearchableEntity nextToReseach = findInResearchOrder(researchableEntity -> !isEntityDiscovered(researchableEntity));
        if (nextToReseach == null) {
            nextToReseach = findInResearchOrder(researchableEntity -> researchableEntity.isUpgradedable()
                    && isEntityDiscovered(researchableEntity) && !isEntityUpgraded(researchableEntity));
        }
        keeper.setCurrentResearch(nextToReseach);
    }

    private ResearchableEntity findInResearchOrder(Predicate<ResearchableEntity> condition) {
        for (ResearchOrder researchOrder : kwdFile.getResearchOrder()) {
            ResearchableEntity researchableEntity = getAvailableEntity(researchOrder);
            if (researchableEntity != null && condition.test(researchableEntity)) {
                return researchableEntity;
            }
        }
        return null;
    }

    private ResearchableEntity getAvailableEntity(ResearchOrder researchOrder) {
        ResearchableType researchableType = getResearchableType(researchOrder);
        if (researchableType == null) {
            return null;
        }
        List<ResearchableEntity> availableEntities = switch (researchableType) {
            case ROOM ->
                keeper.getAvailableRooms();
            case DOOR ->
                keeper.getAvailableDoors();
            case TRAP ->
                keeper.getAvailableTraps();
            case SPELL ->
                keeper.getAvailableSpells();
        };
        for (ResearchableEntity researchableEntity : availableEntities) {
            if (researchableEntity.getId() == researchOrder.getTypeId()) {
                return researchableEntity;
            }
        }
        return null;
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
