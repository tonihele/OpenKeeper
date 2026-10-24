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

import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hero with the DESTROY_ROOMS flag wrecks the keeper's room tiles he walks
 * over. DKII.EXE: the hit (0x497DF0) only lands on a room that is buildable
 * and placed on land, and takes ten times ATTACK_ROOM_HEALTH off the tile
 * (0x452F60). At no health the room tile is lost (0x4C5DB0)
 */
class DestroyRoomTileTest {

    private final RoomTileWorld world = new RoomTileWorld();
    private final ICreatureController hero = RoomTileWorld.creature(Player.GOOD_PLAYER_ID);

    @Test
    void targetsTheKeepersRoomTile() {
        DestroyRoomTile task = createTask();

        assertTrue(task.isValid(hero));
        assertEquals(WorldUtils.pointToVector2f(RoomTileWorld.TILE), task.getTarget(hero));
        assertEquals(TaskType.DESTROY_ROOM_TILE, task.getTaskType());
    }

    @Test
    void leavesHeroAndNeutralRoomsAlone() {
        world.ownerId = Player.GOOD_PLAYER_ID;
        assertFalse(createTask().isValid(hero));

        world.ownerId = Player.NEUTRAL_PLAYER_ID;
        assertFalse(createTask().isValid(hero));
    }

    @Test
    void leavesTheHeartPortalAndBridgesAlone() {
        world.room = DestroyRoomsFixtures.destroyRoomsRoom("Dungeon Heart", RoomTileWorld.LAIR_TERRAIN_ID,
                Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.ATTACKABLE);
        assertFalse(createTask().isValid(hero));

        world.room = DestroyRoomsFixtures.destroyRoomsRoom("Portal", RoomTileWorld.LAIR_TERRAIN_ID,
                Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.HAS_FLAME);
        assertFalse(createTask().isValid(hero));

        world.room = DestroyRoomsFixtures.destroyRoomsRoom("Stone Bridge", RoomTileWorld.LAIR_TERRAIN_ID,
                Room.RoomFlag.PLACEABLE_ON_WATER, Room.RoomFlag.PLACEABLE_ON_LAVA, Room.RoomFlag.BUILDABLE);
        assertFalse(createTask().isValid(hero));
    }

    @Test
    void destroysTheLibraryThatIsNotAttackable() {
        world.room = DestroyRoomsFixtures.destroyRoomsRoom("Library", RoomTileWorld.LAIR_TERRAIN_ID,
                Room.RoomFlag.PLACEABLE_ON_LAND, Room.RoomFlag.HAS_WALLS, Room.RoomFlag.BUILDABLE);

        assertTrue(createTask().isValid(hero));
    }

    @Test
    void leavesTilesWithoutARoomAlone() {
        world.terrain = RoomTileWorld.claimedPath();

        assertFalse(createTask().isValid(hero));
    }

    @Test
    void oneBlowLosesTheTileAtTheDefaultVariables() {
        DestroyRoomTile task = createTask();
        task.assign(hero, false);

        task.executeTask(hero, 0.5f);
        assertEquals(RoomTileWorld.ROOM_TILE_HEALTH, world.health);
        assertTrue(world.destroyedTiles.isEmpty());

        task.executeTask(hero, 1.0f);
        assertEquals(List.of(RoomTileWorld.TILE), world.destroyedTiles);

        // The tile is the keeper's claimed path now, with the health of one
        assertEquals(RoomTileWorld.CLAIMED_PATH_HEALTH, world.health);
        assertFalse(task.isValid(hero));
        task.executeTask(hero, 2.0f);
        assertEquals(List.of(RoomTileWorld.TILE), world.destroyedTiles);
    }

    @Test
    void weakerBlowsTakeSeveralSwings() {
        world.setAttackRoomHealth(-400);
        DestroyRoomTile task = createTask();
        task.assign(hero, false);

        task.executeTask(hero, 1.0f);
        assertEquals(6000, world.health);
        task.executeTask(hero, 1.5f);
        task.executeTask(hero, 2.0f);
        assertEquals(2000, world.health);
        assertTrue(world.destroyedTiles.isEmpty());

        task.executeTask(hero, 3.0f);
        assertEquals(List.of(RoomTileWorld.TILE), world.destroyedTiles);
    }

    private DestroyRoomTile createTask() {
        return new DestroyRoomTile(null, world.mapController, world.gameWorldController, world.gameSettings,
                RoomTileWorld.TILE, Player.GOOD_PLAYER_ID);
    }

}
