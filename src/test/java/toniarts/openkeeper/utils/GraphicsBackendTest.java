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
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Settings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphicsBackendTest {

    @Test
    void mapsAngleRendererToVulkanOption() {
        assertEquals(GraphicsBackend.VULKAN_OPTION, GraphicsBackend.toOption(AppSettings.ANGLE_GLES3));
        assertEquals(AppSettings.ANGLE_GLES3, GraphicsBackend.toRenderer(GraphicsBackend.VULKAN_OPTION));
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.ANGLE_GLES3);
        assertTrue(GraphicsBackend.isVulkan(settings));
    }

    @Test
    void preservesOpenGlOptionsAlongsideVulkan() {
        var options = GraphicsBackend.getRendererOptions();

        assertTrue(options.contains(AppSettings.LWJGL_OPENGL3));
        assertTrue(options.contains(AppSettings.LWJGL_OPENGL33));
        assertEquals(GraphicsBackend.isVulkanPlatformSupported(),
                options.contains(GraphicsBackend.VULKAN_OPTION));
        assertFalse(options.contains(AppSettings.ANGLE_GLES3));
        assertFalse(Settings.OPENGL.contains(GraphicsBackend.VULKAN_OPTION));
    }

    @Test
    void supportsVulkanOnlyOnBundledWindowsX64Platform() {
        assertTrue(GraphicsBackend.isVulkanPlatformSupported("Windows 11", "amd64"));
        assertTrue(GraphicsBackend.isVulkanPlatformSupported("Windows 10", "x86_64"));
        assertFalse(GraphicsBackend.isVulkanPlatformSupported("Linux", "amd64"));
        assertFalse(GraphicsBackend.isVulkanPlatformSupported("Windows 11", "aarch64"));
    }
}
