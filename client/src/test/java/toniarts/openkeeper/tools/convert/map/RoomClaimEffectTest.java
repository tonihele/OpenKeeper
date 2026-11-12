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

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.VisualEffect;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import toniarts.openkeeper.view.map.MapTileEffectTracker;
import static org.junit.jupiter.api.Assertions.*;

class RoomClaimEffectTest {
    @Test
    void claimingThreeByThreeRoomSpawnsNineIncomingKeeperColouredBurstsOnce() {
        var f = new Fixture(Player.NEUTRAL_PLAYER_ID);
        f.claimAll(Player.KEEPER1_ID);
        f.tracker.update(f.points());
        assertEquals(9, f.effects.spawns.size());
        for (int i = 0; i < 9; i++) {
            Point p = f.points()[i];
            assertEquals(new Spawn(8, Player.KEEPER1_ID, new Vector3f(p.x, 1, p.y)), f.effects.spawns.get(i));
        }
        f.tracker.update(f.points());
        assertEquals(9, f.effects.spawns.size());
    }

    @Test
    void enemyCaptureUsesTheNewOwnersColour() {
        var f = new Fixture(Player.KEEPER1_ID);
        f.owners[1][1] = Player.KEEPER2_ID;
        f.tracker.update(new Point(1, 1), new Point(1, 1));
        assertEquals(List.of(new Spawn(8, Player.KEEPER2_ID, new Vector3f(1, 1, 1))), f.effects.spawns);
    }

    @Test
    void loadingAlreadyOwnedRoomAndOrdinaryRefreshesDoNotReplayClaims() {
        var f = new Fixture(Player.KEEPER1_ID);
        assertTrue(f.effects.spawns.isEmpty());
        f.tracker.update(f.points());
        assertTrue(f.effects.spawns.isEmpty());
    }

    @Test
    void hiddenCaptureIsNotReplayedWhenItsTileIsLaterExplored() {
        var f = new Fixture(Player.NEUTRAL_PLAYER_ID);
        f.explored = false;
        f.claimAll(Player.KEEPER1_ID);
        f.tracker.update(f.points());
        f.explored = true;
        f.tracker.update(f.points());
        assertTrue(f.effects.spawns.isEmpty());
    }

    @Test
    void buildingAndSellingRoomsDoNotCountAsCaptures() {
        var f = new Fixture(Player.NEUTRAL_PLAYER_ID);
        f.terrains[0][0] = 20;
        f.tracker.update(new Point(0, 0));
        f.terrains[0][0] = 10;
        f.owners[0][0] = Player.KEEPER1_ID;
        f.tracker.update(new Point(0, 0));
        f.terrains[0][0] = 20;
        f.tracker.update(new Point(0, 0));
        assertTrue(f.effects.spawns.isEmpty());
    }

    @Test
    void floorClaimsAndReturningRoomsToNeutralDoNotPlayRoomBursts() {
        var f = new Fixture(Player.KEEPER1_ID);
        f.terrains[0][0] = 20;
        f.tracker.update(new Point(0, 0));
        f.owners[0][0] = Player.KEEPER2_ID;
        f.owners[1][1] = Player.NEUTRAL_PLAYER_ID;
        f.tracker.update(new Point(0, 0), new Point(1, 1));
        assertTrue(f.effects.spawns.isEmpty());
    }

    @Test
    void terrainRepairAndDestructionEffectsKeepTheirExistingBehaviour() {
        var f = new Fixture(Player.KEEPER1_ID);
        f.terrains[0][0] = 30;
        f.owners[0][0] = Player.KEEPER2_ID;
        f.tracker.update(new Point(0, 0));
        assertEquals(List.of(new Spawn(1001, Player.KEEPER2_ID, new Vector3f(0, 1, 0))), f.effects.spawns);
        f.terrains[0][1] = 40;
        f.tracker.update(new Point(0, 1));
        assertEquals(new Spawn(1002, Player.NEUTRAL_PLAYER_ID, new Vector3f(0, 1, 1)), f.effects.spawns.getLast());
    }

    private record Spawn(int id, short owner, Vector3f position) { }

    private static final class RecordingEffects extends EffectManagerState {
        final List<Spawn> spawns = new ArrayList<>();
        RecordingEffects() { super(null, null); }
        @Override
        public VisualEffect load(Node parent, Vector3f position, int id, boolean infinite, short owner) {
            assertFalse(infinite);
            if (id != 0) spawns.add(new Spawn(id, owner, position.clone()));
            return null;
        }
    }

    private static final class Fixture {
        final short[][] terrains = new short[3][3];
        final short[][] owners = new short[3][3];
        boolean explored = true;
        final RecordingEffects effects = new RecordingEffects();
        final MapTileEffectTracker tracker;

        Fixture(short owner) {
            Terrain room = terrain((short) 10, true);
            room.setMaxHealthTypeTerrainId((short) 30);
            room.setMaxHealthEffectId(1001);
            room.setDestroyedTypeTerrainId((short) 40);
            room.setDestroyedEffectId(1002);
            Map<Short, Terrain> definitions = Map.of((short) 10, room, (short) 20, terrain((short) 20, false),
                    (short) 30, terrain((short) 30, false), (short) 40, terrain((short) 40, false));
            IKwdFile kwd = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("getTerrain")) return definitions.get((Short) args[0]);
                        throw new UnsupportedOperationException(method.getName());
                    });
            IMapTileInformation[][] tiles = new IMapTileInformation[3][3];
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    final int tx = x, ty = y;
                    terrains[x][y] = 10;
                    owners[x][y] = owner;
                    tiles[x][y] = (IMapTileInformation) Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                                case "getTerrainId" -> terrains[tx][ty];
                                case "getOwnerId" -> owners[tx][ty];
                                case "getLocation" -> new Point(tx, ty);
                                default -> throw new UnsupportedOperationException(method.getName());
                            });
                }
            }
            IMapDataInformation map = (IMapDataInformation) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getWidth", "getHeight" -> 3;
                        case "getTile" -> {
                            Point p = args.length == 1 ? (Point) args[0] : new Point((Integer) args[0], (Integer) args[1]);
                            yield tiles[p.x][p.y];
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            IFogOfWarInformation fog = (IFogOfWarInformation) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{IFogOfWarInformation.class}, (proxy, method, args) -> explored);
            tracker = new MapTileEffectTracker(map, kwd, fog, effects, new Node());
        }

        void claimAll(short owner) {
            for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) owners[x][y] = owner;
        }

        Point[] points() {
            var points = new ArrayList<Point>();
            for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) points.add(new Point(x, y));
            return points.toArray(Point[]::new);
        }

        static Terrain terrain(short id, boolean isRoom) {
            var terrain = new Terrain();
            terrain.setTerrainId(id);
            terrain.setFlags(isRoom ? EnumSet.of(Terrain.TerrainFlag.ROOM) : EnumSet.noneOf(Terrain.TerrainFlag.class));
            return terrain;
        }
    }
}
