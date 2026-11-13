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
package toniarts.openkeeper.gui.nifty.jme;

import com.jme3.niftygui.JmeBatchRenderBackend;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import de.lessvoid.nifty.render.batch.BatchRenderConfiguration;
import de.lessvoid.nifty.render.batch.BatchRenderDevice;
import de.lessvoid.nifty.render.batch.BatchRenderImage;
import de.lessvoid.nifty.render.batch.TextureAtlasGenerator;
import de.lessvoid.nifty.render.batch.spi.BatchRenderBackend;
import de.lessvoid.nifty.spi.render.RenderImage;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Nifty's batched render device, but an image whose size differs from its
 * converted original's, like an HD copy made by tools/hd-textures, reports the
 * original's size. Nifty lays images out by their size, so it is laid out as
 * before and drawn with the extra detail.
 *
 * @author OpenKeeper
 */
public class HdImageRenderDevice extends BatchRenderDevice {

    private static final Logger logger = System.getLogger(HdImageRenderDevice.class.getName());

    /**
     * Takes no image, so an HD image always gets a texture of its own. Nifty
     * works out the texture coordinates from the size the image reports,
     * which only fits a texture of its own.
     */
    private static final TextureAtlasGenerator NO_ATLAS = new TextureAtlasGenerator(1, 1, 0, 0f);

    private final BatchRenderBackend renderBackend;
    private final Path originalsFolder;
    private final Map<String, RenderImage> hdImages = new HashMap<>();

    /**
     * Creates the render device
     *
     * @param renderBackend the render backend
     * @param renderConfig the batch configuration
     * @param originalsFolder the folder of the converted originals, the image
     * file names are relative to it
     */
    public HdImageRenderDevice(BatchRenderBackend renderBackend, BatchRenderConfiguration renderConfig,
            Path originalsFolder) {
        super(renderBackend, renderConfig);
        this.renderBackend = renderBackend;
        this.originalsFolder = originalsFolder;
    }

    @Override
    public RenderImage createImage(String filename, boolean filterLinear) {
        RenderImage hdImage = hdImages.get(filename);
        if (hdImage != null) {
            return hdImage;
        }

        Dimension original = getOriginalSize(filename);
        if (original != null) {
            BatchRenderBackend.Image image = renderBackend.loadImage(filename);
            if (image != null && (image.getWidth() != original.width || image.getHeight() != original.height)) {
                hdImage = new HdImage(image, filename, original);
                hdImages.put(filename, hdImage);
                return hdImage;
            }
        }
        return super.createImage(filename, filterLinear);
    }

    private Dimension getOriginalSize(String filename) {

        // Nifty's pulsating image overlay creates its image before the file name is set
        if (filename == null) {
            return null;
        }
        File file = new File(originalsFolder.toFile(), filename);
        if (!file.isFile()) {
            return null;
        }
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            reader.setInput(in);
            return new Dimension(reader.getWidth(0), reader.getHeight(0));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * An image that reports its original's size, with a smoothly filtered
     * texture of its own since it is drawn smaller than it is
     */
    private class HdImage extends BatchRenderImage {

        private final Dimension original;

        HdImage(BatchRenderBackend.Image image, String filename, Dimension original) {
            super(image, filename, renderBackend, NO_ATLAS, -1, false);
            this.original = original;
        }

        @Override
        public int getWidth() {
            return original.width;
        }

        @Override
        public int getHeight() {
            return original.height;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void upload() {
            super.upload();
            if (!isUploaded() || !(renderBackend instanceof JmeBatchRenderBackend)) {
                return;
            }

            // jME keeps the textures to itself and filters them with the nearest pixel
            try {
                Field textures = JmeBatchRenderBackend.class.getDeclaredField("textures");
                textures.setAccessible(true);
                Texture2D texture = ((Map<Integer, Texture2D>) textures.get(renderBackend)).get(getTextureId());
                texture.setMinFilter(Texture.MinFilter.Trilinear);
                texture.setMagFilter(Texture.MagFilter.Bilinear);
            } catch (ReflectiveOperationException | RuntimeException e) {
                logger.log(Level.WARNING, "Can't filter the HD interface image " + this + " smoothly!", e);
            }
        }
    }

}
