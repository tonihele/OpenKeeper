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
 * Builds {@link Door} fixtures for tests. Lives in {@code Door}'s own package
 * because its setters are {@code protected} (parser-only).
 */
public final class DoorFixtures {

    private DoorFixtures() {
    }

    public static Door door(short doorId, int goldCost, Door.DoorFlag... flags) {
        Door door = new Door();
        door.setDoorId(doorId);
        door.setGoldCost(goldCost);
        EnumSet<Door.DoorFlag> doorFlags = EnumSet.noneOf(Door.DoorFlag.class);
        for (Door.DoorFlag flag : flags) {
            doorFlags.add(flag);
        }
        door.setFlags(doorFlags);
        return door;
    }

    public static Door door(short doorId, int goldCost, int health) {
        Door door = door(doorId, goldCost);
        door.setHealth(health);
        return door;
    }

    public static Door doorToManufacture(short doorId, int manufToBuild, short manufCrateObjectId) {
        Door door = door(doorId, 0);
        door.setManufToBuild(manufToBuild);
        door.setManufCrateObjectId(manufCrateObjectId);
        return door;
    }

}
