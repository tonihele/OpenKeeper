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
package toniarts.openkeeper.tools.screenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotCaptureConfigTest {

    @TempDir
    Path tempDirectory;

    @Test
    void loadsNamedShotsAndResolvesRelativeOutputDirectory() throws IOException {
        Path configPath = tempDirectory.resolve("capture.properties");
        Files.writeString(configPath, """
                output-dir=captures
                width=1600
                height=900
                startup-frames=90
                shots=hatchery,graveyard
                shot.hatchery.file=hatchery-close.png
                shot.hatchery.position=4,12,8
                shot.hatchery.look-at=4,0,4
                shot.hatchery.fov=38
                shot.hatchery.settle-frames=5
                shot.hatchery.ceiling-visible=false
                shot.graveyard.file=graveyard-wide.png
                shot.graveyard.position=10,22,12
                shot.graveyard.rotation-degrees=30,-40,0
                """);

        ScreenshotCaptureConfig config = ScreenshotCaptureConfig.load(configPath);

        assertEquals(tempDirectory.resolve("captures").toAbsolutePath().normalize(), config.outputDirectory());
        assertEquals(1600, config.width());
        assertEquals(900, config.height());
        assertEquals(90, config.startupFrames());
        assertEquals(2, config.shots().size());
        assertEquals("hatchery-close.png", config.shots().get(0).fileName());
        assertEquals(5, config.shots().get(0).settleFrames());
        assertEquals(false, config.shots().get(0).ceilingVisible());
        assertEquals(true, config.shots().get(1).ceilingVisible(), "ceiling visibility defaults on for compatibility");
        assertNotNull(config.shots().get(0).lookAt());
        assertNull(config.shots().get(0).rotationDegrees());
        assertNull(config.shots().get(1).lookAt());
        assertEquals(30f, config.shots().get(1).rotationDegrees().x);
    }

    @Test
    void rejectsInvalidCameraAndOutputSettings() throws IOException {
        Path configPath = tempDirectory.resolve("invalid.properties");
        Files.writeString(configPath, """
                output-dir=captures
                shots=bad
                shot.bad.file=../outside.png
                shot.bad.position=0,0,0
                shot.bad.look-at=0,0,1
                """);

        assertThrows(IllegalArgumentException.class, () -> ScreenshotCaptureConfig.load(configPath));
    }

    @Test
    void requiresExactlyOneCameraOrientationMode() throws IOException {
        Path configPath = tempDirectory.resolve("missing-orientation.properties");
        Files.writeString(configPath, """
                output-dir=captures
                shots=bad
                shot.bad.file=bad.png
                shot.bad.position=0,0,0
                """);

        assertThrows(IllegalArgumentException.class, () -> ScreenshotCaptureConfig.load(configPath));
    }

    @Test
    void rejectsInvalidCeilingVisibility() throws IOException {
        Path configPath = tempDirectory.resolve("invalid-ceiling-visibility.properties");
        Files.writeString(configPath, """
                output-dir=captures
                shots=bad
                shot.bad.file=bad.png
                shot.bad.position=0,0,0
                shot.bad.look-at=0,0,1
                shot.bad.ceiling-visible=sometimes
                """);

        assertThrows(IllegalArgumentException.class, () -> ScreenshotCaptureConfig.load(configPath));
    }

    @Test
    void preparesOutputDirectoryAndRefusesToOverwriteCapture() throws IOException {
        Path configPath = tempDirectory.resolve("output.properties");
        Files.writeString(configPath, """
                output-dir=captures
                shots=wide
                shot.wide.file=wide.png
                shot.wide.position=0,5,5
                shot.wide.look-at=0,0,0
                """);
        ScreenshotCaptureConfig config = ScreenshotCaptureConfig.load(configPath);

        config.prepareOutputDirectory();
        assertTrue(Files.isDirectory(config.outputDirectory()));
        Path existingCapture = config.outputDirectory().resolve("wide.png");
        Files.writeString(existingCapture, "keep this capture");

        assertThrows(IOException.class, config::prepareOutputDirectory);
        assertEquals("keep this capture", Files.readString(existingCapture));
    }
}
