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
import java.awt.Point;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.action.ActionPoint;
import toniarts.openkeeper.game.action.FlashControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.CreatureSpawnLogicState;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.game.state.SoundState;
import toniarts.openkeeper.game.state.SystemMessageState;
import toniarts.openkeeper.game.trigger.TriggerActionData;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.ICreatureEntrance;

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

    public PlayerTriggerControl(final AppStateManager stateManager, int triggerId) {
        super(stateManager, triggerId);
        this.playerState = this.stateManager.getState(PlayerState.class);
    }

    public void setPlayer(short playerId) {
        this.playerId = playerId;
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = false;

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
                return super.isActive(trigger);
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, value);
        }

        return result;
    }

    @Override
    protected void doAction(TriggerActionData trigger) {

        TriggerAction.ActionType type = trigger.getType();
        switch (type) {
            case WIN_GAME: // Game part. only for keeper x
                if (playerId == playerState.getPlayerId()) {
                    stateManager.getState(GameState.class).setEnd(true);
                }
                break;

            case LOSE_GAME: // Game part. only for keeper x
                if (playerId == playerState.getPlayerId()) {
                    stateManager.getState(GameState.class).setEnd(false);
                }
                break;

            case GENERATE_CREATURE: // Creature part. Only for keeper x
                short creatureId = trigger.getUserData("creatureId", short.class);
                short level = trigger.getUserData("level", short.class);
                Keeper keeper = getPlayer();

                // Get first spawn point of the player (this flag is only for the players)
                Set<GenericRoom> rooms = keeper.getRoomControl().getTypes().get(stateManager.getState(GameState.class).getLevelData().getPortal());
                if (rooms == null || rooms.isEmpty()) {
                    logger.warning("Generate creature triggered but no entrances found!");
                    break;
                }
                ICreatureEntrance room = ((ICreatureEntrance) rooms.iterator().next());
                room.spawnCreature(creatureId, level,
                        stateManager.getState(GameState.class).getApplication(),
                        stateManager.getState(WorldState.class).getThingLoader());
                break;

            case SET_PORTAL_STATUS: // Creature part. Only for keeper x
                boolean available = trigger.getUserData("available", short.class) != 0;
                getPlayer().getRoomControl().setPortalsOpen(available);
                break;

            case FLASH_BUTTON: // gui part. Only for keeper x
                if (playerId == playerState.getPlayerId()) {
                    TriggerAction.MakeType buttonType = ConversionUtils.parseEnum(trigger.getUserData("type", short.class),
                            TriggerAction.MakeType.class);
                    short targetId = trigger.getUserData("targetId", short.class);
                    available = trigger.getUserData("available", short.class) != 0;
                    int time = trigger.getUserData("value", int.class);
                    playerState.flashButton(targetId, buttonType, available, time);
                }
                break;

            case FOLLOW_CAMERA_PATH: // gui part. Only for keeper x
                if (playerId == playerState.getPlayerId()) {
                    // TODO disable control
                    //GameState.setEnabled(false);
                    PlayerCameraState pcs = stateManager.getState(PlayerCameraState.class);
                    ActionPoint ap = getActionPoint(trigger.getUserData("actionPointId", short.class));
                    pcs.doTransition(trigger.getUserData("pathId", short.class), ap);
                }
                break;

            case MAKE_OBJECTIVE: // Game part
                short targetId = trigger.getUserData("targetId", short.class);
                if (targetId == 0) { // 0 = Off
                    makeObjectiveOff();
                } else {
                    logger.log(Level.WARNING, "Unsupported MAKE_OBJECTIVE target {0}", targetId);
                }
                break;

            case FLASH_ACTION_POINT: // AP part
                if (playerId == playerState.getPlayerId()) {
                    ActionPoint ap = getActionPoint(trigger.getUserData("actionPointId", short.class));
                    int time = trigger.getUserData("value", int.class);
                    available = trigger.getUserData("available", short.class) != 0;
                    if (available && time != 0) {
                        ap.addControl(new FlashControl(time));
                    } else if (!available) {
                        ap.removeControl(FlashControl.class);
                    }
                    ap.getParent().getWorldState().flashTile(available, ap.getPoints());
                }
                break;

            case REVEAL_ACTION_POINT: // AP part
                if (playerId == playerState.getPlayerId()) {
                    // TODO this
                    // remove fog of war from tiles in action point
                    // or
                    // add fog of war to tiles in action point
                }
                break;

            case ZOOM_TO_ACTION_POINT: // AP part
                if (playerId == playerState.getPlayerId()) {
                    short apId = trigger.getUserData("targetId", short.class);
                    zoomToAP(apId);
                }
                break;

            case ROTATE_AROUND_ACTION_POINT: // AP part
                if (playerId == playerState.getPlayerId()) {
                    ActionPoint ap = getActionPoint(trigger.getUserData("targetId", short.class));
                    boolean isRelative = trigger.getUserData("available", short.class) == 0;
                    int angle = trigger.getUserData("angle", int.class);
                    int time = trigger.getUserData("time", int.class);

                    PlayerCameraState pcs = stateManager.getState(PlayerCameraState.class);
                    ap.addControl(new PlayerCameraRotateControl(pcs.getCamera(), isRelative, angle, time));
                }
                break;

            case DISPLAY_OBJECTIVE: // Info part
                if (playerId == playerState.getPlayerId()) {
                    // TODO this
                    int objectiveId = trigger.getUserData("objectiveId", int.class); // limit 32767
                    short apId = trigger.getUserData("actionPointId", short.class);
                    // if != 0 => Zoom To AP = this
                    zoomToAP(apId);
                }
                break;

            case PLAY_SPEECH: // Info part
                if (playerId == playerState.getPlayerId()) {
                    int speechId = trigger.getUserData("speechId", int.class);
                    stateManager.getState(SoundState.class).attachLevelSpeech(speechId);
                    stateManager.getState(SystemMessageState.class).addMessage(SystemMessageState.MessageType.INFO, String.format("${level.%d}", speechId - 1));
                    int pathId = trigger.getUserData("pathId", int.class);
                    // text show when Cinematic camera by pathId
                    boolean introduction = trigger.getUserData("introduction", short.class) != 0;
                    if (trigger.getUserData("text", short.class) == 0) {
                        playerState.setText(speechId, introduction, pathId);
                    }
                }
                break;

            case DISPLAY_TEXT_STRING: // Info part
                if (playerId == playerState.getPlayerId()) {
                    int textId = trigger.getUserData("textId", int.class);
                    // TODO display text message
                }
                break;

            case SET_WIDESCREEN_MODE: // Info part
                if (playerId == playerState.getPlayerId()) {
                    available = trigger.getUserData("available", short.class) != 0;
                    playerState.setWideScreen(available);
                }
                break;

            case DISPLAY_SLAB_OWNER: // Info part
                if (playerId == playerState.getPlayerId()) {
                    // TODO this
                    available = trigger.getUserData("available", short.class) != 0;
                }
                break;

            case DISPLAY_NEXT_ROOM_TYPE: // Info part
                if (playerId == playerState.getPlayerId()) {
                    // TODO this
                    targetId = trigger.getUserData("targetId", short.class); // 0 = Off or roomId
                }
                break;

            default:
                super.doAction(trigger);
                break;
        }
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

    protected void makeObjectiveOff() {
        //TODO this
    }
}
