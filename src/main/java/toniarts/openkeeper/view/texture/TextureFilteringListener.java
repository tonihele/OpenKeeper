/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.texture;

import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import toniarts.openkeeper.game.data.TextureFiltering;

/** Applies world-texture filtering before cached assets are cloned or rendered. */
public final class TextureFilteringListener implements AssetEventListener {
    private final AssetManager assets;
    private final Supplier<TextureFiltering> setting;
    private final int maxAnisotropy;
    private final Set<Texture> textures = Collections.newSetFromMap(new WeakHashMap<>());

    public TextureFilteringListener(AssetManager assets, Supplier<TextureFiltering> setting, int maxAnisotropy) {
        this.assets = assets;
        this.setting = setting;
        this.maxAnisotropy = Math.max(1, maxAnisotropy);
    }

    @Override public void assetRequested(AssetKey key) { }
    @Override public void assetDependencyNotFound(AssetKey parentKey, AssetKey dependentAssetKey) { }

    @Override public void assetLoaded(AssetKey key) {
        if (key.getCacheType() == null) return;
        Object asset = assets.getFromCache(key);
        if (asset instanceof Texture texture && key instanceof TextureKey textureKey) {
            // The cached original has no key yet; jME assigns it only to the clone after this callback.
            texture.setKey(textureKey);
            apply(texture);
        }
        else if (asset instanceof Material material) apply(material);
        else if (asset instanceof Spatial spatial) apply(spatial);
    }

    public void apply(Spatial spatial) {
        spatial.depthFirstTraversal(child -> {
            if (child instanceof Geometry geometry && geometry.getMaterial() != null) {
                apply(geometry.getMaterial());
            }
        });
    }

    private void apply(Material material) {
        for (var parameter : material.getParams()) {
            if (parameter instanceof MatParamTexture texture) apply(texture.getTextureValue());
        }
    }

    /** Unkeyed textures include generated sprite atlases, shadow maps and light data. */
    public synchronized void apply(Texture texture) {
        if (texture == null || !(texture.getKey() instanceof TextureKey key)) return;
        String name = key.getName().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (!name.startsWith("textures/") || name.startsWith("textures/gui/")) return;
        textures.add(texture);
        TextureFiltering selected = setting.get();
        // setMinFilter also asks jME to generate missing mipmaps, including for converted models.
        texture.setMinFilter(selected.getMinFilter());
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        texture.setAnisotropicFilter(Math.min(selected.getAnisotropy(), maxAnisotropy));
    }

    /** Refresh cached originals as well as visible material instances after a settings change. */
    public synchronized void refresh() {
        for (Texture texture : textures.toArray(Texture[]::new)) apply(texture);
    }
}
