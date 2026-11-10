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

import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengles.GLES20;
import static org.lwjgl.sdl.SDLStdinc.SDL_setenv_unsafe;

/**
 * Maps OpenKeeper's graphics choices onto jMonkeyEngine and configures ANGLE
 * to use its Vulkan implementation when the Vulkan option is selected.
 */
public final class GraphicsBackend {

    private static final Logger LOGGER = Logger.getLogger(GraphicsBackend.class.getName());
    private static final String ANGLE_DEFAULT_PLATFORM = "ANGLE_DEFAULT_PLATFORM";
    private static final String ANGLE_VULKAN = "vulkan";
    private static final String OPENGL = "OpenGL";
    private static final String VULKAN = "Vulkan";

    /** The user-facing graphics option backed by jME's ANGLE GLES3 renderer. */
    public static final String VULKAN_OPTION = "Vulkan (ANGLE GLES3)";

    private GraphicsBackend() {
    }

    /**
     * Lists the available OpenGL renderers and the Vulkan compatibility
     * renderer.
     *
     * @return an immutable list of renderer menu options
     */
    public static List<String> getRendererOptions() {
        List<String> renderers = new ArrayList<>();
        try {
            for (Field field : AppSettings.class.getFields()) {
                if (field.getName().startsWith("LWJGL_OPENGL")
                        && field.getType().equals(String.class)) {
                    renderers.add((String) field.get(null));
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException ex) {
            LOGGER.warning("Unable to enumerate jMonkeyEngine OpenGL renderers: " + ex.getMessage());
        }
        if (isVulkanPlatformSupported()) {
            renderers.add(VULKAN_OPTION);
        }
        return Collections.unmodifiableList(renderers);
    }

    /**
     * Reports whether this platform has the bundled Vulkan compatibility runtime.
     *
     * @return {@code true} on Windows x86_64
     */
    public static boolean isVulkanPlatformSupported() {
        return isVulkanPlatformSupported(System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    static boolean isVulkanPlatformSupported(String operatingSystem, String architecture) {
        return operatingSystem != null
                && operatingSystem.toLowerCase(Locale.ROOT).startsWith("windows")
                && architecture != null
                && (architecture.equalsIgnoreCase("amd64") || architecture.equalsIgnoreCase("x86_64"));
    }

    /**
     * Converts a persisted jME renderer identifier to its graphics menu label.
     *
     * @param renderer the jME renderer identifier
     * @return the user-facing option
     */
    public static String toOption(String renderer) {
        return AppSettings.ANGLE_GLES3.equals(renderer) ? VULKAN_OPTION : renderer;
    }

    /**
     * Converts a graphics menu choice to the renderer identifier expected by
     * jMonkeyEngine.
     *
     * @param option the selected menu option
     * @return the jME renderer identifier
     */
    public static String toRenderer(String option) {
        return VULKAN_OPTION.equals(option) ? AppSettings.ANGLE_GLES3 : option;
    }

    /**
     * Returns whether the selected jME renderer is OpenKeeper's Vulkan mode.
     *
     * @param settings the current application settings
     * @return {@code true} for ANGLE GLES3 configured to use Vulkan
     */
    public static boolean isVulkan(AppSettings settings) {
        return AppSettings.ANGLE_GLES3.equals(settings.getRenderer());
    }

    /**
     * Selects ANGLE's Vulkan driver before jME creates its GLES context.
     *
     * @param settings the renderer settings that will be passed to jME
     */
    public static void prepareForStartup(AppSettings settings) {
        if (isVulkan(settings)) {
            if (!isVulkanPlatformSupported()) {
                throw new IllegalStateException("The bundled ANGLE Vulkan backend is available only on Windows x86_64");
            }
            if (SDL_setenv_unsafe(ANGLE_DEFAULT_PLATFORM, ANGLE_VULKAN, 1) != 0) {
                throw new IllegalStateException("Unable to select ANGLE's Vulkan driver");
            }
        }
    }

    /**
     * Sets the window title using the graphics API reported by the active
     * context. This also labels an OpenGL fallback when Vulkan was requested.
     *
     * @param context the initialized graphics context
     * @param baseTitle the game title without a renderer label
     * @return the active graphics API name
     */
    public static String setWindowTitleForActiveRenderer(JmeContext context, String baseTitle) {
        RendererInfo renderer = queryActiveRenderer();
        String api = isVulkanRenderer(renderer) ? VULKAN : OPENGL;
        context.setTitle(baseTitle + " - " + api);
        logActiveRenderer(api, renderer);
        return api;
    }

    private static RendererInfo queryActiveRenderer() {
        try {
            GL.getCapabilities();
            RendererInfo renderer = queryOpenGlRenderer();
            return renderer != null ? renderer : new RendererInfo(null, null);
        } catch (IllegalStateException | LinkageError ex) {
            // Only initialize LWJGL's GLES binding when the active context is not desktop OpenGL.
            RendererInfo renderer = queryGlesRenderer();
            return renderer != null ? renderer : new RendererInfo(null, null);
        }
    }

    private static RendererInfo queryGlesRenderer() {
        try {
            String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
            return renderer == null ? null : new RendererInfo(renderer, GLES20.glGetString(GLES20.GL_VERSION));
        } catch (RuntimeException ex) {
            // This context uses desktop OpenGL, so no GLES capabilities are current.
            return null;
        }
    }

    private static RendererInfo queryOpenGlRenderer() {
        try {
            String renderer = GL11.glGetString(GL11.GL_RENDERER);
            return renderer == null ? null : new RendererInfo(renderer, GL11.glGetString(GL11.GL_VERSION));
        } catch (RuntimeException ex) {
            // This context uses GLES (including ANGLE), so no OpenGL capabilities are current.
            return null;
        }
    }

    private static boolean isVulkanRenderer(RendererInfo renderer) {
        String description = (renderer.name() + " " + renderer.version()).toLowerCase(Locale.ROOT);
        return description.contains("vulkan");
    }

    private static void logActiveRenderer(String api, RendererInfo renderer) {
        LOGGER.info(() -> api + " renderer: " + renderer.name() + " (" + renderer.version() + ")");
    }

    private record RendererInfo(String name, String version) {
    }
}
