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
package toniarts.openkeeper.game.player;

import com.jme3.app.state.AppStateManager;
import java.util.logging.Logger;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 *
 * @author ArchDemon
 */
public class PlayerTriggerControl extends TriggerControl {

    private PlayerState playerState = null;
    private final short playerId;
    private static final Logger logger = Logger.getLogger(PlayerTriggerControl.class.getName());

    public PlayerTriggerControl() { // empty serialization constructor
        super();
        playerId = Keeper.KEEPER1_ID;
    }

    public PlayerTriggerControl(final AppStateManager stateManager, int triggerId, short playerId) {
        super(stateManager, triggerId);
        this.playerId = playerId;
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = playerState;
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = super.isActive(trigger);
        if (checked) {
            return result;
        }

        result = false;
        int target = 0;
        int value = 0;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case PLAYER_CREATURES:
                short playerId = trigger.getUserData("playerId", short.class);
                Keeper keeper = getPlayer(playerId);
                short creatureId = trigger.getUserData("creatureId", short.class);
                boolean isValue = trigger.getUserData("flag", short.class) == 1;
                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                    if (creatureId == 0) {
                        target = keeper.getCreatureControl().getTypeCount();
                    } else {
                        GameState gameState = stateManager.getState(GameState.class);
                        target = keeper.getCreatureControl().getTypeCount(gameState.getLevelData().getCreature(creatureId));
                    }
                } else {
                    // TODO what?
                    return false;
                }
                break;
            case PLAYER_HAPPY_CREATURES:
                return false;
            case PLAYER_ANGRY_CREATURES:
                return false;
            case PLAYER_CREATURES_KILLED:
                return false;
            case PLAYER_KILLS_CREATURES:
                return false;
            case PLAYER_ROOM_SLABS:
                return false;
            case PLAYER_ROOMS: {
                playerId = trigger.getUserData("playerId", short.class);
                keeper = getPlayer(playerId);
                short roomId = trigger.getUserData("roomId", short.class);
                isValue = trigger.getUserData("flag", short.class) == 1;
                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                    if (roomId == 0) {
                        target = keeper.getRoomControl().getTypeCount();
                    } else {
                        GameState gameState = stateManager.getState(GameState.class);
                        target = keeper.getRoomControl().getTypeCount(gameState.getLevelData().getRoomById(roomId));
                    }
                } else {
                    // TODO what?
                    return false;
                }
                break;
            }
            case PLAYER_ROOM_SIZE:
                return false;
            case PLAYER_DOORS:
                return false;
            case PLAYER_TRAPS:
                return false;
            case PLAYER_KEEPER_SPELL:
                return false;
            case PLAYER_GOLD:
                playerId = trigger.getUserData("playerId", short.class);
                keeper = getPlayer(playerId);
                target = keeper.getGoldControl().getGold();
                isValue = trigger.getUserData("flag", short.class) == 1;
                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    // TODO what?
                    return false;
                }
                break;

            case PLAYER_GOLD_MINED:
                playerId = trigger.getUserData("playerId", short.class);
                keeper = getPlayer(playerId);
                target = keeper.getGoldControl().getGoldMined();
                isValue = trigger.getUserData("flag", short.class) == 1;
                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    // TODO what?
                    return false;
                }
                break;

            case PLAYER_MANA:
                PlayerManaControl pmc = playerState.getManaControl();
                target = pmc.getMana();
                break;

            case PLAYER_DESTROYS:
                return false;
            case PLAYER_CREATURES_AT_LEVEL:
                return false;
            case PLAYER_KILLED:
                return false;
            case PLAYER_DUNGEON_BREACHED:
                return false;
            case PLAYER_ENEMY_BREACHED:
                return false;
            case PLAYER_CREATURE_PICKED_UP:
                return false;
            case PLAYER_CREATURE_DROPPED:
                return false;
            case PLAYER_CREATURE_SLAPPED:
                PlayerStatsControl psc = playerState.getStatsControl();
                creatureId = trigger.getUserData("creatureId", short.class);
                if (creatureId == 0) {

                    // Any creature
                    return psc.hasSlapped();
                } else {

                    // Certain creature
                    return psc.hasSlapped(stateManager.getState(GameState.class).getLevelData().getCreature(creatureId));
                }
            case PLAYER_CREATURE_SACKED:
                return false;
            case PLAYER_ROOM_FURNITURE:
                return false;
            case PLAYER_SLAPS:
                return false;
            case PLAYER_CREATURES_GROUPED:
                return false;
            case PLAYER_CREATURES_DYING:
                return false;
            case GUI_TRANSITION_ENDS:
                return playerState.isTransitionEnd();

            case GUI_BUTTON_PRESSED:
                return false;
            default:
                logger.warning("Target Type not supported");
                return false;
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, value);
        }

        return result;
    }

    @Override
    protected Keeper getPlayer(short playerId) {
        if (playerId == 0) {
            return super.getPlayer(this.playerId);
        }
        return super.getPlayer(playerId); //To change body of generated methods, choose Tools | Templates.
    }

}
