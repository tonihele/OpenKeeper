/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.AssetUtils;
import static org.junit.jupiter.api.Assertions.*;

class AssetContextIsolationTest {
    private static final class Assets extends DesktopAssetManager {
        int loads;
        final String root;
        Assets(String root) { super(true); this.root = root; }
        @Override public Spatial loadModel(ModelKey key) { loads++; return new Node(root); }
    }

    @Test void identicalKeysNeverCrossAssetManagersAndReturnedClonesAreIndependent() {
        var a = new Assets("A"); var b = new Assets("B");
        var first = AssetUtils.loadModel(a, "context-isolation", null);
        assertEquals("A", first.getName()); first.setName("mutated clone");
        assertEquals("B", AssetUtils.loadModel(b, "context-isolation", null).getName());
        assertEquals("A", AssetUtils.loadModel(a, "context-isolation", null).getName());
        assertEquals(1, a.loads); assertEquals(1, b.loads);
    }

    @Test void explicitReleaseAndManagerClearDropUtilityTemplatesWithoutChangingLiveOwners() {
        var assets = new Assets("root");
        var live = AssetUtils.loadModel(assets, "context-release", null);
        AssetUtils.releaseCaches(assets);
        AssetUtils.loadModel(assets, "context-release", null);
        assertEquals(2, assets.loads);
        assets.clearCache();
        AssetUtils.loadModel(assets, "context-release", null);
        assertEquals(3, assets.loads); assertEquals("root", live.getName());
        AssetUtils.releaseCaches(assets); AssetUtils.releaseCaches(assets);
    }

    @Test void weakTemplatesAreManagerScopedToo() {
        var a = new Assets("weakA"); var b = new Assets("weakB");
        assertEquals("weakA", AssetUtils.loadModel(a, "context-weak", true).getName());
        assertEquals("weakB", AssetUtils.loadModel(b, "context-weak", true).getName());
    }
}
