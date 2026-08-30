/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.MapTile;
import toniarts.openkeeper.game.component.WoodenBridgeDecay;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.utils.Point;

import java.util.ArrayList;
import java.util.List;

/** Removes wooden bridge tiles after their lava lifetime expires. */
public final class WoodenBridgeDecaySystem implements IGameLogicUpdatable {

    private static final short WOODEN_BRIDGE_ROOM_ID = 8;

    private final EntityData entityData;
    private final EntitySet decayingTiles;
    private final IGameTimer gameTimer;
    private final IGameWorldController gameWorldController;

    public WoodenBridgeDecaySystem(EntityData entityData, IGameTimer gameTimer,
            IGameWorldController gameWorldController, IKwdFile kwdFile,
            IMapController mapController, double lifetime) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;
        this.gameWorldController = gameWorldController;
        initializeExistingBridges(kwdFile, mapController, lifetime);
        decayingTiles = entityData.getEntities(WoodenBridgeDecay.class, MapTile.class);
    }

    private void initializeExistingBridges(IKwdFile kwdFile, IMapController mapController,
            double lifetime) {
        for (int x = 0; x < mapController.getMapData().getWidth(); x++) {
            for (int y = 0; y < mapController.getMapData().getHeight(); y++) {
                IMapTileInformation tile = mapController.getMapData().getTile(x, y);
                Room room = kwdFile.getRoomByTerrain(tile.getTerrainId());
                if (room != null && room.getRoomId() == WOODEN_BRIDGE_ROOM_ID
                        && tile.getBridgeTerrainType() == Tile.BridgeTerrainType.LAVA
                        && entityData.getComponent(tile.getEntityId(), WoodenBridgeDecay.class) == null) {
                    entityData.setComponent(tile.getEntityId(),
                            new WoodenBridgeDecay(gameTimer.getGameTime() + lifetime));
                }
            }
        }
    }

    @Override
    public void processTick(float tpf) {
        decayingTiles.applyChanges();
        List<Point> expired = new ArrayList<>();
        for (Entity entity : decayingTiles) {
            if (entity.get(WoodenBridgeDecay.class).endTime <= gameTimer.getGameTime()) {
                expired.add(entity.get(MapTile.class).p);
                entityData.removeComponent(entity.getId(), WoodenBridgeDecay.class);
            }
        }
        if (!expired.isEmpty()) {
            gameWorldController.destroyRoomTiles(expired);
        }
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        decayingTiles.release();
    }
}
