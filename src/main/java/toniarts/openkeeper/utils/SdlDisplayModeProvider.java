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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDL_DisplayMode;
import static org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO;
import static org.lwjgl.sdl.SDLInit.SDL_InitSubSystem;
import static org.lwjgl.sdl.SDLInit.SDL_QuitSubSystem;
import static org.lwjgl.sdl.SDLInit.SDL_WasInit;
import static org.lwjgl.sdl.SDLVideo.SDL_GetCurrentDisplayMode;
import static org.lwjgl.sdl.SDLVideo.SDL_GetFullscreenDisplayModes;
import static org.lwjgl.sdl.SDLVideo.SDL_GetPrimaryDisplay;
import static org.lwjgl.sdl.SDLPixels.SDL_BITSPERPIXEL;
import static org.lwjgl.sdl.SDLStdinc.SDL_free;

/**
 * Enumerates the primary display modes through the SDL3 API used by jME 3.10.
 */
final class SdlDisplayModeProvider extends DefaultDisplayModeProvider {

    @Override
    public List<DisplayMode> getDisplayModes() {
        boolean initializeVideo = (SDL_WasInit(SDL_INIT_VIDEO) & SDL_INIT_VIDEO) == 0;
        if (initializeVideo && !SDL_InitSubSystem(SDL_INIT_VIDEO)) {
            return Collections.emptyList();
        }

        try {
            int displayId = SDL_GetPrimaryDisplay();
            if (displayId == 0) {
                return Collections.emptyList();
            }

            List<DisplayMode> displayModes = new ArrayList<>();
            PointerBuffer modes = SDL_GetFullscreenDisplayModes(displayId);
            if (modes != null) {
                try {
                    while (modes.hasRemaining()) {
                        addDisplayMode(displayModes, toDisplayMode(SDL_DisplayMode.create(modes.get())));
                    }
                } finally {
                    SDL_free(modes);
                }
            }

            if (displayModes.isEmpty()) {
                SDL_DisplayMode currentMode = SDL_GetCurrentDisplayMode(displayId);
                if (currentMode != null) {
                    addDisplayMode(displayModes, toDisplayMode(currentMode));
                }
            }

            return displayModes;
        } finally {
            if (initializeVideo) {
                SDL_QuitSubSystem(SDL_INIT_VIDEO);
            }
        }
    }

    private DisplayMode toDisplayMode(SDL_DisplayMode mode) {
        DisplayMode displayMode = new DisplayMode(mode.w(), mode.h());
        displayMode.addBitDepth(SDL_BITSPERPIXEL(mode.format()));
        displayMode.addRefreshRate(Math.round(mode.refresh_rate()));
        return displayMode;
    }

    @Override
    public boolean isFullScreenSupported() {
        return true;
    }
}
