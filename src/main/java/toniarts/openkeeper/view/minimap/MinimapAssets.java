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
package toniarts.openkeeper.view.minimap;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.utils.PathUtils;

/**
 * The minimap's static, non-derived assets: the palette (§4.1), the rock
 * texture (§4.2) and the camera-frustum sprite (§4.3). Loaded once and
 * reused for the life of the session - none of this changes at runtime.
 */
public final class MinimapAssets {

    private static final String PALETTE_PATH = "Textures" + File.separator + "GUI"
            + File.separator + "Map" + File.separator + "MapColours.png";
    private static final String ROCK_TEXTURE_PATH = "Textures" + File.separator + "GUI"
            + File.separator + "Map" + File.separator + "Map-BG.png";
    private static final String FRUSTUM_SPRITE_ASSET_PATH = "Textures/GUI/Map/MapCameraBox.png";

    public static final int ROCK_TEXTURE_SIZE = 128;

    private final MinimapPalette palette;
    private final byte[] rockTextureBgr;

    private MinimapAssets(MinimapPalette palette, byte[] rockTextureBgr) {
        this.palette = palette;
        this.rockTextureBgr = rockTextureBgr;
    }

    public MinimapPalette getPalette() {
        return palette;
    }

    /**
     * 128x128x3 bytes, B,G,R order per pixel (matching the final raster's
     * own {@code BGR8} format, so {@code fillRock} can copy bytes straight
     * across with no per-pixel conversion), row-major, index
     * {@code (y * 128 + x) * 3}. Read-only: callers must not mutate the
     * returned array.
     */
    public byte[] rockTextureBgr() {
        return rockTextureBgr;
    }

    /**
     * Resolves and decodes both files from the converted-assets folder,
     * following the same path-resolution convention
     * {@code utils.MapThumbnailGenerator} already uses for its own
     * (unrelated) palette file.
     *
     * @throws IllegalArgumentException if {@code MapColours.png} isn't
     * 24-bit RGB, or {@code Map-BG.png} isn't exactly 128x128 (any other
     * size breaks the rasteriser's {@code (x & 127, y & 127)} tiling)
     * @throws IOException if either file can't be found/read
     */
    public static MinimapAssets load() throws IOException {
        return fromImages(readImage(PALETTE_PATH), readImage(ROCK_TEXTURE_PATH));
    }

    /**
     * The pure decode step behind {@link #load()}, split out so it's
     * headlessly testable against already-decoded images.
     *
     * @throws IllegalArgumentException if {@code paletteImage} isn't 24-bit
     * RGB, or {@code rockImage} isn't exactly 128x128 (any other size
     * breaks the rasteriser's {@code (x & 127, y & 127)} tiling)
     */
    public static MinimapAssets fromImages(BufferedImage paletteImage, BufferedImage rockImage) {
        if (rockImage.getWidth() != ROCK_TEXTURE_SIZE || rockImage.getHeight() != ROCK_TEXTURE_SIZE) {
            throw new IllegalArgumentException("Map-BG.png must be exactly "
                    + ROCK_TEXTURE_SIZE + "x" + ROCK_TEXTURE_SIZE + ", was "
                    + rockImage.getWidth() + "x" + rockImage.getHeight());
        }

        return new MinimapAssets(MinimapPalette.load(paletteImage), toBgrBytes(rockImage));
    }

    private static byte[] toBgrBytes(BufferedImage image) {
        byte[] bgr = new byte[ROCK_TEXTURE_SIZE * ROCK_TEXTURE_SIZE * 3];
        int i = 0;
        for (int y = 0; y < ROCK_TEXTURE_SIZE; y++) {
            for (int x = 0; x < ROCK_TEXTURE_SIZE; x++) {
                int rgb = image.getRGB(x, y);
                bgr[i++] = (byte) rgb;
                bgr[i++] = (byte) (rgb >> 8);
                bgr[i++] = (byte) (rgb >> 16);
            }
        }
        return bgr;
    }

    private static BufferedImage readImage(String assetRelativePath) throws IOException {
        Path path = Paths.get(PathUtils.getRealFileName(AssetsConverter.getAssetsFolder(), assetRelativePath));
        ImageIO.setUseCache(false);
        try (InputStream is = Files.newInputStream(path); BufferedInputStream bis = new BufferedInputStream(is)) {
            return ImageIO.read(bis);
        }
    }

    /**
     * The fit-mode frustum outline sprite (design §4.3), loaded through the
     * same GUI-texture asset path the rest of the interface's sprites use -
     * treated as an opaque handle, nothing here inspects its pixels.
     */
    public static Texture2D loadFrustumSprite(AssetManager assetManager) {
        Texture texture = assetManager.loadTexture(FRUSTUM_SPRITE_ASSET_PATH);
        return (Texture2D) texture;
    }

}
