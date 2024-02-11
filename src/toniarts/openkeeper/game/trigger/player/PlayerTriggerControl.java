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
package toniarts.openkeeper.game.trigger.player;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Set;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.controller.room.ICreatureEntrance;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.trigger.TriggerActionData;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Trigger control that is targeted for specified player
 *
 * @author ArchDemon
 */
public class PlayerTriggerControl extends TriggerControl {
    
    private static final Logger logger = System.getLogger(PlayerTriggerControl.class.getName());

    private short playerId;
    private PlayerService playerService;

    /**
     * empty serialization constructor
     */
    public PlayerTriggerControl() {
        super();
    }

    public PlayerTriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final int triggerId, final short playerId,
            final PlayerService playerService) {
        super(gameController, levelInfo, gameTimer, mapController, creaturesController, triggerId);
        this.playerId = playerId;
        this.playerService = playerService;
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
                boolean isAvailable = trigger.getUserData("flag", short.class) == 1;
                short keeperSpellId = trigger.getUserData("targetId", short.class);

                KeeperSpell keeperSpell = levelInfo.getLevelData().getKeeperSpellById(keeperSpellId);

                return isAvailable == getPlayerController(playerId).getSpellControl().isAvailable(keeperSpell);

            case PLAYER_GOLD:
                isValue = trigger.getUserData("flag", short.class) == 1;

                target = getPlayer().getGold();

                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    short otherPlayerId = trigger.getUserData("playerId", short.class);
                    value = getPlayer(otherPlayerId).getGold();
                }
                break;

            case PLAYER_GOLD_MINED:
                isValue = trigger.getUserData("flag", short.class) == 1;

                target = getPlayer().getGoldMined();

                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    short otherPlayerId = trigger.getUserData("playerId", short.class);
                    value = getPlayer(otherPlayerId).getGoldMined();
                }
                break;

            case PLAYER_MANA:
                isValue = trigger.getUserData("flag", short.class) == 1;

                target = getPlayer().getMana();

                if (isValue) {
                    value = trigger.getUserData("value", int.class);
                } else {
                    short otherPlayerId = trigger.getUserData("playerId", short.class);
                    value = getPlayer(otherPlayerId).getMana();
                }
                break;

            case PLAYER_DESTROYS:
                return false;
            case PLAYER_CREATURES_AT_LEVEL:
                return false;
            case PLAYER_KILLED:

                // TODO: Is player killed specifically by this other player
                short otherPlayerId = trigger.getUserData("playerId", short.class);
                return false;
            case PLAYER_DUNGEON_BREACHED:
                return false;
            case PLAYER_ENEMY_BREACHED:
                return false;

            case PLAYER_CREATURE_PICKED_UP:
                PlayerStatsControl psc = getPlayerController(playerId).getStatsControl();
                creatureId = trigger.getUserData("creatureId", short.class);

                if (creatureId == 0) {
                    // Any creature
                    return psc.hasPickedUp();
                } else {
                    // Certain creature
                    return psc.hasPickedUp(levelInfo.getLevelData().getCreature(creatureId));
                }

            case PLAYER_CREATURE_DROPPED:
                psc = getPlayerController(playerId).getStatsControl();
                creatureId = trigger.getUserData("creatureId", short.class);

                if (creatureId == 0) {
                    // Any creature
                    return psc.hasDropped();
                } else {
                    // Certain creature
                    return psc.hasDropped(levelInfo.getLevelData().getCreature(creatureId));
                }

            case PLAYER_CREATURE_SLAPPED:
                psc = getPlayerController(playerId).getStatsControl();
                creatureId = trigger.getUserData("creatureId", short.class);

                if (creatureId == 0) {
                    // Any creature
                    return psc.hasSlapped();
                } else {
                    // Certain creature
                    return psc.hasSlapped(levelInfo.getLevelData().getCreature(creatureId));
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
                return !playerService.isInTransition();
//                return playerState.isTransitionEnd();

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
                gameController.endGame(playerId, true);
                break;

            case LOSE_GAME: // Game part. only for keeper x
                gameController.endGame(playerId, false);
                break;

            case GENERATE_CREATURE: // Creature part. Only for keeper x
                short creatureId = trigger.getUserData("creatureId", short.class);
                short level = trigger.getUserData("level", short.class);

                // Get first spawn point of the player (this flag is only for the players)
                Set<IRoomController> rooms = getPlayerController(playerId).getRoomControl().getRoomControllers().get(levelInfo.getLevelData().getPortal());
                if (rooms == null || rooms.isEmpty()) {
                    logger.log(Level.WARNING, "Generate creature triggered but no entrances found!");
                    break;
                }
                ICreatureEntrance room = ((ICreatureEntrance) rooms.iterator().next());
                creaturesController.spawnCreature(creatureId, playerId, level, WorldUtils.pointToVector2f(room.getEntranceCoordinate()), true);
                break;

            case SET_PORTAL_STATUS: // Creature part. Only for keeper x
                boolean available = trigger.getUserData("available", short.class) != 0;
                getPlayerController(playerId).getRoomControl().setPortalsOpen(available);
                break;

            case FLASH_BUTTON: // gui part. Only for keeper x
//                if (playerId == playerState.getPlayerId()) {
                TriggerAction.MakeType buttonType = ConversionUtils.parseEnum(trigger.getUserData("type", short.class),
                        TriggerAction.MakeType.class);
                short targetId = trigger.getUserData("targetId", short.class);
                TriggerAction.ButtonType targetButtonType = null;
                if (buttonType == TriggerAction.MakeType.MISC_BUTTON) {
                    targetButtonType = ConversionUtils.parseEnum(targetId,
                            TriggerAction.ButtonType.class);
                }
                available = trigger.getUserData("available", short.class) != 0;
                int time = trigger.getUserData("value", int.class);
                playerService.flashButton(buttonType, targetId, targetButtonType, available, time, playerId);
//                    playerState.flashButton(targetId, buttonType, available, time);
//                }
                break;

            case FOLLOW_CAMERA_PATH: // gui part. Only for keeper x
//                if (playerId == playerState.getPlayerId()) {
//                    // TODO disable control
//                    //GameState.setEnabled(false);
//                    PlayerCameraState pcs = stateManager.getState(PlayerCameraState.class);
                ActionPoint ap = levelInfo.getActionPoint(trigger.getUserData("actionPointId", short.class));
//                    pcs.doTransition(trigger.getUserData("pathId", short.class), ap);
//                }
                playerService.doTransition(trigger.getUserData("pathId", short.class), WorldUtils.ActionPointToVector3f(ap), playerId);
                break;

            case MAKE_OBJECTIVE: // Game part
                targetId = trigger.getUserData("targetId", short.class);
                if (targetId == 0) { // 0 = Off
                    makeObjectiveOff();
                } else {
                    logger.log(Level.WARNING, "Unsupported MAKE_OBJECTIVE target {0}", targetId);
                }
                break;

            case FLASH_ACTION_POINT: // AP part
//                if (playerId == playerState.getPlayerId()) {
                ap = levelInfo.getActionPoint(trigger.getUserData("actionPointId", short.class));
                time = trigger.getUserData("value", int.class);
                available = trigger.getUserData("available", short.class) != 0;
                if (available) {
                    mapController.flashTiles(ap.getPoints(), playerId, time);
                } else {
                    mapController.unFlashTiles(ap.getPoints(), playerId);
                }
//                    ap.getParent().getWorldState().flashTile(available, ap.getPoints());
//                }
                break;

            case REVEAL_ACTION_POINT: // AP part
//                if (playerId == playerState.getPlayerId()) {
//                    // TODO this
//                    // remove fog of war from tiles in action point
//                    // or
//                    // add fog of war to tiles in action point
//                }
                break;

            case ZOOM_TO_ACTION_POINT: // AP part
//                if (playerId == playerState.getPlayerId()) {
                short apId = trigger.getUserData("targetId", short.class);
                zoomToAP(apId);
//                }
                break;

            case ROTATE_AROUND_ACTION_POINT: // AP part
//                if (playerId == playerState.getPlayerId()) {
                ap = levelInfo.getActionPoint(trigger.getUserData("actionPointId", short.class));
                boolean isRelative = trigger.getUserData("available", short.class) == 0;
                int angle = trigger.getUserData("angle", int.class);
                time = trigger.getUserData("time", int.class);
//
//                    PlayerCameraState pcs = stateManager.getState(PlayerCameraState.class);
//                    ap.addControl(new PlayerCameraRotateControl(pcs.getCamera(), isRelative, angle, time));
                playerService.rotateViewAroundPoint(WorldUtils.ActionPointToVector3f(ap), isRelative, angle, time, playerId);
//                }
                break;

            case DISPLAY_OBJECTIVE: // Info part
//                if (playerId == playerState.getPlayerId()) {
                // TODO this
                int objectiveId = trigger.getUserData("objectiveId", int.class); // limit 32767
                apId = trigger.getUserData("actionPointId", short.class);
                // if != 0 => Zoom To AP = this
                zoomToAP(apId);
//                }
                break;

            case PLAY_SPEECH: // Info part
                int speechId = trigger.getUserData("speechId", int.class);
                //stateManager.getState(SoundState.class).attachLevelSpeech(speechId);
                //stateManager.getState(SystemMessageState.class).addMessage(SystemMessageState.MessageType.INFO, String.format("${level.%d}", speechId - 1));
                int pathId = trigger.getUserData("pathId", int.class);
                // text show when Cinematic camera by pathId
                boolean introduction = trigger.getUserData("introduction", short.class) != 0;
                boolean showText = trigger.getUserData("text", short.class) == 0;
                playerService.playSpeech(speechId, showText, introduction, pathId, playerId);
                break;

            case DISPLAY_TEXT_STRING: // Info part
//                if (playerId == playerState.getPlayerId()) {
                int textId = trigger.getUserData("textId", int.class);
                playerService.showMessage(textId, playerId);
//                }
                break;

            case SET_WIDESCREEN_MODE: // Info part
                available = trigger.getUserData("available", short.class) != 0;
                playerService.setWidescreen(available, playerId);
                break;

            case DISPLAY_SLAB_OWNER: // Info part
//                if (playerId == playerState.getPlayerId()) {
//                    // TODO this
//                    available = trigger.getUserData("available", short.class) != 0;
//                }
                break;

            case DISPLAY_NEXT_ROOM_TYPE: // Info part
//                if (playerId == playerState.getPlayerId()) {
//                    // TODO this
//                    targetId = trigger.getUserData("targetId", short.class); // 0 = Off or roomId
//                }
                break;

            default:
                super.doAction(trigger);
                break;
        }
    }

    protected Keeper getPlayer() {
        return super.getPlayer(playerId);
    }

    private int getCreaturesCount(short playerId, short creatureId) {
        if (creatureId == 0) {
            return getPlayerController(playerId).getCreatureControl().getTypeCount();
        } else {
            return getPlayerController(playerId).getCreatureControl().getTypeCount(levelInfo.getLevelData().getCreature(creatureId));
        }
    }

    private int getRoomSlabsCount(short playerId, short roomId) {
        if (roomId == 0) {
            return getPlayerController(playerId).getRoomControl().getRoomSlabsCount();
        } else {
            return getPlayerController(playerId).getRoomControl().getRoomSlabsCount(levelInfo.getLevelData().getRoomById(roomId));
        }
    }

    private int getRoomCount(short playerId, short roomId) {
        if (roomId == 0) {
            return getPlayerController(playerId).getRoomControl().getTypeCount();
        } else {
            return getPlayerController(playerId).getRoomControl().getTypeCount(levelInfo.getLevelData().getRoomById(roomId));
        }
    }

    protected void makeObjectiveOff() {
        //TODO this
    }

    private void zoomToAP(short actionPointId) {
        ActionPoint ap = levelInfo.getActionPoint(actionPointId);
        if (ap != null) {
            playerService.zoomViewToPoint(WorldUtils.ActionPointToVector3f(ap), playerId);
        }
    }

    protected PlayerService getPlayerService() {
        return playerService;
    }

}
