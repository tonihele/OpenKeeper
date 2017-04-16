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
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.game.player.PlayerCreatureControl;
import toniarts.openkeeper.game.player.PlayerGoldControl;
import toniarts.openkeeper.game.player.PlayerManaControl;
import toniarts.openkeeper.game.player.PlayerRoomControl;
import toniarts.openkeeper.game.player.PlayerSpellControl;
import toniarts.openkeeper.game.player.PlayerStatsControl;
import toniarts.openkeeper.game.player.PlayerTriggerControl;
import toniarts.openkeeper.tools.convert.map.AI.AIType;
import toniarts.openkeeper.tools.convert.map.Player;

/**
 * Your FRIENDLY neighbourhood Keeper, or not
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Keeper implements Comparable<Keeper> {

    public final static short KEEPER1_ID = 3;
    public final static short KEEPER2_ID = 4;
    public final static short KEEPER3_ID = 5;
    public final static short KEEPER4_ID = 6;

    private boolean ai;
    private AIType aiType = AIType.MASTER_KEEPER;
    private String name;
    private boolean ready = false;
    private transient Player player;
    private short id;
    private int initialGold = 0;
    private transient PlayerGoldControl goldControl = new PlayerGoldControl();
    private transient PlayerCreatureControl creatureControl;
    private transient PlayerSpellControl spellControl;
    private transient PlayerStatsControl statsControl = new PlayerStatsControl();
    private transient PlayerRoomControl roomControl;
    private transient PlayerTriggerControl triggerControl;
    private transient PlayerManaControl manaControl;
    private boolean destroyed = false;
    private final Set<Short> allies = new HashSet<>(4);

    public Keeper() {

    }

    public Keeper(boolean ai, String name, short id, final Application app) {
        this.ai = ai;
        this.name = name;
        this.id = id;

        // AI is always ready
        ready = ai;
    }

    public Keeper(Player player, final Application app) {
        this.player = player;
        this.id = player.getPlayerId();
        initialGold = player.getStartingGold();
    }

    public void initialize(final AppStateManager stateManager, final Application app) {
        creatureControl = new PlayerCreatureControl(app);
        roomControl = new PlayerRoomControl(app);
        spellControl = new PlayerSpellControl(app);

        int triggerId = player.getTriggerId();
        if (triggerId != 0) {
            triggerControl = new PlayerTriggerControl(stateManager, triggerId);
            triggerControl.setPlayer(id);
        }

        // Don't create mana control for neutral nor good player
        if (id != Player.GOOD_PLAYER_ID && id != Player.NEUTRAL_PLAYER_ID) {
            manaControl = new PlayerManaControl(id, stateManager);
        }
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
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

    public String getName() {
        return name;
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

    public PlayerSpellControl getSpellControl() {
        return spellControl;
    }

    public void update(float tpf) {
        if (triggerControl != null) {
            triggerControl.update(tpf);
        }

        if (manaControl != null) {
            manaControl.update(tpf);
        }
    }

    public void setPlayer(Player player) {
        this.player = player;

        // Set the gold
        initialGold = player.getStartingGold();
    }

    @Override
    public String toString() {
        return (ai ? aiType.toString() : name);
    }

    /**
     * Check whether the player is destroyed or not
     *
     * @return is destroyed
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Is the given player an enemy of ours
     *
     * @param playerId the other player
     * @return is the player enemy of ours
     * @see #isAlly(short)
     */
    public boolean isEnemy(short playerId) {
        if (playerId == id || playerId == Player.NEUTRAL_PLAYER_ID || id == Player.NEUTRAL_PLAYER_ID) {
            return false; // Neutral player has no enemies
        }
        return !allies.contains(playerId);
    }

    /**
     * Is the player an ally of us. If not, it is not necessarily the enemy. But
     * we do not share vision, battless etc. with non-allies
     *
     * @param playerId the other player
     * @return is the player an ally of ours
     * @see #isEnemy(short)
     */
    public boolean isAlly(short playerId) {
        return id == playerId || allies.contains(playerId);
    }

    /**
     * Create alliance between two players, remember to set both players as
     * allies
     *
     * @param playerId the player to form alliance with
     */
    public void createAlliance(short playerId) {
        allies.add(playerId);
    }

    /**
     * Breaks alliance between two players, remember to break the alliance on
     * both players
     *
     * @param playerId the player to break alliance with
     */
    public void breakAlliance(short playerId) {
        allies.remove(playerId);
    }

    @Override
    public int compareTo(Keeper keeper) {
        return Short.compare(id, keeper.id);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Keeper other = (Keeper) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

}
