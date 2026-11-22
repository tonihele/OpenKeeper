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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.InputManager;
import com.jme3.niftygui.InputSystemJme;
import com.jme3.niftygui.JmeBatchRenderBackend;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.niftygui.SoundDeviceJme;
import com.jme3.renderer.ViewPort;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.render.batch.BatchRenderConfiguration;
import de.lessvoid.nifty.render.batch.BatchRenderDevice;
import de.lessvoid.nifty.render.batch.spi.BatchRenderBackend;
import de.lessvoid.nifty.spi.render.RenderImage;
import de.lessvoid.nifty.spi.time.impl.AccurateTimeProvider;
import de.lessvoid.nifty.tools.Color;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.hdtextures.HdTextures;

/**
 * jME's Nifty display with the batched renderer, where the HD copies of the
 * interface images made by tools/hd-textures are cut in their original's
 * pixels.
 * <p>
 * The screens give each image its original's size, so an HD copy is laid out
 * like the original. But an {@code imageMode} that cuts the image (repeat,
 * resize, sprite-resize) is in the image's own pixels, and Nifty draws each
 * piece at its size in pixels on screen too. So the cuts stay in the
 * original's pixels for the layout, and only where a piece is taken from the
 * HD copy is multiplied by the HD scale. Images the tool didn't write are
 * drawn as before.
 *
 * @author OpenKeeper
 */
public class HdImageNiftyJmeDisplay extends NiftyJmeDisplay {

    private static final Logger logger = System.getLogger(HdImageNiftyJmeDisplay.class.getName());

    /**
     * Creates the Nifty display, like
     * {@link NiftyJmeDisplay#newNiftyJmeDisplay(AssetManager, InputManager, AudioRenderer, ViewPort, BatchRenderConfiguration)}
     *
     * @param assetManager the asset manager
     * @param inputManager the input manager
     * @param audioRenderer the audio renderer
     * @param viewport the view port to draw on
     * @param batchRenderConfiguration the batch configuration
     */
    public HdImageNiftyJmeDisplay(AssetManager assetManager, InputManager inputManager,
            AudioRenderer audioRenderer, ViewPort viewport, BatchRenderConfiguration batchRenderConfiguration) {
        this.assetManager = assetManager;
        this.inputManager = inputManager;
        this.w = viewport.getCamera().getWidth();
        this.h = viewport.getCamera().getHeight();
        this.soundDev = new SoundDeviceJme(assetManager, audioRenderer);
        this.inputSys = new InputSystemJme(inputManager);
        this.batchRendererBackend = new JmeBatchRenderBackend(this);

        nifty = new Nifty(new HdImageRenderDevice(batchRendererBackend, batchRenderConfiguration, readHdImages(assetManager)),
                soundDev, inputSys, new AccurateTimeProvider());
        inputSys.setNifty(nifty);

        resourceLocation = new ResourceLocationJme() {
        };
        nifty.getResourceLoader().removeAllResourceLocations();
        nifty.getResourceLoader().addResourceLocation(resourceLocation);
    }

    /**
     * The images tools/hd-textures wrote, from its record of them
     */
    private static Set<String> readHdImages(AssetManager assetManager) {
        Set<String> images = new HashSet<>();
        AssetInfo record = assetManager.locateAsset(new AssetKey<>(AssetsConverter.TEXTURES_FOLDER + "/" + HdTextures.MANIFEST_NAME));
        if (record != null) {
            Properties manifest = new Properties();
            try (InputStream in = record.openStream()) {
                manifest.load(in);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Can't read the HD textures' record!", e);
            }
            for (String image : manifest.stringPropertyNames()) {
                images.add((AssetsConverter.TEXTURES_FOLDER + "/" + image).toLowerCase(Locale.ROOT));
            }
        }
        return images;
    }

    /**
     * Nifty's batched render device, but a cut of an HD image is taken from
     * the HD copy at the HD scale
     */
    static class HdImageRenderDevice extends BatchRenderDevice {

        private final Set<String> hdImageFiles;
        private final Set<RenderImage> hdImages = Collections.newSetFromMap(new WeakHashMap<>());

        HdImageRenderDevice(BatchRenderBackend renderBackend, BatchRenderConfiguration renderConfig, Set<String> hdImageFiles) {
            super(renderBackend, renderConfig);
            this.hdImageFiles = hdImageFiles;
        }

        @Override
        public RenderImage createImage(String filename, boolean filterLinear) {
            RenderImage image = super.createImage(filename, filterLinear);

            // Nifty's pulsating image overlay creates its image before the file name is set
            if (image != null && filename != null && hdImageFiles.contains(filename.toLowerCase(Locale.ROOT))) {
                hdImages.add(image);
            }
            return image;
        }

        @Override
        public void renderImage(RenderImage image, int x, int y, int w, int h, int srcX, int srcY, int srcW, int srcH,
                Color c, float scale, int centerX, int centerY) {

            // The whole image is the HD copy's own size, a cut is in the original's pixels
            int s = (srcW != image.getWidth() || srcH != image.getHeight()) && hdImages.contains(image) ? HdTextures.SCALE : 1;
            super.renderImage(image, x, y, w, h, srcX * s, srcY * s, srcW * s, srcH * s, c, scale, centerX, centerY);
        }
    }

}
