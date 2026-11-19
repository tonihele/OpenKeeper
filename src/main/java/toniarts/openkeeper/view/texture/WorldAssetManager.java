/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.view.texture;

import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import com.jme3.asset.FilterKey;
import com.jme3.asset.ModelKey;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Caps;
import com.jme3.scene.Spatial;
import com.jme3.shader.ShaderGenerator;
import com.jme3.texture.Texture;

import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * An explicitly world-scoped view of the existing asset manager, with the same loaders and caches.
 * UI and conversion keep using the original manager. Preparation happens after complete
 * loader/deserialization processing and on cache hits too; no event listener or cached Texture key
 * mutation is required.
 */
public final class WorldAssetManager implements AssetManager {

    private final AssetManager delegate;
    private final WorldTextureFiltering filtering;

    public WorldAssetManager(AssetManager delegate, WorldTextureFiltering filtering) {
        this.delegate = Objects.requireNonNull(delegate);
        this.filtering = Objects.requireNonNull(filtering);
    }

    public void prepareDerived(Texture result, Texture source) {
        filtering.prepareDerived(result, source);
    }

    private <T> T prepare(T asset) {
        if (asset instanceof Texture texture) {
            filtering.prepare(texture);
        } else if (asset instanceof Material material) {
            filtering.prepare(material);
        } else if (asset instanceof Spatial spatial) {
            filtering.prepare(spatial);
        }
        return asset;
    }

    @Override
    public void registerLoader(Class<? extends AssetLoader> loaderClass, String... extensions) {
        delegate.registerLoader(loaderClass, extensions);
    }

    @Override
    public void unregisterLoader(Class<? extends AssetLoader> loaderClass) {
        delegate.unregisterLoader(loaderClass);
    }

    @Override
    public void registerLocator(String rootPath, Class<? extends AssetLocator> locatorClass) {
        delegate.registerLocator(rootPath, locatorClass);
    }

    @Override
    public void unregisterLocator(String rootPath, Class<? extends AssetLocator> locatorClass) {
        delegate.unregisterLocator(rootPath, locatorClass);
    }

    @Override
    public void addAssetEventListener(AssetEventListener listener) {
        delegate.addAssetEventListener(listener);
    }

    @Override
    public void removeAssetEventListener(AssetEventListener listener) {
        delegate.removeAssetEventListener(listener);
    }

    @Override
    public void clearAssetEventListeners() {
        delegate.clearAssetEventListeners();
    }

    @Override
    public AssetInfo locateAsset(AssetKey<?> key) {
        return delegate.locateAsset(key);
    }

    @Override
    public <T> T loadAssetFromStream(AssetKey<T> key, InputStream inputStream) {
        return prepare(delegate.loadAssetFromStream(key, inputStream));
    }

    @Override
    public <T> T loadAsset(AssetKey<T> key) {
        return prepare(delegate.loadAsset(key));
    }

    @Override
    public Object loadAsset(String name) {
        return prepare(delegate.loadAsset(name));
    }

    @Override
    public Texture loadTexture(TextureKey key) {
        return prepare(delegate.loadTexture(key));
    }

    @Override
    public Texture loadTexture(String name) {
        return prepare(delegate.loadTexture(name));
    }

    @Override
    public AudioData loadAudio(AudioKey key) {
        return delegate.loadAudio(key);
    }

    @Override
    public AudioData loadAudio(String name) {
        return delegate.loadAudio(name);
    }

    @Override
    public Spatial loadModel(ModelKey key) {
        return prepare(delegate.loadModel(key));
    }

    @Override
    public Spatial loadModel(String name) {
        return prepare(delegate.loadModel(name));
    }

    @Override
    public Material loadMaterial(String name) {
        return prepare(delegate.loadMaterial(name));
    }

    @Override
    public BitmapFont loadFont(String name) {
        return delegate.loadFont(name);
    }

    @Override
    public FilterPostProcessor loadFilter(FilterKey key) {
        return delegate.loadFilter(key);
    }

    @Override
    public FilterPostProcessor loadFilter(String name) {
        return delegate.loadFilter(name);
    }

    @Override
    public void setShaderGenerator(ShaderGenerator generator) {
        delegate.setShaderGenerator(generator);
    }

    @Override
    public ShaderGenerator getShaderGenerator(EnumSet<Caps> caps) {
        return delegate.getShaderGenerator(caps);
    }

    @Override
    public <T> T getFromCache(AssetKey<T> key) {
        return delegate.getFromCache(key);
    }

    @Override
    public <T> void addToCache(AssetKey<T> key, T asset) {
        delegate.addToCache(key, asset);
    }

    @Override
    public <T> boolean deleteFromCache(AssetKey<T> key) {
        return delegate.deleteFromCache(key);
    }

    @Override
    public void clearCache() {
        delegate.clearCache();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addClassLoader(ClassLoader loader) {
        delegate.addClassLoader(loader);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeClassLoader(ClassLoader loader) {
        delegate.removeClassLoader(loader);
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<ClassLoader> getClassLoaders() {
        return delegate.getClassLoaders();
    }
}
