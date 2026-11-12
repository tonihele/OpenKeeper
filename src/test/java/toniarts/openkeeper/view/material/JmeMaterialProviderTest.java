package toniarts.openkeeper.view.material;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.lighting.LightRules;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JmeMaterialProviderTest {

    @Test
    void rebindsReplacementAnimationMeshesAfterInitialSetup() {
        DesktopAssetManager assetManager = new DesktopAssetManager(true);
        assetManager.registerLocator("assets", FileLocator.class);

        Node model = new Node("model");
        Geometry initialGeometry = geometry(assetManager, "initial");
        model.attachChild(initialGeometry);

        LightRules lightRules = new LightRules();
        JmeMaterialProvider provider = new JmeMaterialProvider(assetManager);

        // The first setup can happen before the level's ambient texture exists.
        provider.applyVertexLight(model, lightRules, false, true);
        assertTrue(AssetUtils.isVertexMaterial(initialGeometry.getMaterial()));
        assertNotNull(initialGeometry.getMaterial().getTextureParam("DiffuseMap"));
        assertNotNull(initialGeometry.getMaterial().getTextureParam("LightData"));
        assertNull(initialGeometry.getMaterial().getTextureParam("AmbientMap"));

        // AnimationLoader replaces child models when a creature or object
        // changes animation; those new materials must be rebound as well.
        lightRules.initAmbientMap(4, 4);
        model.detachChild(initialGeometry);
        Geometry addedGeometry = geometry(assetManager, "added");
        model.attachChild(addedGeometry);
        provider.applyVertexLight(model, lightRules, false, true);

        assertTrue(AssetUtils.isVertexMaterial(addedGeometry.getMaterial()));
        assertNotNull(addedGeometry.getMaterial().getTextureParam("DiffuseMap"));
        assertNotNull(addedGeometry.getMaterial().getTextureParam("LightData"));
        assertNotNull(addedGeometry.getMaterial().getTextureParam("AmbientMap"));
    }

    @Test
    void rebindsExistingMaterialsWhenAmbientMapBecomesAvailable() {
        DesktopAssetManager assetManager = new DesktopAssetManager(true);
        assetManager.registerLocator("assets", FileLocator.class);

        Node model = new Node("model");
        Geometry geometry = geometry(assetManager, "initial");
        model.attachChild(geometry);
        LightRules lightRules = new LightRules();
        JmeMaterialProvider provider = new JmeMaterialProvider(assetManager, MaterialProviderConfig.CACHED);

        provider.applyVertexLight(model, lightRules, false, true);
        assertNull(geometry.getMaterial().getTextureParam("AmbientMap"));

        lightRules.initAmbientMap(4, 4);
        provider.applyVertexLight(model, lightRules, false, true);

        assertNotNull(geometry.getMaterial().getTextureParam("AmbientMap"));
    }

    @Test
    void legacyAssetUtilsFacadeKeepsApplyingVertexLighting() {
        DesktopAssetManager assetManager = new DesktopAssetManager(true);
        assetManager.registerLocator("assets", FileLocator.class);

        Node model = new Node("model");
        Geometry geometry = geometry(assetManager, "legacy-facade");
        model.attachChild(geometry);
        LightRules lightRules = new LightRules();

        AssetUtils.applyVertexLight(assetManager, model, lightRules, false, true);

        assertTrue(AssetUtils.isVertexMaterial(geometry.getMaterial()));
        assertNotNull(geometry.getMaterial().getTextureParam("DiffuseMap"));
        assertNotNull(geometry.getMaterial().getTextureParam("LightData"));
    }

    @Test
    void invalidationReconfiguresReplacementGeometryInCachedMode() {
        DesktopAssetManager assetManager = new DesktopAssetManager(true);
        assetManager.registerLocator("assets", FileLocator.class);

        Node model = new Node("model");
        Geometry initialGeometry = geometry(assetManager, "initial");
        model.attachChild(initialGeometry);
        LightRules lightRules = new LightRules();
        lightRules.initAmbientMap(4, 4);
        JmeMaterialProvider provider = new JmeMaterialProvider(assetManager, MaterialProviderConfig.CACHED);
        provider.applyVertexLight(model, lightRules, false, true);

        model.detachChild(initialGeometry);
        Geometry replacement = geometry(assetManager, "replacement");
        model.attachChild(replacement);
        provider.invalidate(model);
        provider.applyVertexLight(model, lightRules, false, true);

        assertTrue(AssetUtils.isVertexMaterial(replacement.getMaterial()));
        assertNotNull(replacement.getMaterial().getTextureParam("DiffuseMap"));
        assertNotNull(replacement.getMaterial().getTextureParam("LightData"));
        assertNotNull(replacement.getMaterial().getTextureParam("AmbientMap"));
    }

    private static Geometry geometry(DesktopAssetManager assetManager, String name) {
        Geometry geometry = new Geometry(name, new Quad(1, 1));
        Material material = new Material(assetManager, "MatDefs/DkLighting/Flat.j3md");
        Image image = new Image(Image.Format.RGBA8, 1, 1, ByteBuffer.allocateDirect(4));
        material.setTexture("DiffuseMap", new Texture2D(image));
        geometry.setMaterial(material);
        return geometry;
    }
}
