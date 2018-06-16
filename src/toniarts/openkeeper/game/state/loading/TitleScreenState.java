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
package toniarts.openkeeper.game.state.loading;

import com.jme3.asset.TextureKey;
import com.jme3.texture.Texture;
import java.util.Locale;
import toniarts.openkeeper.Main;

/**
 * Loading screen state without any progression
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class TitleScreenState extends LoadingState {

    public TitleScreenState(final Main app) {
        super(app);
    }

    @Override
    protected Texture getLoadingScreenTexture() {

        // Load up the texture, there are few localized ones available
        TextureKey texKey = new TextureKey("Textures/TitleScreen/Titlescreen-" + Locale.getDefault().getDisplayLanguage(Locale.ENGLISH) + ".png");
        if (assetManager.locateAsset(texKey) == null) {
            texKey = new TextureKey("Textures/TitleScreen/Titlescreen.png");
        }
        return assetManager.loadTexture(texKey);
    }
}
