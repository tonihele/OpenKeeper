/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.task.hero;

import com.jme3.math.Vector2f;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.RoomAttackMessages;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A hero with the DESTROY_ROOMS flag wrecks the keeper's room tile he has
 * stepped on. He swings at it until it is gone, every blow takes ten times the
 * per tick ATTACK_ROOM_HEALTH off the tile, and when the tile runs out of
 * health the room loses it
 */
public final class DestroyRoomTile extends AbstractTileTask {

    /**
     * The heroes hit the room tiles ten times as hard as the keepers'
     * creatures (DKII.EXE 0x497DF0)
     */
    private static final int HERO_DAMAGE_MULTIPLIER = 10;

    /**
     * The time of one swing, in seconds, when the hero has no attack animation
     */
    private static final float DEFAULT_BLOW_DURATION = 1.0f;

    private final IGameWorldController gameWorldController;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final int ticksPerSec;
    private int blows = 0;

    public DestroyRoomTile(final INavigationService navigationService, final IMapController mapController,
            final IGameWorldController gameWorldController, final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            int ticksPerSec, Point p, short playerId) {
        super(navigationService, mapController, p, playerId);
        this.gameWorldController = gameWorldController;
        this.gameSettings = gameSettings;
        this.ticksPerSec = ticksPerSec;
    }

    /**
     * Can the player's hero wreck the room tile. The room has to belong to a
     * keeper, and be one that is built on land, so the dungeon heart, the
     * portals, the bridges and the hero gates are left alone
     *
     * @param mapController the map
     * @param p the tile
     * @param playerId the owner of the hero
     * @return true if the hero can wreck the tile
     */
    public static boolean isDestroyable(IMapController mapController, Point p, short playerId) {
        IMapTileController tile = mapController.getMapData().getTile(p);
        if (tile == null || !isKeeperTile(tile.getOwnerId(), playerId)
                || !mapController.getTerrain(tile).getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            return false;
        }
        IRoomController roomController = mapController.getRoomControllerByCoordinates(p);
        return roomController != null && isDestroyable(roomController.getRoom());
    }

    private static boolean isKeeperTile(short ownerId, short playerId) {
        return ownerId != playerId && ownerId != Player.GOOD_PLAYER_ID && ownerId != Player.NEUTRAL_PLAYER_ID;
    }

    private static boolean isDestroyable(Room room) {
        return room.getFlags().contains(Room.RoomFlag.BUILDABLE) && room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAND);
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return isDestroyable(mapController, getTaskLocation(), playerId);
    }

    /**
     * The blow lands as the hero starts his attack action, and he starts a new
     * one when the attack animation is over (DKII.EXE 0x458620)
     */
    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        int blowsLanded = (int) (executionDuration / getBlowDuration(creature)) + 1;
        while (blows < blowsLanded && isValid(creature)) {
            blows++;
            strike();
        }
    }

    private static float getBlowDuration(ICreatureController creature) {
        Creature creatureData = creature.getCreature();
        ArtResource attack = creatureData != null ? creatureData.getAnimation(Creature.AnimationType.MELEE_ATTACK) : null;
        Integer frames = attack != null ? attack.getData("frames") : null;
        Integer fps = attack != null ? attack.getData("fps") : null;
        if (frames == null || fps == null || frames <= 0 || fps <= 0) {
            return DEFAULT_BLOW_DURATION;
        }
        return frames / (float) fps;
    }

    /**
     * The level variables are kept per game tick (DKII.EXE 0x51CB80), so the
     * blow is ATTACK_ROOM_HEALTH / ticks per second, ten times over
     *
     * @return the damage of one blow
     */
    private int getBlowDamage() {
        int attackRoomHealth = (int) gameSettings.get(Variable.MiscVariable.MiscType.ATTACK_ROOM_HEALTH).getValue();
        return Math.abs(attackRoomHealth / ticksPerSec) * HERO_DAMAGE_MULTIPLIER;
    }

    private void strike() {
        IMapTileController tile = mapController.getMapData().getTile(getTaskLocation());
        short ownerId = tile.getOwnerId();
        Room room = mapController.getRoomControllerByCoordinates(getTaskLocation()).getRoom();
        RoomAttackMessages messages = gameWorldController.getRoomAttackMessages();
        tile.setHealth(Math.max(0, tile.getHealth() - getBlowDamage()));
        if (tile.getHealth() > 0) {
            if (messages != null) {
                messages.roomTileAttacked(ownerId, room, getTaskLocation());
            }
            return;
        }

        // The room loses the tile, it turns to the keeper's claimed path
        int roomsBefore = countRooms(room, ownerId);
        gameWorldController.destroyRoomTiles(List.of(getTaskLocation()));
        if (messages != null) {
            messages.roomLost(ownerId, room, roomsBefore, countRooms(room, ownerId), getTaskLocation());
        }
    }

    private int countRooms(Room room, short ownerId) {
        int count = 0;
        for (IRoomController roomController : mapController.getRoomControllers()) {
            if (roomController.getOwnerId() == ownerId && room.equals(roomController.getRoom())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return "Destroy room tile at " + getTaskLocation();
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.DESTROY_ROOM_TILE;
    }

}
