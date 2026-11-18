/*
 * Copyright (C) 2026 OpenKeeper
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package toniarts.openkeeper.view.texture;

import com.jme3.asset.TextureKey;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;

import toniarts.openkeeper.game.data.TextureFiltering;
import toniarts.openkeeper.utils.WeakIdentityMap;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntSupplier;

/**
 * One policy for explicitly adopted world assets. New unpublished samplers may be prepared on a
 * loading thread; refresh and changes to live samplers belong to the render thread. No asset keys,
 * cache prototypes or pixel buffers are owned by this service.
 */
public final class WorldTextureFiltering implements AutoCloseable {

    private static final Set<String> SURFACE_MAPS =
            Set.of(
                    "DiffuseMap",
                    "ColorMap",
                    "Texture",
                    "BaseColorMap",
                    "NormalMap",
                    "SpecularMap",
                    "ParallaxMap",
                    "AlphaMap",
                    "GlowMap",
                    "LightMap",
                    "RoughnessMap",
                    "MetallicMap",
                    "MetallicRoughnessMap",
                    "AoMap");
    private final WeakIdentityMap<Texture, WeakReference<Image>> textures = new WeakIdentityMap<>();
    private final WeakIdentityMap<Image, WeakReference<Image>> preparedImages =
            new WeakIdentityMap<>();
    private final IntSupplier anisotropyLimit;
    private TextureFiltering selected;
    private int effectiveAnisotropy;
    private boolean closed;

    public WorldTextureFiltering(TextureFiltering selected, IntSupplier anisotropyLimit) {
        this.anisotropyLimit = Objects.requireNonNull(anisotropyLimit);
        setSelection(selected);
    }

    /**
     * Prepares a standalone sampler returned to an explicit world consumer. After replacing a
     * managed sampler's Image, prepare it again on the render thread before drawing.
     */
    public synchronized Texture prepare(Texture texture) {
        if (!closed && texture != null && isOrdinary(texture)) {
            prepareSampler(texture);
        }
        return texture;
    }

    /** Run after deserialization, which restores its own sampler values. */
    public void prepare(Spatial spatial) {
        spatial.depthFirstTraversal(child -> {
            if (child instanceof Geometry geometry && geometry.getMaterial() != null) {
                prepare(geometry.getMaterial());
            }
        });
    }

    public void prepare(Material material) {
        var tiles = material.getParam("NumberOfTiles");
        if (tiles != null && tiles.getValue() instanceof Number count && count.intValue() > 1) {
            return; // Whole-strip mipmaps mix adjacent animation frames.
        }
        for (var parameter : material.getParams()) {
            if (parameter instanceof MatParamTexture map
                    && SURFACE_MAPS.contains(map.getName())
                    && map.getTextureValue() != null
                    && map.getTextureValue().getKey() instanceof TextureKey) {
                prepare(map.getTextureValue());
            }
        }
    }

    /** Keyless generated diffuse images inherit the source's managed role. */
    public synchronized void prepareDerived(Texture result, Texture source) {
        if (!closed
                && result != null
                && source != null
                && textures.containsKey(source)
                && isSupportedImage(result)) {
            prepareSampler(result);
        }
    }

    private void prepareSampler(Texture texture) {
        var previousImage = textures.get(texture);
        if (previousImage != null && previousImage.get() == texture.getImage()) {
            return; // Cache hit: no sampler writes or new registration.
        }
        prepareImage(texture);
        apply(texture);
        textures.put(texture, new WeakReference<>(texture.getImage()));
    }

    /** Apply on the render thread; all registered, including detached, samplers update. */
    public synchronized void refresh(TextureFiltering selected) {
        if (!closed) {
            var previous = this.selected;
            int previousAnisotropy = effectiveAnisotropy;
            setSelection(selected);
            if (previous == selected && previousAnisotropy == effectiveAnisotropy) {
                return;
            }
            textures.forEach((texture, ignored) -> apply(texture));
        }
    }

    public synchronized int getEffectiveAnisotropy() {
        return effectiveAnisotropy;
    }

    private void setSelection(TextureFiltering selected) {
        this.selected = Objects.requireNonNull(selected);
        effectiveAnisotropy =
                Math.min(selected.getAnisotropy(), Math.max(1, anisotropyLimit.getAsInt()));
    }

    private void apply(Texture texture) {
        Image image = texture.getImage();
        // Serialized sampler values alone do not guarantee that Image requests mips.
        if (texture.getMinFilter() != selected.getMinFilter()
                || (image != null && !image.hasMipmaps() && !image.isGeneratedMipmapsRequired())) {
            texture.setMinFilter(selected.getMinFilter());
        }
        if (texture.getMagFilter() != Texture.MagFilter.Bilinear) {
            texture.setMagFilter(Texture.MagFilter.Bilinear);
        }
        if (texture.getAnisotropicFilter() != effectiveAnisotropy) {
            texture.setAnisotropicFilter(effectiveAnisotropy);
        }
    }

    private static boolean isOrdinary(Texture texture) {
        if (!isSupportedImage(texture)) {
            return false;
        }
        if (!(texture.getKey() instanceof TextureKey key)) {
            // An explicitly loaded standalone stream is allowed. Material traversal
            // separately excludes keyless generated atlases and shader data.
            return texture.getKey() == null;
        }
        if (key.getName() == null) {
            return false;
        }
        String name = key.getName().replace('\\', '/').toLowerCase(Locale.ROOT);
        return !name.startsWith("interface/")
                && !name.startsWith("sprites/")
                && !name.startsWith("textures/gui/")
                && !name.startsWith("textures/loadingscreen")
                && !name.startsWith("textures/titlescreen/")
                && !name.startsWith("textures/ingamedigit")
                && !name.equals("textures/mapback.png")
                && !name.equals("textures/maprock.png")
                && !name.equals("textures/mapover.png");
    }

    private static boolean isSupportedImage(Texture texture) {
        Image image = texture.getImage();
        return texture.getType() == Texture.Type.TwoDimensional
                && image != null
                && image.getWidth() > 0
                && image.getHeight() > 0
                && image.getMultiSamples() <= 1
                && !image.getFormat().isDepthFormat()
                && !image.getData().isEmpty()
                && image.getData(0) != null;
    }

    private void prepareImage(Texture texture) {
        Image image = texture.getImage();
        WeakReference<Image> reference = preparedImages.get(image);
        Image prepared = reference == null ? null : reference.get();
        if (prepared == null) {
            prepared = image;
            if (image.getId() != -1 && !image.hasMipmaps() && !image.isMipmapsGenerated()) {
                // jME 3.9 leaves GL_TEXTURE_MAX_LEVEL at zero after a base-only upload.
                // Copy on adoption avoids disposing a GPU image still used by UI.
                // Construct a fresh Image: jME 3.9 Image.clone also copies the
                // cached weak reference used for texture binding state.
                prepared =
                        new Image(
                                image.getFormat(),
                                image.getWidth(),
                                image.getHeight(),
                                image.getData(0),
                                image.getColorSpace());
                preparedImages.put(prepared, new WeakReference<>(prepared));
            }
            preparedImages.put(image, new WeakReference<>(prepared));
        }
        if (prepared != image) {
            texture.setImage(prepared);
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        textures.clear();
        preparedImages.clear();
    }
}
