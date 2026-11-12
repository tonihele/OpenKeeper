package toniarts.openkeeper.view.material;

import com.jme3.scene.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VertexLightingSetupCacheTest {

    @Test
    void defaultProviderPolicyIsUncachedAndGameplayCanOptIn() {
        assertFalse(MaterialProviderConfig.DEFAULT.cacheVertexLightingSetup());
        assertTrue(MaterialProviderConfig.gameplayDefault().cacheVertexLightingSetup());
        assertFalse(MaterialProviderConfig.UNCACHED.cacheVertexLightingSetup());
        assertTrue(MaterialProviderConfig.CACHED.cacheVertexLightingSetup());
    }

    @Test
    void cachesOnlyTheSameSpatialContextAndLightingOptions() {
        VertexLightingSetupCache cache = new VertexLightingSetupCache();
        Node spatial = new Node("model");
        Object lightContext = new Object();
        Object otherLightContext = new Object();

        assertFalse(cache.isConfigured(spatial, lightContext, 4L, false, true));

        cache.markConfigured(spatial, lightContext, 4L, false, true);

        assertTrue(cache.isConfigured(spatial, lightContext, 4L, false, true));
        assertFalse(cache.isConfigured(spatial, lightContext, 5L, false, true));
        assertFalse(cache.isConfigured(spatial, otherLightContext, 4L, false, true));
        assertFalse(cache.isConfigured(spatial, lightContext, 4L, true, true));
        assertFalse(cache.isConfigured(spatial, lightContext, 4L, false, false));
    }

    @Test
    void invalidationAndClearForceSetupAgain() {
        VertexLightingSetupCache cache = new VertexLightingSetupCache();
        Node first = new Node("first");
        Node second = new Node("second");
        Object lightContext = new Object();
        cache.markConfigured(first, lightContext, 0L, false, true);
        cache.markConfigured(second, lightContext, 0L, false, true);

        cache.invalidate(first);

        assertFalse(cache.isConfigured(first, lightContext, 0L, false, true));
        assertTrue(cache.isConfigured(second, lightContext, 0L, false, true));

        cache.clear();

        assertFalse(cache.isConfigured(second, lightContext, 0L, false, true));
    }
}
