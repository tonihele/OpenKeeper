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
package toniarts.openkeeper.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class AssetUtilsTextureMipmapsTest {

    @Test
    void largeWorldTextureUsesTrilinearFilteringAndRequestsGpuMipmaps() {
        ByteBuffer basePixels = BufferUtils.createByteBuffer(128 * 128 * 4);
        Image image = new Image(Image.Format.RGBA8, 128, 128, basePixels);
        Texture texture = new Texture2D(image);

        Texture result = AssetUtils.enableWorldTextureMipmaps(texture);

        assertSame(texture, result);
        assertEquals(Texture.MinFilter.Trilinear, texture.getMinFilter());
        assertTrue(image.isGeneratedMipmapsRequired());
        assertFalse(image.hasMipmaps());
        assertEquals(128 * 128 * 4, image.getData(0).remaining());
    }

    @Test
    void smallWorldTextureKeepsBaseLevelFiltering() {
        ByteBuffer basePixels = BufferUtils.createByteBuffer(64 * 64 * 4);
        Image image = new Image(Image.Format.RGBA8, 64, 64, basePixels);
        Texture texture = new Texture2D(image);

        AssetUtils.enableWorldTextureMipmaps(texture);

        assertEquals(Texture.MinFilter.BilinearNoMipMaps, texture.getMinFilter());
        assertFalse(image.isGeneratedMipmapsRequired());
    }
}
