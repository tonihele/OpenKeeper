/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.map;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.utils.Point;

/**
 * A minimal, mutable {@link IMapTileInformation} test double. Public: also
 * reused by {@code view.minimap}'s rendering tests.
 */
public final class FakeMapTile implements IMapTileInformation {

    private final Point location;
    public short terrainId;
    public short ownerId;
    public EntityId roomId;

    public FakeMapTile(Point location) {
        this.location = location;
    }

    @Override
    public EntityId getEntityId() {
        return null;
    }

    @Override
    public Tile.BridgeTerrainType getBridgeTerrainType() {
        return null;
    }

    @Override
    public int getGold() {
        return 0;
    }

    @Override
    public int getHealth() {
        return 0;
    }

    @Override
    public Integer getHealthPercent() {
        return null;
    }

    @Override
    public int getIndex() {
        return 0;
    }

    @Override
    public Point getLocation() {
        return location;
    }

    @Override
    public int getManaGain() {
        return 0;
    }

    @Override
    public int getMaxHealth() {
        return 0;
    }

    @Override
    public short getOwnerId() {
        return ownerId;
    }

    @Override
    public int getRandomTextureIndex() {
        return 0;
    }

    @Override
    public short getTerrainId() {
        return terrainId;
    }

    @Override
    public int getX() {
        return location.x;
    }

    @Override
    public int getY() {
        return location.y;
    }

    @Override
    public boolean isAtFullHealth() {
        return true;
    }

    @Override
    public boolean isFlashed(short playerId) {
        return false;
    }

    @Override
    public boolean isSelected(short playerId) {
        return false;
    }

    @Override
    public EntityId getRoomId() {
        return roomId;
    }

}
