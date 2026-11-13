/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.tools.convert.map;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.VisualEffect;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import toniarts.openkeeper.view.map.RoomFlameControl;
import static org.junit.jupiter.api.Assertions.*;

class RoomFlameControlTest {
    @Test
    void ownedFlaggedRoomEmitsTwicePerSecondWithOwnerAndNativeTravelSpeed() {
        var f = new Fixture(IFogOfWarInformation.ALWAYS_VISIBLE);
        f.node.updateLogicalState(2.5f);
        assertEquals(5, f.effects.positions.size());
        assertEquals(List.of((short) 3, (short) 3, (short) 3, (short) 3, (short) 3), f.effects.owners);
        // The ninth room tick has advanced from the west edge onto the north edge.
        assertEquals(new Vector3f(10.2578125f, 1, 19.625f), f.effects.positions.getLast());
    }

    @Test
    void emissionDoesNotDependOnRenderFrameRate() {
        var a = new Fixture(IFogOfWarInformation.ALWAYS_VISIBLE);
        var b = new Fixture(IFogOfWarInformation.ALWAYS_VISIBLE);
        a.node.updateLogicalState(0.5f);
        for (int i = 0; i < 50; i++) b.node.updateLogicalState(0.01f);
        assertEquals(a.effects.positions, b.effects.positions);
    }

    @Test
    void ownerlessAndUnflaggedRoomsDoNotEmit() {
        var f = new Fixture(IFogOfWarInformation.ALWAYS_VISIBLE);
        f.room.setOwnerId((short) 0);
        f.node.updateLogicalState(0.2f);
        f.room.setOwnerId((short) 3);
        f.room.getRoom().setFlags(EnumSet.noneOf(Room.RoomFlag.class));
        f.node.updateLogicalState(0.2f);
        assertTrue(f.effects.positions.isEmpty());
    }

    @Test
    void neutralRoomEmitsBeforeClaimAndKeepsItsPhaseWhenClaimed() {
        var f = new Fixture(IFogOfWarInformation.ALWAYS_VISIBLE);
        f.room.setOwnerId(Player.NEUTRAL_PLAYER_ID);
        Node root = new Node();
        var manager = new toniarts.openkeeper.view.map.RoomFlameManager(root, f.effects,
                IFogOfWarInformation.ALWAYS_VISIBLE);
        manager.add(f.room);
        root.updateLogicalState(0.5f);
        Node originalTrail = (Node) root.getChild(0);
        assertEquals(List.of(Player.NEUTRAL_PLAYER_ID), f.effects.owners);
        Vector3f firstPosition = f.effects.positions.getFirst();

        var claimed = new RoomInstance(f.room.getRoom());
        claimed.setOwnerId(Player.KEEPER1_ID);
        claimed.addCoordinates(f.room.getCoordinates());
        manager.add(claimed);
        manager.retainRooms(List.of(claimed));
        assertSame(originalTrail, root.getChild(0));
        assertFalse(originalTrail.getChildren().isEmpty());
        root.updateLogicalState(0.5f);
        assertEquals(List.of(Player.NEUTRAL_PLAYER_ID, Player.KEEPER1_ID), f.effects.owners);
        assertNotEquals(firstPosition, f.effects.positions.getLast());
    }

    @Test
    void unexploredRoomsStayHidden() {
        var hidden = (IFogOfWarInformation) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IFogOfWarInformation.class}, (proxy, method, args) -> false);
        var f = new Fixture(hidden);
        f.node.updateLogicalState(0.5f);
        assertTrue(f.effects.positions.isEmpty());
    }

    @Test
    void removingOrDestroyingRoomRemovesItsRemainingTrail() {
        var f = new Fixture(IFogOfWarInformation.ALWAYS_VISIBLE);
        f.node.updateLogicalState(0.5f);
        assertFalse(f.node.getChildren().isEmpty());
        f.control.clear();
        assertTrue(f.node.getChildren().isEmpty());
        f.node.updateLogicalState(0.5f);
        f.room.setDestroyed(true);
        f.node.updateLogicalState(0.2f);
        assertTrue(f.node.getChildren().isEmpty());
        f.effects.update(0.1f);
        assertTrue(f.node.getChildren().isEmpty());
    }

    @Test
    void roomViewsKeepTheirOwnerAndDoNotMergeAcrossOwnerBoundaries() throws Exception {
        var definition = new Room();
        definition.setName("synthetic room");
        definition.setFlags(EnumSet.of(Room.RoomFlag.HAS_FLAME));
        definition.setTileConstruction(Room.TileConstruction.COMPLETE);
        var floor = new Terrain();
        floor.setFlags(EnumSet.of(Terrain.TerrainFlag.ROOM));
        var outside = new Terrain();
        outside.setFlags(EnumSet.noneOf(Terrain.TerrainFlag.class));
        var kwd = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getTerrain" -> ((Short) args[0]) == 1 ? floor : outside;
                    case "getRoomByTerrain" -> definition;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var data = (toniarts.openkeeper.game.map.IMapDataInformation) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{toniarts.openkeeper.game.map.IMapDataInformation.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("getTile")) throw new UnsupportedOperationException(method.getName());
                    Point p = args.length == 1 ? (Point) args[0] : new Point((Integer) args[0], (Integer) args[1]);
                    boolean roomTile = p.y == 1 && (p.x == 1 || p.x == 2);
                    return Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[]{toniarts.openkeeper.game.map.IMapTileInformation.class},
                            (t, m, a) -> switch (m.getName()) {
                                case "getTerrainId" -> (short) (roomTile ? 1 : 2);
                                case "getOwnerId" -> (short) (p.x == 1 ? 3 : 4);
                                case "getLocation" -> p;
                                default -> throw new UnsupportedOperationException(m.getName());
                            });
                });
        var information = (toniarts.openkeeper.game.map.IMapInformation) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{toniarts.openkeeper.game.map.IMapInformation.class},
                (proxy, method, args) -> data);
        var view = new toniarts.openkeeper.view.map.MapViewController(null, kwd, information, (short) 3, false, false) {
            @Override protected void updateProgress(float progress) { }
        };
        var find = toniarts.openkeeper.view.map.MapViewController.class.getDeclaredMethod(
                "handleRoom", Point.class, Room.class, Thing.Room.class);
        find.setAccessible(true);
        var red = (RoomInstance) find.invoke(view, new Point(1, 1), definition, null);
        var blue = (RoomInstance) find.invoke(view, new Point(2, 1), definition, null);
        assertEquals(3, red.getOwnerId());
        assertEquals(4, blue.getOwnerId());
        assertEquals(List.of(new Point(1, 1)), red.getCoordinates());
        assertEquals(List.of(new Point(2, 1)), blue.getCoordinates());
    }

    @Test
    void rebuildingUnchangedRoomKeepsTheRunningTrailAndItsPhase() {
        var f = new Fixture(IFogOfWarInformation.ALWAYS_VISIBLE);
        Node root = new Node();
        var manager = new toniarts.openkeeper.view.map.RoomFlameManager(root, f.effects,
                IFogOfWarInformation.ALWAYS_VISIBLE);
        manager.add(f.room);
        root.updateLogicalState(0.5f);
        Node originalTrail = (Node) root.getChild(0);
        Vector3f firstPosition = f.effects.positions.getFirst();

        var rebuilt = new RoomInstance(f.room.getRoom());
        rebuilt.setOwnerId(f.room.getOwnerId());
        rebuilt.addCoordinates(f.room.getCoordinates());
        manager.add(rebuilt);
        manager.retainRooms(List.of(rebuilt));
        assertEquals(1, root.getQuantity());
        assertSame(originalTrail, root.getChild(0));
        assertFalse(originalTrail.getChildren().isEmpty());
        root.updateLogicalState(0.5f);
        assertEquals(2, f.effects.positions.size());
        assertNotEquals(firstPosition, f.effects.positions.getLast());

        manager.retainRooms(List.of());
        assertEquals(0, root.getQuantity());
        assertTrue(originalTrail.getChildren().isEmpty());
        f.effects.update(0.1f);
        assertTrue(originalTrail.getChildren().isEmpty());
    }

    private static final class Fixture {
        final RoomInstance room;
        final Node node = new Node();
        final RecordingEffects effects = new RecordingEffects();
        final RoomFlameControl control;
        Fixture(IFogOfWarInformation fog) {
            var definition = new Room();
            definition.setFlags(EnumSet.of(Room.RoomFlag.HAS_FLAME));
            room = new RoomInstance(definition);
            room.setOwnerId((short) 3);
            room.addCoordinate(new Point(10, 20));
            control = new RoomFlameControl(room, effects, fog);
            node.addControl(control);
        }
    }

    private static final class RecordingEffects extends EffectManagerState {
        final List<Vector3f> positions = new ArrayList<>();
        final List<Short> owners = new ArrayList<>();
        RecordingEffects() { super(data(), null); }
        @Override
        public VisualEffect load(Node parent, Vector3f position, int id, boolean infinite, short owner) {
            assertEquals(20, id);
            assertFalse(infinite);
            positions.add(position.clone());
            owners.add(owner);
            var effect = super.load(parent, position, id, infinite, owner);
            effect.getEffectNode().attachChild(new Node("synthetic surviving particle"));
            return effect;
        }
        private static IKwdFile data() {
            var effect = new Effect();
            effect.setName("synthetic room flame");
            effect.setFlags(EnumSet.of(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS));
            effect.setGenerateIds(List.of());
            effect.setDeathEffectId(20);
            return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                    new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getEffect")) return effect;
                        throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
