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
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;

/**
 * The classifier table test: exercises every row
 * of decision table (in particular Gems -> 5, Gold -> 7, Dirt
 * Path/Mana Vault -> 2, unexplored -> 1) across
 * {explored, perceived, unexplored} x {neutral, owned} x {room, no room}.
 */
class MapColourClassifierTest {

    private static final short KEEPER = Player.KEEPER1_ID; // 3
    private static final short OTHER_KEEPER = Player.KEEPER2_ID; // 4

    private static final short ROCK_ID = 1;
    private static final short GEMS_ID = 2;
    private static final short GOLD_ID = 3;
    private static final short DIGGABLE_ROCK_ID = 4;
    private static final short WATER_ID = 5;
    private static final short LAVA_ID = 6;
    private static final short DIRT_PATH_ID = 7;
    private static final short CLAIMED_PATH_ID = 8;
    private static final short REVEALED_GOLD_ID = 9;

    private FakeMapData mapData;
    private FakeMapInformation mapInformation;
    private FakeFogOfWarInformation fog;
    private FakeRoomsInformation rooms;
    private MapColourClassifier classifier;

    private void configureMap(int width, int height) {
        mapData = new FakeMapData(width, height);

        Map<Short, Terrain> terrains = new HashMap<>();
        terrains.put(ROCK_ID, TerrainFixtures.terrain(ROCK_ID,
                flags(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.IMPENETRABLE), 0, 0, 0, ROCK_ID));
        terrains.put(GEMS_ID, TerrainFixtures.terrain(GEMS_ID,
                flags(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.IMPENETRABLE), 100, 0, 0, GEMS_ID));
        terrains.put(GOLD_ID, TerrainFixtures.terrain(GOLD_ID,
                flags(Terrain.TerrainFlag.SOLID), 100, 0, 0, GOLD_ID));
        terrains.put(DIGGABLE_ROCK_ID, TerrainFixtures.terrain(DIGGABLE_ROCK_ID,
                flags(Terrain.TerrainFlag.SOLID), 0, 0, 0, DIGGABLE_ROCK_ID));
        terrains.put(WATER_ID, TerrainFixtures.terrain(WATER_ID,
                flags(Terrain.TerrainFlag.WATER), 0, 0, 0, WATER_ID));
        terrains.put(LAVA_ID, TerrainFixtures.terrain(LAVA_ID,
                flags(Terrain.TerrainFlag.LAVA), 0, 0, 0, LAVA_ID));
        // Dirt Path: non-ownable, heals up to a different terrain - claimable.
        terrains.put(DIRT_PATH_ID, TerrainFixtures.terrain(DIRT_PATH_ID,
                flags(), 0, 1, 10, CLAIMED_PATH_ID));
        // Claimed Path: ownable - never claimable regardless of health fields.
        terrains.put(CLAIMED_PATH_ID, TerrainFixtures.terrain(CLAIMED_PATH_ID,
                flags(Terrain.TerrainFlag.OWNABLE), 0, 10, 10, CLAIMED_PATH_ID));
        terrains.put(REVEALED_GOLD_ID, TerrainFixtures.terrain(REVEALED_GOLD_ID,
                flags(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.REVEAL_THROUGH_FOG_OF_WAR), 100, 0, 0, REVEALED_GOLD_ID));

        mapInformation = new FakeMapInformation(mapData, terrains);
        fog = new FakeFogOfWarInformation();
        rooms = new FakeRoomsInformation();
        classifier = new MapColourClassifier(mapInformation, fog, rooms);
    }

    private void explore(int x, int y) {
        fog.explored.add(new Point(x, y));
    }

    private void perceive(int x, int y) {
        fog.perceived.add(new Point(x, y));
    }

    private void setTerrain(int x, int y, short terrainId) {
        mapData.getTile(x, y).terrainId = terrainId;
    }

    private void setOwner(int x, int y, short ownerId) {
        mapData.getTile(x, y).ownerId = ownerId;
    }

    private EntityId putRoom(int x, int y, short roomTypeId, short ownerId, boolean dungeonHeart) {
        EntityId entityId = new EntityId(x * 1000L + y);
        FakeRoomInformation room = new FakeRoomInformation(entityId);
        room.roomId = roomTypeId;
        room.ownerId = ownerId;
        room.dungeonHeart = dungeonHeart;
        rooms.rooms.put(entityId, room);
        mapData.getTile(x, y).roomId = entityId;
        return entityId;
    }

    @Test
    void unexploredAndUnperceivedTileIsClassOne() {
        configureMap(3, 3);
        setTerrain(1, 1, GOLD_ID);

        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, classifier.classify(1, 1));
    }

    @Test
    void outOfBoundsTileIsClassOne() {
        configureMap(3, 3);

        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, classifier.classify(-1, 0));
        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, classifier.classify(0, 99));
    }

    @Test
    void perceivedWithoutRevealThroughFogTerrainStaysUnexplored() {
        configureMap(3, 3);
        setTerrain(1, 1, GOLD_ID); // no REVEAL_THROUGH_FOG_OF_WAR
        perceive(1, 1);

        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, classifier.classify(1, 1));
    }

    @Test
    void perceivedWithRevealThroughFogTerrainShowsItsRealClass() {
        configureMap(3, 3);
        setTerrain(1, 1, REVEALED_GOLD_ID);
        perceive(1, 1);

        assertEquals(MapColourClass.GOLD, classifier.classify(1, 1));
    }

    @Test
    void gemsAreSolidGoldValueImpenetrable() {
        configureMap(3, 3);
        setTerrain(1, 1, GEMS_ID);
        explore(1, 1);

        assertEquals(MapColourClass.GEMS, classifier.classify(1, 1));
    }

    @Test
    void goldIsSolidGoldValueNotImpenetrable() {
        configureMap(3, 3);
        setTerrain(1, 1, GOLD_ID);
        explore(1, 1);

        assertEquals(MapColourClass.GOLD, classifier.classify(1, 1));
    }

    @Test
    void impenetrableRockWithNoGoldValueIsClassOne() {
        configureMap(3, 3);
        setTerrain(1, 1, ROCK_ID);
        explore(1, 1);

        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, classifier.classify(1, 1));
    }

    @Test
    void diggableRockIsSolidNoGoldNotImpenetrable() {
        configureMap(3, 3);
        setTerrain(1, 1, DIGGABLE_ROCK_ID);
        explore(1, 1);

        assertEquals(MapColourClass.DIGGABLE_ROCK, classifier.classify(1, 1));
    }

    @Test
    void water() {
        configureMap(3, 3);
        setTerrain(1, 1, WATER_ID);
        explore(1, 1);

        assertEquals(MapColourClass.WATER, classifier.classify(1, 1));
    }

    @Test
    void lava() {
        configureMap(3, 3);
        setTerrain(1, 1, LAVA_ID);
        explore(1, 1);

        assertEquals(MapColourClass.LAVA, classifier.classify(1, 1));
    }

    @Test
    void dirtPathIsClaimableFloor() {
        configureMap(3, 3);
        setTerrain(1, 1, DIRT_PATH_ID);
        explore(1, 1);

        assertEquals(MapColourClass.CLAIMABLE_FLOOR, classifier.classify(1, 1));
    }

    @Test
    void neutralOwnedNonClaimableNonRoomFloorIsTheMagentaSentinel() {
        configureMap(3, 3);
        setTerrain(1, 1, CLAIMED_PATH_ID);
        setOwner(1, 1, Player.NEUTRAL_PLAYER_ID);
        explore(1, 1);

        assertEquals(MapColourClass.NEUTRAL_OWNED_SENTINEL, classifier.classify(1, 1));
    }

    @Test
    void ownedSolidTileUsesTheOwnedSolidBase() {
        configureMap(3, 3);
        setTerrain(1, 1, DIGGABLE_ROCK_ID); // solid
        setOwner(1, 1, KEEPER);
        explore(1, 1);

        assertEquals((short) (MapColourClass.OWNED_SOLID_BASE + KEEPER), classifier.classify(1, 1));
    }

    @Test
    void ownedFloorTileUsesTheOwnedFloorBase() {
        configureMap(3, 3);
        setTerrain(1, 1, CLAIMED_PATH_ID); // not solid
        setOwner(1, 1, KEEPER);
        explore(1, 1);

        assertEquals((short) (MapColourClass.OWNED_FLOOR_BASE + KEEPER), classifier.classify(1, 1));
    }

    @Test
    void neutralOwnedRoomTileUsesTheRoomOwnersFloorBase() {
        configureMap(3, 3);
        setTerrain(1, 1, CLAIMED_PATH_ID);
        setOwner(1, 1, Player.NEUTRAL_PLAYER_ID);
        putRoom(1, 1, (short) 1, OTHER_KEEPER, false);
        explore(1, 1);

        assertEquals((short) (MapColourClass.OWNED_FLOOR_BASE + OTHER_KEEPER), classifier.classify(1, 1));
    }

    @Test
    void dungeonHeartRoomTileWinsOverPlainOwnerCheck() {
        configureMap(3, 3);
        setTerrain(1, 1, CLAIMED_PATH_ID);
        setOwner(1, 1, KEEPER);
        putRoom(1, 1, (short) 5, KEEPER, true);
        explore(1, 1);

        assertEquals((short) (MapColourClass.DUNGEON_HEART_BASE + KEEPER), classifier.classify(1, 1));
    }

    @Test
    void dungeonHeartUsesTheRoomsOwnerNotJustTheTileOwner() {
        configureMap(3, 3);
        setTerrain(1, 1, CLAIMED_PATH_ID);
        setOwner(1, 1, KEEPER);
        putRoom(1, 1, (short) 5, OTHER_KEEPER, true);
        explore(1, 1);

        assertEquals((short) (MapColourClass.DUNGEON_HEART_BASE + OTHER_KEEPER), classifier.classify(1, 1));
    }

    @Test
    void removedRoomIsIgnoredEntirely() {
        configureMap(3, 3);
        setTerrain(1, 1, CLAIMED_PATH_ID);
        setOwner(1, 1, Player.NEUTRAL_PLAYER_ID);
        EntityId roomInstance = putRoom(1, 1, (short) 5, OTHER_KEEPER, true);
        rooms.rooms.get(roomInstance).removed = true;
        explore(1, 1);

        // Falls through to the plain neutral/no-room/non-claimable case.
        assertEquals(MapColourClass.NEUTRAL_OWNED_SENTINEL, classifier.classify(1, 1));
    }

}
