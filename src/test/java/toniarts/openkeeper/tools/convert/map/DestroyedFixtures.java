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

/**
 * Doors with the effect they blow up with when destroyed
 */
public final class DestroyedFixtures {

    /**
     * The Wooden Door's death effect in the original data, "Wooden Door
     * Group Explosion"
     */
    public static final int WOODEN_DOOR_EXPLOSION_ID = 220;

    private DestroyedFixtures() {
    }

    public static Door door(short doorId, int deathEffectId) {
        Door door = new Door();
        door.setDoorId(doorId);
        door.setDeathEffectId(deathEffectId);
        return door;
    }

}
