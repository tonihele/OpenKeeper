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

import com.jme3.math.Vector3f;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import toniarts.openkeeper.view.PossessionCameraState;

/**
 * Configuration for deterministic offscreen map screenshots.
 *
 * @param outputDirectory directory where screenshots are written
 * @param width requested capture width, or {@code null} to use the renderer default
 * @param height requested capture height, or {@code null} to use the renderer default
 * @param startupFrames frames to render after loading the level before moving the camera
 * @param ceilingSubdivisions grid cells per tile edge for the ceiling mesh
 * @param nearClip camera near clipping distance in world units
 * @param shots ordered camera shots to capture
 */
public record ScreenshotCaptureConfig(
        Path outputDirectory,
        Integer width,
        Integer height,
        int startupFrames,
        int ceilingSubdivisions,
        float nearClip,
        List<Shot> shots) {

    public ScreenshotCaptureConfig {
        outputDirectory = outputDirectory.toAbsolutePath().normalize();
        shots = List.copyOf(shots);
    }

    /**
     * Load a screenshot configuration from a Java properties file.
     * Relative output directories are resolved relative to the properties file.
     *
     * @param configFile properties file to load
     * @return parsed screenshot capture configuration
     * @throws IOException if the properties file cannot be read
     */
    public static ScreenshotCaptureConfig load(Path configFile) throws IOException {
        Path absoluteConfigFile = configFile.toAbsolutePath().normalize();
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(absoluteConfigFile)) {
            properties.load(input);
        }

        Path parent = absoluteConfigFile.getParent();
        String outputValue = required(properties, "output-dir");
        Path outputDirectory = Path.of(outputValue);
        if (!outputDirectory.isAbsolute()) {
            outputDirectory = parent.resolve(outputDirectory);
        }

        Integer width = optionalInteger(properties, "width", 1, 8192);
        Integer height = optionalInteger(properties, "height", 1, 8192);
        if ((width == null) != (height == null)) {
            throw new IllegalArgumentException("width and height must either both be set or both be omitted");
        }

        int startupFrames = integer(properties, "startup-frames", 120, 0, 36000);
        int ceilingSubdivisions = integer(properties, "ceiling-subdivisions", 4, 2, 16);
        float nearClip = decimal(properties, "near-clip", PossessionCameraState.VIEW_NEAR_CLIP, 0.001f, 10f);
        String[] ids = required(properties, "shots").split(",");
        if (ids.length == 0) {
            throw new IllegalArgumentException("shots must contain at least one shot id");
        }

        List<Shot> shots = new ArrayList<>(ids.length);
        Set<String> fileNames = new HashSet<>();
        for (String value : ids) {
            String id = value.strip();
            if (!id.matches("[A-Za-z0-9_-]+")) {
                throw new IllegalArgumentException("Invalid shot id: " + id);
            }

            String prefix = "shot." + id + ".";
            String fileName = required(properties, prefix + "file");
            validateFileName(fileName);
            if (!fileNames.add(fileName.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate screenshot filename: " + fileName);
            }

            Vector3f position = vector(properties, prefix + "position");
            Vector3f lookAt = optionalVector(properties, prefix + "look-at");
            Vector3f rotation = optionalVector(properties, prefix + "rotation-degrees");
            if ((lookAt == null) == (rotation == null)) {
                throw new IllegalArgumentException(prefix
                        + " must define exactly one of look-at or rotation-degrees");
            }

            float fov = decimal(properties, prefix + "fov", 45f, 1f, 179f);
            int settleFrames = integer(properties, prefix + "settle-frames", 3, 1, 36000);
            boolean ceilingVisible = bool(properties, prefix + "ceiling-visible", true);
            shots.add(new Shot(id, fileName, position, lookAt, rotation, fov, settleFrames, ceilingVisible));
        }

        return new ScreenshotCaptureConfig(outputDirectory, width, height, startupFrames,
                ceilingSubdivisions, nearClip, shots);
    }

    /**
     * Validate output files and create the output directory before starting the renderer.
     * Existing files are left untouched.
     *
     * @throws IOException if the output directory cannot be created or an output already exists
     */
    public void prepareOutputDirectory() throws IOException {
        Files.createDirectories(outputDirectory);
        for (Shot shot : shots) {
            Path outputFile = outputDirectory.resolve(shot.fileName());
            if (Files.exists(outputFile)) {
                throw new IOException("Refusing to overwrite existing screenshot: " + outputFile);
            }
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required screenshot setting: " + key);
        }
        return value.strip();
    }

    private static Integer optionalInteger(Properties properties, String key, int min, int max) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? null : parseInteger(key, value.strip(), min, max);
    }

    private static int integer(Properties properties, String key, int defaultValue, int min, int max) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : parseInteger(key, value.strip(), min, max);
    }

    private static int parseInteger(String key, String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException(key + " must be between " + min + " and " + max);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be an integer", ex);
        }
    }

    private static boolean bool(Properties properties, String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(value.strip())) {
            return true;
        }
        if ("false".equalsIgnoreCase(value.strip())) {
            return false;
        }
        throw new IllegalArgumentException(key + " must be true or false");
    }

    private static float decimal(Properties properties, String key, float defaultValue, float min, float max) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            float parsed = Float.parseFloat(value.strip());
            if (!Float.isFinite(parsed) || parsed < min || parsed > max) {
                throw new IllegalArgumentException(key + " must be between " + min + " and " + max);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be a number", ex);
        }
    }

    private static Vector3f vector(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required screenshot setting: " + key);
        }
        return parseVector(key, value);
    }

    private static Vector3f optionalVector(Properties properties, String key) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? null : parseVector(key, value);
    }

    private static Vector3f parseVector(String key, String value) {
        String[] components = value.split(",");
        if (components.length != 3) {
            throw new IllegalArgumentException(key + " must have three comma-separated components");
        }
        try {
            float x = Float.parseFloat(components[0].strip());
            float y = Float.parseFloat(components[1].strip());
            float z = Float.parseFloat(components[2].strip());
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                throw new IllegalArgumentException(key + " components must be finite numbers");
            }
            return new Vector3f(x, y, z);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must have numeric components", ex);
        }
    }

    private static void validateFileName(String fileName) {
        if (!fileName.matches("(?i)[A-Za-z0-9][A-Za-z0-9 _.-]*\\.png")) {
            throw new IllegalArgumentException("Screenshot file must be a simple .png filename: " + fileName);
        }
    }

    /**
     * One camera setup and output file.
     * Rotation is yaw, pitch, roll in degrees when look-at is omitted.
     *
     * @param id shot id from the properties file
     * @param fileName screenshot file name
     * @param position camera position in world coordinates
     * @param lookAt world-space target, or {@code null} for an Euler rotation
     * @param rotationDegrees yaw, pitch, roll in degrees, or {@code null} for look-at
     * @param fovY vertical field of view in degrees
     * @param settleFrames frames to render after setting this camera
     * @param ceilingVisible whether the map ceiling is shown for this camera
     */
    public record Shot(
            String id,
            String fileName,
            Vector3f position,
            Vector3f lookAt,
            Vector3f rotationDegrees,
            float fovY,
            int settleFrames,
            boolean ceilingVisible) {

        public Shot {
            position = position.clone();
            lookAt = lookAt == null ? null : lookAt.clone();
            rotationDegrees = rotationDegrees == null ? null : rotationDegrees.clone();
        }

        @Override
        public Vector3f position() {
            return position.clone();
        }

        @Override
        public Vector3f lookAt() {
            return lookAt == null ? null : lookAt.clone();
        }

        @Override
        public Vector3f rotationDegrees() {
            return rotationDegrees == null ? null : rotationDegrees.clone();
        }
    }
}
