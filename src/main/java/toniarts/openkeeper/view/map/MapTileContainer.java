/*
 * Copyright (C) 2014-2024 OpenKeeper
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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.MapTile;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.map.AbstractMapTileInformation;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;

/**
 * Contains the map tiles
 */
public abstract class MapTileContainer extends EntityContainer<IMapTileInformation> implements IMapDataInformation<IMapTileInformation> {

    private static final System.Logger logger = System.getLogger(MapTileContainer.class.getName());

    private final int width;
    private final int height;
    private final IMapTileInformation[][] tiles;
    private final Consumer<Point[]> tileUpdateCallback;
    private int tilesAdded = 0;

    protected MapTileContainer(EntityData entityData, IKwdFile kwdFile, Consumer<Point[]> tileUpdateCallback) {
        super(entityData, MapTile.class, Owner.class, Health.class, Gold.class, Mana.class);

        this.tileUpdateCallback = tileUpdateCallback;
        width = kwdFile.getMap().getWidth();
        height = kwdFile.getMap().getHeight();

        // Duplicate the map
        this.tiles = new IMapTileInformation[width][height];
    }

    @Override
    protected IMapTileInformation addObject(Entity e) {
        logger.log(Level.TRACE, "MapTileContainer.addObject({0})", e);
        IMapTileInformation result = new MapTileInformation(e);
        Point p = result.getLocation();
        this.tiles[p.x][p.y] = result;

        // Naive completion checker
        tilesAdded++;
        if (tilesAdded == getSize()) {
            onLoadComplete();
        }

        return result;
    }

    @Override
    protected void updateObjects(Set<Entity> set) {
        if (set.isEmpty()) {
            return;
        }

        logger.log(System.Logger.Level.TRACE, "MapTileContainer.updateObjects({0})", set.size());

        // Collect the tiles
        Point[] updatableTiles = new Point[set.size()];
        int i = 0;
        for (Entity e : set) {
            IMapTileInformation object = getObject(e.getId());
            if (object == null) {
                logger.log(Level.WARNING, "Update: No matching object for entity:{0}", e);
                continue;
            }
            updatableTiles[i] = object.getLocation();
            i++;
        }

        // Update the batch
        tileUpdateCallback.accept(updatableTiles);
    }

    @Override
    protected void updateObject(IMapTileInformation object, Entity e) {
        throw new UnsupportedOperationException("Should use the batch method.");
    }

    @Override
    protected void removeObject(IMapTileInformation object, Entity e) {
        logger.log(Level.TRACE, "MapTileContainer.removeObject({0})", e);
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public IMapTileInformation getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }

        return this.tiles[x][y];
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setTiles(List<IMapTileInformation> mapTiles) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    protected abstract void onLoadComplete();

    /**
     * Single map tile that taps into the entity information
     */
    private static class MapTileInformation extends AbstractMapTileInformation {

        private final Entity entity;

        public MapTileInformation(Entity entity) {
            super(entity.getId());

            this.entity = entity;
        }

        @Override
        protected <T extends EntityComponent> T getEntityComponent(Class<T> type) {
            return entity.get(type);
        }

    }

}
