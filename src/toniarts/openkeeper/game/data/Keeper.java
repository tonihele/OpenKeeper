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
package toniarts.openkeeper.game.data;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import toniarts.openkeeper.game.player.PlayerCreatureControl;
import toniarts.openkeeper.game.player.PlayerGoldControl;
import toniarts.openkeeper.game.player.PlayerManaControl;
import toniarts.openkeeper.game.player.PlayerRoomControl;
import toniarts.openkeeper.game.player.PlayerStatsControl;
import toniarts.openkeeper.game.player.PlayerTriggerControl;
import toniarts.openkeeper.tools.convert.map.AI.AIType;
import toniarts.openkeeper.tools.convert.map.Player;

/**
 * Your FRIENDLY neighbourhood Keeper, or not
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Keeper {

    public final static short KEEPER1_ID = 3;
    public final static short KEEPER2_ID = 4;
    public final static short KEEPER3_ID = 5;
    public final static short KEEPER4_ID = 6;

    private boolean ai;
    private AIType aiType = AIType.MASTER_KEEPER;
    private String name;
    private boolean ready = false;
    private final Player player;
    private final short id;
    private int initialGold = 0;
    private final PlayerGoldControl goldControl = new PlayerGoldControl();
    private final PlayerCreatureControl creatureControl = new PlayerCreatureControl();
    private final PlayerStatsControl statsControl = new PlayerStatsControl();
    private final PlayerRoomControl roomControl = new PlayerRoomControl();
    private PlayerTriggerControl triggerControl;
    private PlayerManaControl manaControl;

    public Keeper(boolean ai, String name, short id) {
        this.ai = ai;
        this.name = name;
        player = null;
        this.id = id;

        // AI is always ready
        ready = ai;
    }

    public Keeper(Player player) {
        this.player = player;
        this.id = player.getPlayerId();
        initialGold = player.getStartingGold();
    }
    
    public void initialize(final AppStateManager stateManager, final Application app) {
        int triggerId = player.getTriggerId();
        if (triggerId != 0) {
            triggerControl = new PlayerTriggerControl(stateManager, triggerId, id);
        }
        
        manaControl = new PlayerManaControl(id, stateManager);
    }

    public boolean isReady() {
        return ready;
    }

    public short getId() {
        return id;
    }

    public int getInitialGold() {
        return initialGold;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerGoldControl getGoldControl() {
        return goldControl;
    }

    public PlayerCreatureControl getCreatureControl() {
        return creatureControl;
    }

    public PlayerStatsControl getStatsControl() {
        return statsControl;
    }

    public PlayerRoomControl getRoomControl() {
        return roomControl;
    }
    
    public PlayerTriggerControl getTriggerControl() {
        return triggerControl;
    }
    
    public PlayerManaControl getManaControl() {
        return manaControl;
    }
    
    public void update(float tpf) {
        if (triggerControl != null) {
            triggerControl.update(tpf);
        }
        
        if (manaControl != null) {
            manaControl.update(tpf);
        }
    }

    @Override
    public String toString() {
        return (ai ? aiType.toString() : name);
    }
}
