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
package toniarts.openkeeper.game.map;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.MapTile;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Path finding asks a tile for its location for every neighbour it looks at
 */
class MapTileControllerTest {

    @Test
    void locationIsReadWithoutLookingUpTheMapTileComponent() {
        EntityData realEntityData = new DefaultEntityData();
        EntityId entityId = realEntityData.createEntity();
        realEntityData.setComponent(entityId, new MapTile(0, (short) 1, null, new Point(3, 4), 43));
        AtomicInteger mapTileLookups = new AtomicInteger();
        EntityData entityData = (EntityData) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityData.class},
                (proxy, method, args) -> {
                    if ("getComponent".equals(method.getName()) && args[1] == MapTile.class) {
                        mapTileLookups.incrementAndGet();
                    }
                    return method.invoke(realEntityData, args);
                });
        MapTileController tile = new MapTileController(entityId, entityData);
        mapTileLookups.set(0);

        assertEquals(new Point(3, 4), tile.getLocation());
        assertEquals(3, tile.getX());
        assertEquals(4, tile.getY());
        assertEquals(43, tile.getIndex());
        assertEquals(0, mapTileLookups.get());

        tile.setPoint(new Point(5, 6));
        tile.setIndex(65);

        assertEquals(new Point(5, 6), tile.getLocation());
        assertEquals(65, tile.getIndex());
        assertEquals(new Point(5, 6), realEntityData.getComponent(entityId, MapTile.class).p);
    }
}
