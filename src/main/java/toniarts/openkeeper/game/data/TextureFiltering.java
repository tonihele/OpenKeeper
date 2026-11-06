/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.data;

import com.jme3.texture.Texture;

/** User-selectable filtering for ordinary world textures. */
public enum TextureFiltering {
    BILINEAR("Bilinear", Texture.MinFilter.BilinearNearestMipMap, 1),
    TRILINEAR("Trilinear", Texture.MinFilter.Trilinear, 1),
    ANISOTROPIC_2("Anisotropic 2×", Texture.MinFilter.Trilinear, 2),
    ANISOTROPIC_4("Anisotropic 4×", Texture.MinFilter.Trilinear, 4),
    ANISOTROPIC_8("Anisotropic 8×", Texture.MinFilter.Trilinear, 8),
    ANISOTROPIC_16("Anisotropic 16×", Texture.MinFilter.Trilinear, 16);

    private final String label;
    private final Texture.MinFilter minFilter;
    private final int anisotropy;

    TextureFiltering(String label, Texture.MinFilter minFilter, int anisotropy) {
        this.label = label;
        this.minFilter = minFilter;
        this.anisotropy = anisotropy;
    }

    public Texture.MinFilter getMinFilter() { return minFilter; }
    public int getAnisotropy() { return anisotropy; }

    /** Older configurations contain only an anisotropy level. */
    public static TextureFiltering resolve(String saved, int legacyAnisotropy) {
        if (saved != null && !saved.isBlank()) {
            try {
                return valueOf(saved);
            } catch (IllegalArgumentException ignored) {
                // Unknown settings fall back to the older configuration.
            }
        }
        if (legacyAnisotropy <= 1) return TRILINEAR;
        if (legacyAnisotropy <= 2) return ANISOTROPIC_2;
        if (legacyAnisotropy <= 4) return ANISOTROPIC_4;
        if (legacyAnisotropy <= 8) return ANISOTROPIC_8;
        return ANISOTROPIC_16;
    }

    @Override public String toString() { return label; }
}
