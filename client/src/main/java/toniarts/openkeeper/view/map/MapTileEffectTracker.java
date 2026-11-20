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
package toniarts.openkeeper.view.map;

import com.jme3.scene.Node;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/** Remembers the last rendered tile state so one-shot transition effects play only once. */
public final class MapTileEffectTracker {
    private static final int ROOM_CLAIM_EFFECT_ID = 8;
    private final short[][] terrainIds;
    private final short[][] owners;
    private final IMapDataInformation map;
    private final IKwdFile kwdFile;
    private final IFogOfWarInformation fog;
    private final EffectManagerState effects;
    private final Node parent;

    public MapTileEffectTracker(IMapDataInformation map, IKwdFile kwdFile,
            IFogOfWarInformation fog, EffectManagerState effects, Node parent) {
        this.map = map;
        this.kwdFile = kwdFile;
        this.fog = fog;
        this.effects = effects;
        this.parent = parent;
        terrainIds = new short[map.getWidth()][map.getHeight()];
        owners = new short[map.getWidth()][map.getHeight()];
        // Joining a game or loading a level must not replay claims on already-owned rooms.
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                IMapTileInformation tile = map.getTile(x, y);
                terrainIds[x][y] = tile.getTerrainId();
                owners[x][y] = tile.getOwnerId();
            }
        }
    }

    public void update(Point... points) {
        for (Point point : points) {
            IMapTileInformation tile = map.getTile(point);
            if (tile == null) {
                continue;
            }
            short terrainId = tile.getTerrainId();
            short oldTerrainId = terrainIds[point.x][point.y];
            short owner = tile.getOwnerId();
            short oldOwner = owners[point.x][point.y];
            terrainIds[point.x][point.y] = terrainId;
            owners[point.x][point.y] = owner;

            if (terrainId != oldTerrainId) {
                Terrain oldTerrain = kwdFile.getTerrain(oldTerrainId);
                if (terrainId == oldTerrain.getMaxHealthTypeTerrainId()) {
                    spawn(point, oldTerrain.getMaxHealthEffectId(), owner);
                } else if (terrainId == oldTerrain.getDestroyedTypeTerrainId()) {
                    spawn(point, oldTerrain.getDestroyedEffectId(), Player.NEUTRAL_PLAYER_ID);
                }
            } else if (owner != oldOwner && owner != 0 && owner != Player.NEUTRAL_PLAYER_ID
                    && kwdFile.getTerrain(terrainId).getFlags().contains(Terrain.TerrainFlag.ROOM)
                    && fog.isExplored(point)) {
                // Claiming/capturing keeps the room terrain but changes every tile's owner.
                // Spawn the authored burst once per tile, with the incoming keeper's colour.
                spawn(point, ROOM_CLAIM_EFFECT_ID, owner);
            }
        }
    }

    private void spawn(Point point, int effectId, short owner) {
        effects.load(parent, WorldUtils.pointToVector3f(point).addLocal(0, WorldUtils.FLOOR_HEIGHT, 0),
                effectId, false, owner);
    }
}
