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
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.sound.SoundManager;

/**
 * Utility class for Nifty related stuff
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class NiftyUtils {

    private static final Set<String> REGISTERED_SOUNDS = new HashSet<>();

    private NiftyUtils() {
    }

    /**
     * After changing label text, you need to resize its constraints
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
        String assetPath = SoundManager.getSoundAssetPath(category, id);
        if (assetPath == null) {
            return null;
        }

        // Nifty logs a warning for every unknown sound, so remember what has been added
        SoundHandle soundHandler = null;
        if (REGISTERED_SOUNDS.contains(assetPath) || nifty.getSoundSystem().addSound(assetPath, assetPath)) {
            REGISTERED_SOUNDS.add(assetPath);
            soundHandler = nifty.getSoundSystem().getSound(assetPath);
        }

        return soundHandler;
    }

    /**
     * Plays a GUI sound right away
     */
    public static void playSound(Nifty nifty, String category, int id) {
        SoundHandle soundHandler = getSoundHandler(nifty, category, id);
        if (soundHandler != null) {
            soundHandler.play();
        }
    }

}
