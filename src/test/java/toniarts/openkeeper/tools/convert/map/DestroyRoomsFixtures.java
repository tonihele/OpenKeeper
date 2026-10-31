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
package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;

/**
 * Builds the rooms, terrain, variables and heroes for the tests of the heroes
 * that destroy rooms. Lives in the data classes' own package because their
 * setters are {@code protected} (parser-only).
 */
public final class DestroyRoomsFixtures {

    private DestroyRoomsFixtures() {
    }

    /**
     * A room type
     *
     * @param name the room name
     * @param terrainId the terrain of the room tiles
     * @param flags the room flags
     * @return the room
     */
    public static Room destroyRoomsRoom(String name, short terrainId, Room.RoomFlag... flags) {
        Room room = new Room();
        room.setName(name);
        room.setTerrainId(terrainId);
        EnumSet<Room.RoomFlag> set = EnumSet.noneOf(Room.RoomFlag.class);
        for (Room.RoomFlag flag : flags) {
            set.add(flag);
        }
        room.setFlags(set);
        return room;
    }

    /**
     * Set the ID of a room type
     *
     * @param room the room
     * @param roomId the room ID
     */
    public static void setRoomId(Room room, short roomId) {
        room.setRoomId(roomId);
    }

    /**
     * The level settings
     *
     * @param ticksPerSec the game ticks per second
     * @return the level settings
     */
    public static GameLevel destroyRoomsGameLevel(int ticksPerSec) {
        GameLevel gameLevel = new GameLevel();
        gameLevel.setTicksPerSec(ticksPerSec);
        return gameLevel;
    }

    /**
     * A terrain
     *
     * @param terrainId the terrain ID
     * @param destroyedTypeTerrainId the terrain the tile turns to when its
     * health runs out
     * @param maxHealth the maximum health of the tile
     * @param flags the terrain flags
     * @return the terrain
     */
    public static Terrain destroyRoomsTerrain(short terrainId, short destroyedTypeTerrainId, int maxHealth, Terrain.TerrainFlag... flags) {
        Terrain terrain = new Terrain();
        terrain.setTerrainId(terrainId);
        terrain.setDestroyedTypeTerrainId(destroyedTypeTerrainId);
        terrain.setMaxHealth(maxHealth);
        terrain.setStartingHealth(maxHealth);
        EnumSet<Terrain.TerrainFlag> set = EnumSet.noneOf(Terrain.TerrainFlag.class);
        for (Terrain.TerrainFlag flag : flags) {
            set.add(flag);
        }
        terrain.setFlags(set);
        return terrain;
    }

    /**
     * A level variable
     *
     * @param type the variable
     * @param value its value
     * @return the variable
     */
    public static Variable.MiscVariable destroyRoomsVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

    /**
     * A hero placed on the map, or in a hero party
     *
     * @param flags2 the second set of creature flags, may be {@code null}
     * @return the hero
     */
    public static Thing.GoodCreature destroyRoomsHero(EnumSet<Thing.Creature.CreatureFlag2> flags2) {
        Thing.GoodCreature hero = new Thing.GoodCreature();
        hero.setFlags2(flags2);
        return hero;
    }

}
