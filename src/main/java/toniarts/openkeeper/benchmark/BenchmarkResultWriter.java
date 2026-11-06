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
package toniarts.openkeeper.benchmark;

import com.jme3.app.Application;
import com.jme3.system.AppSettings;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import toniarts.openkeeper.Main;

/**
 * Assembles and writes the structured benchmark JSON document.
 */
public final class BenchmarkResultWriter {

    public static final int SCHEMA_VERSION = 2;
    public static final String BENCHMARK_ID = "openkeeper-late-game";

    private BenchmarkResultWriter() {
    }

    public static Map<String, Object> buildDocument(BenchmarkConfig config,
            Map<String, Object> scenario, Map<String, Object> metrics,
            Application app, String phaseNotes) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("benchmark", BENCHMARK_ID);
        root.put("frameworkVersion", BenchmarkConfig.FRAMEWORK_VERSION);
        root.put("timestamp", Instant.now().toString());
        root.put("engine", engineInfo());
        root.put("build", buildInfo());
        root.put("hardware", hardwareInfo(app));
        root.put("parameters", config.toMap());
        root.put("scenario", scenario);
        root.put("metrics", metrics);
        if (phaseNotes != null) {
            root.put("notes", phaseNotes);
        }
        return root;
    }

    public static Path write(Path outputPath, Map<String, Object> document) throws IOException {
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String json = BenchmarkJson.toJson(document);
        Files.writeString(outputPath, json, StandardCharsets.UTF_8);
        return outputPath.toAbsolutePath();
    }

    private static Map<String, Object> engineInfo() {
        Map<String, Object> engine = new LinkedHashMap<>();
        engine.put("name", Main.TITLE);
        engine.put("version", Main.VERSION);
        engine.put("jmonkeyengine", "3.9.0-stable");
        engine.put("javaVersion", System.getProperty("java.version"));
        engine.put("javaVendor", System.getProperty("java.vendor"));
        engine.put("javaVmName", System.getProperty("java.vm.name"));
        engine.put("osName", System.getProperty("os.name"));
        engine.put("osArch", System.getProperty("os.arch"));
        engine.put("osVersion", System.getProperty("os.version"));
        return engine;
    }

    private static Map<String, Object> buildInfo() {
        Map<String, Object> build = new LinkedHashMap<>();
        Attributes attrs = readManifestAttributes();
        if (attrs != null) {
            putAttr(build, "buildTimestamp", attrs, "Build-Timestamp");
            putAttr(build, "builtBy", attrs, "Built-By");
            putAttr(build, "createdBy", attrs, "Created-By");
            putAttr(build, "buildJdk", attrs, "Build-JDK");
            putAttr(build, "buildOs", attrs, "Build-OS");
            putAttr(build, "implementationTitle", attrs, Attributes.Name.IMPLEMENTATION_TITLE.toString());
        }
        build.put("workingDirectory", Path.of("").toAbsolutePath().toString().replace('\\', '/'));
        return build;
    }

    private static Map<String, Object> hardwareInfo(Application app) {
        Map<String, Object> hw = new LinkedHashMap<>();
        hw.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        hw.put("maxMemoryMb", Math.round(Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0) * 1000.0) / 1000.0);
        try {
            hw.put("systemLoadAverage", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        } catch (Exception ignored) {
            hw.put("systemLoadAverage", -1.0);
        }
        if (app != null && app.getContext() != null && app.getContext().getSettings() != null) {
            AppSettings settings = app.getContext().getSettings();
            hw.put("renderer", settings.getRenderer());
            hw.put("width", settings.getWidth());
            hw.put("height", settings.getHeight());
            hw.put("fullscreen", settings.isFullscreen());
            hw.put("samples", settings.getSamples());
            hw.put("vsync", settings.isVSync());
            hw.put("frameRateCap", settings.getFrameRate());
            hw.put("frequency", settings.getFrequency());
        }
        String glRenderer = System.getProperty("jme.renderer");
        if (glRenderer != null) {
            hw.put("jmeRendererProperty", glRenderer);
        }
        return hw;
    }

    private static Attributes readManifestAttributes() {
        try {
            var resources = Main.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                try (InputStream in = resources.nextElement().openStream()) {
                    Attributes attrs = new Manifest(in).getMainAttributes();
                    String mainClass = attrs.getValue(Attributes.Name.MAIN_CLASS);
                    if (mainClass != null && mainClass.contains("toniarts.openkeeper.Main")) {
                        return attrs;
                    }
                    String title = attrs.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                    if (title != null && title.equalsIgnoreCase(Main.TITLE)) {
                        return attrs;
                    }
                }
            }
        } catch (IOException ex) {
            return null;
        }
        return null;
    }

    private static void putAttr(Map<String, Object> target, String key, Attributes attrs, String name) {
        String value = attrs.getValue(name);
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }
}
