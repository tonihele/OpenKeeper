/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared client/server validation for an entire room-building selection.
 * Selections are deliberately all-or-nothing: one invalid tile rejects the
 * whole build.
 */
public final class RoomPlacementValidator {

    public enum Failure {
        NONE,
        INVALID_ROOM,
        OUT_OF_BOUNDS,
        INVALID_TERRAIN,
        BLOCKED_BY_ENTITY,
        BRIDGE_NOT_ANCHORED,
        BRIDGE_NOT_CONNECTED,
        NOT_ENOUGH_GOLD
    }

    public record Result(Failure failure, List<Point> plots) {

        public boolean isValid() {
            return failure == Failure.NONE;
        }
    }

    private RoomPlacementValidator() {
    }

    public static Result validate(IKwdFile kwdFile, IMapInformation<?> mapInformation,
                                  Set<Point> blockedTiles, Vector2f start, Vector2f end, short playerId,
                                  short roomId, int availableGold) {
        Room room = kwdFile.getRoomById(roomId);
        if (!isBuildableRoom(room)) {
            return invalid(Failure.INVALID_ROOM);
        }

        int x1 = (int) Math.min(start.x, end.x);
        int x2 = (int) Math.max(start.x, end.x);
        int y1 = (int) Math.min(start.y, end.y);
        int y2 = (int) Math.max(start.y, end.y);
        if (isOutsideMap(mapInformation, x1, x2, y1, y2)) {
            return invalid(Failure.OUT_OF_BOUNDS);
        }

        List<Point> plots = createPlots(x1, x2, y1, y2);

        Result terrainResult = isBridge(room)
                ? validateBridge(kwdFile, mapInformation, blockedTiles, plots,
                        WorldUtils.vectorToPoint(start), playerId, room)
                : validateLand(mapInformation, blockedTiles, plots, playerId, roomId);
        if (!terrainResult.isValid()) {
            return terrainResult;
        }

        long cost = (long) plots.size() * room.getCost();
        if (cost > availableGold) {
            return invalid(Failure.NOT_ENOUGH_GOLD);
        }

        return new Result(Failure.NONE, Collections.unmodifiableList(plots));
    }

    private static boolean isBuildableRoom(Room room) {
        return room != null && room.getFlags().contains(Room.RoomFlag.BUILDABLE);
    }

    private static boolean isOutsideMap(IMapInformation<?> mapInformation,
            int x1, int x2, int y1, int y2) {
        return x1 < 0 || y1 < 0 || x2 >= mapInformation.getMapData().getWidth()
                || y2 >= mapInformation.getMapData().getHeight();
    }

    private static List<Point> createPlots(int x1, int x2, int y1, int y2) {
        List<Point> plots = new ArrayList<>((x2 - x1 + 1) * (y2 - y1 + 1));
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                plots.add(new Point(x, y));
            }
        }
        return plots;
    }

    private static boolean isBridge(Room room) {
        return room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_WATER)
                || room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAVA);
    }

    private static Result validateLand(IMapInformation<?> mapInformation,
            Set<Point> blockedTiles, List<Point> plots, short playerId, short roomId) {
        for (Point plot : plots) {
            if (!mapInformation.isBuildable(plot, playerId, roomId)) {
                return invalid(Failure.INVALID_TERRAIN);
            }
            if (blockedTiles.contains(plot)) {
                return invalid(Failure.BLOCKED_BY_ENTITY);
            }
        }
        return new Result(Failure.NONE, Collections.unmodifiableList(plots));
    }

    private static Result validateBridge(IKwdFile kwdFile, IMapInformation<?> mapInformation,
            Set<Point> blockedTiles, List<Point> plots, Point start, short playerId, Room room) {
        Set<Point> selected = new HashSet<>(plots);
        ArrayDeque<Point> open = new ArrayDeque<>();
        Set<Point> connected = new HashSet<>();

        for (Point plot : plots) {
            IMapTileInformation tile = mapInformation.getMapData().getTile(plot);
            Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
            boolean compatible = (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_WATER)
                    && terrain.getFlags().contains(Terrain.TerrainFlag.WATER))
                    || (room.getFlags().contains(Room.RoomFlag.PLACEABLE_ON_LAVA)
                    && terrain.getFlags().contains(Terrain.TerrainFlag.LAVA));
            if (!compatible) {
                return invalid(Failure.INVALID_TERRAIN);
            }
            if (blockedTiles.contains(plot)) {
                return invalid(Failure.BLOCKED_BY_ENTITY);
            }
        }

        if (!hasAdjacentOwnedTile(kwdFile, mapInformation, start, playerId)) {
            return invalid(Failure.BRIDGE_NOT_ANCHORED);
        }
        open.add(start);
        connected.add(start);

        while (!open.isEmpty()) {
            Point current = open.removeFirst();
            short terrainId = mapInformation.getMapData().getTile(current).getTerrainId();
            for (Point neighbour : WorldUtils.getSurroundingTiles(mapInformation.getMapData(), current, false)) {
                if (selected.contains(neighbour) && !connected.contains(neighbour)
                        && mapInformation.getMapData().getTile(neighbour).getTerrainId() == terrainId) {
                    connected.add(neighbour);
                    open.addLast(neighbour);
                }
            }
        }

        return connected.size() == plots.size()
                ? new Result(Failure.NONE, Collections.unmodifiableList(plots))
                : invalid(Failure.BRIDGE_NOT_CONNECTED);
    }

    private static boolean hasAdjacentOwnedTile(IKwdFile kwdFile, IMapInformation<?> mapInformation,
            Point point, short playerId) {
        for (Point neighbour : WorldUtils.getSurroundingTiles(mapInformation.getMapData(), point, false)) {
            IMapTileInformation tile = mapInformation.getMapData().getTile(neighbour);
            if (tile == null || tile.getOwnerId() != playerId) {
                continue;
            }
            Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
            if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)
                    && !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                return true;
            }
        }
        return false;
    }

    private static Result invalid(Failure failure) {
        return new Result(failure, Collections.emptyList());
    }
}
