/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.task.worker;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.SOLID;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.TAGGABLE;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;

class DigTileTaskTest {

    private static final short PLAYER_ID = 3;
    private static final Point TILE_LOCATION = new Point(4, 5);

    private Terrain terrain = terrain(SOLID, TAGGABLE);
    private short ownerId;
    private boolean selected = true;

    @Test
    void aSelectedDiggableTileKeepsItsTaskValid() {
        DigTileTask task = createTask();

        assertTrue(task.isValid(null));
    }

    @Test
    void aTileThatTurnsIntoOpenDirtPathInvalidatesItsDigTask() {
        DigTileTask task = createTask();
        assertTrue(task.isValid(null));

        terrain = terrain(TAGGABLE);

        assertFalse(task.isValid(null));
    }

    @Test
    void anOwnedOpenTileIsDiggableOnlyByAnotherPlayer() {
        terrain = terrain(toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.OWNABLE, TAGGABLE);
        ownerId = PLAYER_ID;
        DigTileTask task = createTask();

        assertFalse(task.isValid(null));

        ownerId = (short) (PLAYER_ID + 1);
        assertTrue(task.isValid(null));
    }

    @Test
    void anUnselectedTileIsNotValidWork() {
        selected = false;

        assertFalse(createTask().isValid(null));
    }

    private DigTileTask createTask() {
        IMapTileController tile = (IMapTileController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapTileController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getOwnerId" -> ownerId;
            case "isSelected" -> selected;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getTile" -> tile;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IMapController mapController = (IMapController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            case "getTerrain" -> terrain;
            default -> throw new UnsupportedOperationException(method.getName());
        });

        return new DigTileTask((INavigationService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{INavigationService.class}, (proxy, method, args) -> null), mapController, TILE_LOCATION, PLAYER_ID);
    }

    private static Terrain terrain(Terrain.TerrainFlag... flags) {
        return TerrainFixtures.terrain((short) 1, flags(flags), 0, 100, 100, (short) 1);
    }
}
