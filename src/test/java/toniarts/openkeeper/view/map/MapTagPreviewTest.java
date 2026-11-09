package toniarts.openkeeper.view.map;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import static org.junit.jupiter.api.Assertions.*;

class MapTagPreviewTest {
    @Test
    void pressTintsOnlyEligibleTerrainAndCancellationRestoresItsMaterial() throws Exception {
        var assets = new DesktopAssetManager(true);
        Point a = new Point(1, 1), b = new Point(2, 1), floor = new Point(3, 1);
        IMapTileInformation tile = proxy(IMapTileInformation.class, (p, m, args) -> switch (m.getName()) {
            case "getLocation" -> a;
            case "getTerrainId" -> (short) 1;
            case "isSelected" -> false;
            default -> throw new UnsupportedOperationException(m.getName());
        });
        IMapDataInformation<?> data = proxy(IMapDataInformation.class, (p, m, args) -> {
            if (m.isDefault()) return InvocationHandler.invokeDefault(p, m, args);
            if (m.getName().equals("getTile")) return tile;
            throw new UnsupportedOperationException(m.getName());
        });
        IMapInformation<?> info = proxy(IMapInformation.class, (p, m, args) -> data);
        IKwdFile kwd = proxy(IKwdFile.class, (p, m, args) ->
                TerrainFixtures.terrain((short)1, TerrainFixtures.flags(), 0, 0, 0, (short)1));
        IFogOfWarInformation fog = proxy(IFogOfWarInformation.class, (p, m, args) -> switch(m.getName()) {
            case "isHighlightable" -> Set.of(a,b).contains(args[0]);
            case "isPendingTagged" -> false;
            default -> true;
        });
        MapViewController view = new MapViewController(assets, kwd, info, fog, (short)1, false, false) {
            @Override protected void updateProgress(float progress) { }
        };
        Material shared = new Material(assets, "MatDefs/DkLighting/Vertex.j3md");
        Method tint = MapViewController.class.getDeclaredMethod("setTileMaterialToGeometries", IMapTileInformation.class, Node.class);
        tint.setAccessible(true);
        view.previewTaggedTiles(List.of(a, floor), true);
        assertTrue(view.isTileTagged(a));
        assertFalse(view.isTileTagged(b));
        assertFalse(view.isTileTagged(floor));
        Geometry tagged = geometry(shared);
        Node taggedNode = new Node();taggedNode.attachChild(tagged);
        tint.invoke(view, tile, taggedNode);
        assertNotSame(shared, tagged.getMaterial(), "Preview must not tint other tiles sharing the material");
        assertNotEquals(shared.getParam("TintOverlay").getValue(), tagged.getMaterial().getParam("TintOverlay").getValue());
        view.cancelTagPreview();
        Geometry restored = geometry(shared);
        Node restoredNode = new Node();restoredNode.attachChild(restored);
        tint.invoke(view, tile, restoredNode);
        assertSame(shared, restored.getMaterial());
        assertFalse(view.isTileTagged(a));
    }

    private static Geometry geometry(Material material) {
        Geometry geometry = new Geometry("synthetic wall", new Box(1,1,1));
        geometry.setMaterial(material);
        return geometry;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
