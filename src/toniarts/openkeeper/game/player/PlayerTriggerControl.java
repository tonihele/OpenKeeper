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
    private short playerId;
    private static final Logger logger = Logger.getLogger(PlayerTriggerControl.class.getName());

    /**
     * empty serialization constructor
     */
    public PlayerTriggerControl() {
        super();
    }

    public PlayerTriggerControl(final AppStateManager stateManager, int triggerId, short playerId) {
        super(stateManager, triggerId);

        this.playerState = this.stateManager.getState(PlayerState.class);
        this.playerId = playerId;
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
                short creatureId = trigger.getUserData("creatureId", short.class);
                boolean isValue = trigger.getUserData("flag", short.class) == 1;

                target = getCreaturesCount(playerId, creatureId);

                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    short otherPlayerId = trigger.getUserData("playerId", short.class);
                    value = getCreaturesCount(otherPlayerId, creatureId);
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
                short roomId = trigger.getUserData("roomId", short.class);
                isValue = trigger.getUserData("flag", short.class) == 1;

                target = getRoomSlabsCount(playerId, roomId);

                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    short otherPlayerId = trigger.getUserData("playerId", short.class);
                    value = getRoomSlabsCount(otherPlayerId, roomId);
                }
                break;

            case PLAYER_ROOMS:
                roomId = trigger.getUserData("roomId", short.class);
                isValue = trigger.getUserData("flag", short.class) == 1;

                target = getRoomCount(playerId, roomId);

                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    short otherPlayerId = trigger.getUserData("playerId", short.class);
                    value = getRoomCount(otherPlayerId, roomId);
                }
                break;

            case PLAYER_ROOM_SIZE:
                return false;
            case PLAYER_DOORS:
                return false;
            case PLAYER_TRAPS:
                return false;
            case PLAYER_KEEPER_SPELL:
                return false;
            case PLAYER_GOLD:
                isValue = trigger.getUserData("flag", short.class) == 1;

                target = getPlayer().getGoldControl().getGold();

                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    short otherPlayerId = trigger.getUserData("playerId", short.class);
                    value = getPlayer(otherPlayerId).getGoldControl().getGold();
                }
                break;

            case PLAYER_GOLD_MINED:
                isValue = trigger.getUserData("flag", short.class) == 1;

                target = getPlayer().getGoldControl().getGoldMined();

                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    short otherPlayerId = trigger.getUserData("playerId", short.class);
                    value = getPlayer(otherPlayerId).getGoldControl().getGoldMined();
                }
                break;

            case PLAYER_MANA:
                isValue = trigger.getUserData("flag", short.class) == 1;

                target = getPlayer().getManaControl().getMana();

                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    short otherPlayerId = trigger.getUserData("playerId", short.class);
                    value = getPlayer(otherPlayerId).getManaControl().getMana();
                }
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
                PlayerStatsControl psc = getPlayer().getStatsControl();
                creatureId = trigger.getUserData("creatureId", short.class);

                if (creatureId == 0) {
                    // Any creature
                    return psc.hasPickedUp();
                } else {
                    // Certain creature
                    return psc.hasPickedUp(stateManager.getState(GameState.class).getLevelData().getCreature(creatureId));
                }
            case PLAYER_CREATURE_DROPPED:
                psc = getPlayer().getStatsControl();
                creatureId = trigger.getUserData("creatureId", short.class);

                if (creatureId == 0) {
                    // Any creature
                    return psc.hasDropped();
                } else {
                    // Certain creature
                    return psc.hasDropped(stateManager.getState(GameState.class).getLevelData().getCreature(creatureId));
                }
            case PLAYER_CREATURE_SLAPPED:
                psc = getPlayer().getStatsControl();
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

    private Keeper getPlayer() {
        return super.getPlayer(playerId);
    }

    private int getCreaturesCount(int playerId, short creatureId) {
        int result;

        if (creatureId == 0) {
            result = getPlayer((short) playerId).getCreatureControl().getTypeCount();
        } else {
            GameState gameState = stateManager.getState(GameState.class);
            result = getPlayer((short) playerId).getCreatureControl().getTypeCount(gameState.getLevelData().getCreature(creatureId));
        }

        return result;
    }

    private int getRoomSlabsCount(int playerId, short roomId) {
        int result;

        if (roomId == 0) {
            result = getPlayer((short) playerId).getRoomControl().getRoomSlabsCount();
        } else {
            GameState gameState = stateManager.getState(GameState.class);
            result = getPlayer((short) playerId).getRoomControl().getRoomSlabsCount(gameState.getLevelData().getRoomById(roomId));
        }

        return result;
    }

    private int getRoomCount(int playerId, short roomId) {
        int result;

        if (roomId == 0) {
            result = getPlayer((short) playerId).getRoomControl().getTypeCount();
        } else {
            GameState gameState = stateManager.getState(GameState.class);
            result = getPlayer((short) playerId).getRoomControl().getTypeCount(gameState.getLevelData().getRoomById(roomId));
        }

        return result;
    }
}
