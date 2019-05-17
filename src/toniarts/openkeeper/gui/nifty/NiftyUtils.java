/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.gui.nifty;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.spi.sound.SoundHandle;
import de.lessvoid.nifty.tools.SizeValue;
import java.io.File;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.sound.SoundFile;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.modelviewer.SoundsLoader;

/**
 * Utility class for Nifty related stuff
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class NiftyUtils {

    private NiftyUtils() {
    }

    /**
     * After changing label text, you need to resize its contstraints
     *
     * @param label the label
     */
    public static void resetContraints(Label label) {
        TextRenderer renderer = label.getElement().getRenderer(TextRenderer.class);
        label.setHeight(new SizeValue(renderer.getTextHeight() + "px"));
        label.setWidth(new SizeValue(renderer.getTextWidth() + "px"));
        label.getElement().getParent().layoutElements();
    }

    @Nullable
    public static SoundHandle getSoundHandler(Nifty nifty, String category, int id) {
        SoundFile file = SoundsLoader.getAudioFile(category, id);
        if (file == null) {
            return null;
        }

        SoundHandle soundHandler = nifty.getSoundSystem().getSound(file.toString());
        if (soundHandler == null) {
            String filename = AssetsConverter.SOUNDS_FOLDER + File.separator + file.getFilename();
            if (nifty.getSoundSystem().addSound(file.toString(), filename)) {
                soundHandler = nifty.getSoundSystem().getSound(file.toString());
            }
        }

        return soundHandler;
    }

}
