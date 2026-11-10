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

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.MapTile;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.tools.convert.map.GameMap;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The map view doesn't update its tile container until the map has loaded.
 * Tile changes made before then must reach the view at its first update, and
 * none may be lost.
 */
class MapTileContainerTest {

    private static final int SIZE = 3;

    private final List<Point> updatedTiles = new ArrayList<>();
    private final EntityId[][] tiles = new EntityId[SIZE][SIZE];
    private DefaultEntityData entityData;
    private MapTileContainer container;
    private boolean loaded;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getMap")) {
                        return new GameMap(SIZE, SIZE);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        container = new MapTileContainer(entityData, kwdFile, points -> updatedTiles.addAll(Arrays.asList(points))) {

            @Override
            protected void onLoadComplete() {
                loaded = true;
            }
        };
    }

    @AfterEach
    void tearDown() {
        container.stop();
        entityData.close();
    }

    @Test
    void tileChangesBeforeTheFirstUpdateAreAppliedThen() {
        createTiles(SIZE);
        container.start();
        assertTrue(loaded, "In a local game the whole map loads on start");

        // Changed while the map is still being built
        entityData.setComponent(tiles[1][1], new Owner((short) 3, (short) 3));
        entityData.setComponent(tiles[2][0], new Health(50, 100));
        assertTrue(updatedTiles.isEmpty());

        container.update();

        assertEquals(Set.of(new Point(1, 1), new Point(2, 0)), Set.copyOf(updatedTiles));
    }

    @Test
    void tilesComingInBatchesLoadOnUpdate() {
        createTiles(1);
        container.start();
        assertFalse(loaded);

        createTiles(SIZE);
        container.update();

        assertTrue(loaded);
    }

    private void createTiles(int columns) {
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (tiles[x][y] != null) {
                    continue;
                }
                EntityId id = entityData.createEntity();
                entityData.setComponents(id,
                        new MapTile(0, (short) 1, null, new Point(x, y), y * SIZE + x),
                        new Owner((short) 0, (short) 0),
                        new Health(100, 100),
                        new Gold(0, 0),
                        new Mana(0));
                tiles[x][y] = id;
            }
        }
    }

}
