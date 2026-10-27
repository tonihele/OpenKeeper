/*
 * Copyright (C) 2014-2015 OpenKeeper
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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.KeeperComponent;
import toniarts.openkeeper.game.data.Keeper;

/**
 * The player's gold, kept in the {@link Gold} component on the player's entity
 *
 * @author ArchDemon
 */
public final class PlayerGoldControl {

    private final Keeper keeper;
    private final EntityData entityData;

    public PlayerGoldControl(Keeper keeper, EntityData entityData) {
        this.keeper = keeper;
        this.entityData = entityData;
    }

    public void addGold(int value) {
        if (value < 0) {
            return;
        }

        Gold gold = getGoldComponent();
        setGold(Math.min(gold.gold + value, gold.maxGold), gold.maxGold);
        keeper.setGoldMined(keeper.getGoldMined() + value); // Nope, need to get this some other way
    }

    public boolean subGold(int value) {
        Gold gold = getGoldComponent();
        if (gold.gold >= value && value > 0) {
            setGold(gold.gold - value, gold.maxGold);
            return true;
        }
        return false;
    }

    /**
     * Get the player's gold
     *
     * @return the amount of gold the player has
     */
    public int getGold() {
        return getGoldComponent().gold;
    }

    public void setGoldMax(int goldMax) {
        setGold(getGold(), goldMax);
    }

    public int getGoldMax() {
        return getGoldComponent().maxGold;
    }

    public boolean isFullCapacity() {
        Gold gold = getGoldComponent();
        return gold.gold >= gold.maxGold;
    }

    private Gold getGoldComponent() {
        return entityData.getComponent(getEntityId(), Gold.class);
    }

    private void setGold(int gold, int maxGold) {
        entityData.setComponent(getEntityId(), new Gold(gold, maxGold));
    }

    private EntityId getEntityId() {
        return entityData.findEntity(new FieldFilter(KeeperComponent.class, "playerId", keeper.getId()), KeeperComponent.class);
    }
}
