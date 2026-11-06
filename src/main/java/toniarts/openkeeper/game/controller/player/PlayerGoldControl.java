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

import com.jme3.util.SafeArrayList;
import toniarts.openkeeper.game.controller.GameWorldController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.PlayerGoldListener;
import toniarts.openkeeper.game.listener.RoomListener;

/**
 *
 * @author ArchDemon
 */
public final class PlayerGoldControl implements RoomListener {

    private int goldMax = 0;
    private final Keeper keeper;
    private final SafeArrayList<PlayerGoldListener> listeners = new SafeArrayList<>(PlayerGoldListener.class);

    public PlayerGoldControl(Keeper keeper) {
        this.keeper = keeper;
    }

    public void addGold(int value) {
        if (value < 0) {
            return;
        }

        keeper.setGold(Math.min(keeper.getGold() + value, goldMax));
        keeper.setGoldMined(keeper.getGoldMined() + value); // Nope, need to get this some other way
        updateListeners();
    }

    public boolean subGold(int value) {
        if (keeper.getGold() >= value && value > 0) {
            keeper.setGold(keeper.getGold() - value);
            updateListeners();
            return true;
        }
        return false;
    }

    public void addListener(PlayerGoldListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PlayerGoldListener listener) {
        listeners.remove(listener);
    }

    private void updateListeners() {
        for (PlayerGoldListener listener : listeners.getArray()) {
            listener.onGoldChange(keeper.getId(), keeper.getGold());
        }
    }

    public void setGoldMax(int goldMax) {
        this.goldMax = goldMax;
    }

    public int getGoldMax() {
        return goldMax;
    }

    public boolean isFullCapacity() {
        return keeper.getGold() >= goldMax;
    }

    @Override
    public void onBuild(IRoomController room) {
        // GameWorldController adds the capacity of the rooms we build
    }

    /**
     * A gold room we capture brings its capacity and the gold stored in it to
     * us. The gold is not mined, so it doesn't count as such.
     *
     * @param room the captured room
     */
    @Override
    public void onCaptured(IRoomController room) {
        if (room.canStoreGold()) {
            synchronized (GameWorldController.GOLD_LOCK) {
                goldMax += room.getMaxCapacity(ObjectType.GOLD);
                keeper.setGold(Math.min(keeper.getGold() + room.getUsedCapacity(ObjectType.GOLD), goldMax));
                updateListeners();
            }
        }
    }

    /**
     * A gold room the enemy captures takes its capacity and the gold stored
     * in it away from us
     *
     * @param room the lost room
     */
    @Override
    public void onCapturedByEnemy(IRoomController room) {
        if (room.canStoreGold()) {
            synchronized (GameWorldController.GOLD_LOCK) {
                goldMax -= room.getMaxCapacity(ObjectType.GOLD);
                keeper.setGold(Math.max(keeper.getGold() - room.getUsedCapacity(ObjectType.GOLD), 0));
                updateListeners();
            }
        }
    }

    @Override
    public void onSold(IRoomController room) {
        // GameWorldController takes away the capacity and gold of the rooms we sell
    }
}
