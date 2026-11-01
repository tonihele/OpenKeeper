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
 * Builds {@link Terrain} fixtures for tests. Lives in {@code Terrain}'s own
 * package because its setters are {@code protected} (parser-only) - there
 * is no other public way to construct one outside the KWD-file loading
 * pipeline.
 */
public final class TerrainFixtures {

    private TerrainFixtures() {
    }

    public static Terrain terrain(short terrainId, EnumSet<Terrain.TerrainFlag> flags, int goldValue,
            int startingHealth, int maxHealth, short maxHealthTypeTerrainId) {
        Terrain terrain = new Terrain();
        terrain.setTerrainId(terrainId);
        terrain.setFlags(flags);
        terrain.setGoldValue(goldValue);
        terrain.setStartingHealth(startingHealth);
        terrain.setMaxHealth(maxHealth);
        terrain.setMaxHealthTypeTerrainId(maxHealthTypeTerrainId);
        return terrain;
    }

    public static EnumSet<Terrain.TerrainFlag> flags(Terrain.TerrainFlag... flags) {
        EnumSet<Terrain.TerrainFlag> set = EnumSet.noneOf(Terrain.TerrainFlag.class);
        for (Terrain.TerrainFlag flag : flags) {
            set.add(flag);
        }
        return set;
    }

}
