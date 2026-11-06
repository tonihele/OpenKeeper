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
import de.lessvoid.nifty.spi.time.impl.AccurateTimeProvider;
import java.nio.file.Path;

/**
 * jME's Nifty display with the batched renderer, but with the
 * {@link HdImageRenderDevice}, so HD copies of the interface images are laid
 * out at their original's size
 *
 * @author OpenKeeper
 */
public class HdImageNiftyJmeDisplay extends NiftyJmeDisplay {

    /**
     * Creates the Nifty display, like
     * {@link NiftyJmeDisplay#newNiftyJmeDisplay(AssetManager, InputManager, AudioRenderer, ViewPort, BatchRenderConfiguration)}
     *
     * @param assetManager the asset manager
     * @param inputManager the input manager
     * @param audioRenderer the audio renderer
     * @param viewport the view port to draw on
     * @param batchRenderConfiguration the batch configuration
     * @param originalsFolder the folder of the converted originals
     */
    public HdImageNiftyJmeDisplay(AssetManager assetManager, InputManager inputManager,
            AudioRenderer audioRenderer, ViewPort viewport, BatchRenderConfiguration batchRenderConfiguration,
            Path originalsFolder) {
        this.assetManager = assetManager;
        this.inputManager = inputManager;
        this.w = viewport.getCamera().getWidth();
        this.h = viewport.getCamera().getHeight();
        this.soundDev = new SoundDeviceJme(assetManager, audioRenderer);
        this.inputSys = new InputSystemJme(inputManager);
        this.batchRendererBackend = new JmeBatchRenderBackend(this);

        nifty = new Nifty(new HdImageRenderDevice(batchRendererBackend, batchRenderConfiguration, originalsFolder),
                soundDev, inputSys, new AccurateTimeProvider());
        inputSys.setNifty(nifty);

        resourceLocation = new ResourceLocationJme() {
        };
        nifty.getResourceLoader().removeAllResourceLocations();
        nifty.getResourceLoader().addResourceLocation(resourceLocation);
    }

}
