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

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.scene.Node;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.audio.plugins.MP2Loader;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.conversion.task.IConversionTaskUpdate;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.modelviewer.MapLoaderAppState;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.utils.SettingUtils;
import toniarts.openkeeper.view.effect.EffectManagerState;

/**
 * Loads an OpenKeeper map into a minimal offscreen renderer and writes configured camera views.
 * No interactive game session, window, input, or audio device is started.
 */
public final class HeadlessScreenshotCapture {

    private static final Logger LOGGER = Logger.getLogger(HeadlessScreenshotCapture.class.getName());
    private static final int SHUTDOWN_TIMEOUT_MINUTES = 10;

    private HeadlessScreenshotCapture() {
    }

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Exception {
        Map<String, String> arguments = parseArguments(args);
        Path mapPath = Path.of(required(arguments, "--map")).toAbsolutePath().normalize();
        Path configPath = Path.of(required(arguments, "--config")).toAbsolutePath().normalize();
        String dkFolder = arguments.get("--dk-folder");
        String mapRootArgument = arguments.get("--map-root");
        String assetsDirectory = arguments.get("--assets-dir");
        if (!Files.isRegularFile(mapPath)) {
            throw new IllegalArgumentException("Map file does not exist: " + mapPath);
        }
        ScreenshotCaptureConfig config = ScreenshotCaptureConfig.load(configPath);
        config.prepareOutputDirectory();
        Path assetsPath = prepareAssets(dkFolder, assetsDirectory);

        Path mapRoot = mapRootArgument == null
                ? dkFolder == null ? inferMapRoot(mapPath) : Path.of(dkFolder).toAbsolutePath().normalize()
                : Path.of(mapRootArgument).toAbsolutePath().normalize();
        IKwdFile map = KwdFile.load(mapRoot.toString(), mapPath);
        OffscreenApplication application = new OffscreenApplication(map, config);
        application.setAssetManager(createAssetManager(assetsPath));
        AppSettings settings = new AppSettings(true);
        if (config.width() != null) {
            settings.setResolution(config.width(), config.height());
        }
        settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        settings.setUseInput(true);
        settings.setUseJoysticks(false);
        settings.setAudioRenderer(null);
        settings.setVSync(false);
        settings.setSamples(0);
        settings.setFrameRate(60);
        application.setSettings(settings);
        application.setShowSettings(false);
        application.setPauseOnLostFocus(false);

        LOGGER.info(() -> "Starting offscreen map capture for " + mapPath);
        application.start(JmeContext.Type.OffscreenSurface);
        if (!application.awaitTermination(SHUTDOWN_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            application.stop();
            throw new IllegalStateException("Offscreen renderer did not finish within the timeout");
        }
        Throwable failure = application.getFailure();
        if (failure != null) {
            throw new IllegalStateException("Headless screenshot generation failed", failure);
        }
    }

    private static AssetManager createAssetManager(Path assetsPath) {
        URL desktopConfig = Thread.currentThread().getContextClassLoader()
                .getResource("com/jme3/asset/Desktop.cfg");
        if (desktopConfig == null) {
            throw new IllegalStateException("JME desktop asset configuration was not found");
        }
        AssetManager assets = JmeSystem.newAssetManager(desktopConfig);
        assets.registerLocator(assetsPath.toString(), FileLocator.class);
        assets.registerLoader(MP2Loader.class, MP2Loader.FILE_EXTENSION);
        return assets;
    }

    private static Path prepareAssets(String dkFolder, String assetsDirectory) throws IOException {
        Path convertedAssets = assetsDirectory == null || assetsDirectory.isBlank()
                ? Path.of(AssetsConverter.getAssetsFolder())
                : Path.of(assetsDirectory).toAbsolutePath().normalize();
        String normalizedDkFolder = null;
        if (dkFolder != null && !dkFolder.isBlank()) {
            normalizedDkFolder = PathUtils.fixFilePath(dkFolder);
            if (!PathUtils.checkDkFolder(normalizedDkFolder)) {
                throw new IllegalArgumentException("Invalid Dungeon Keeper II installation: " + dkFolder);
            }
            PathUtils.setDKIIFolder(normalizedDkFolder);
        }
        if (assetsDirectory != null && !assetsDirectory.isBlank()) {
            if (!Files.isRegularFile(convertedAssets.resolve("Textures/Thumbnails/MapColours.png"))) {
                throw new IllegalStateException("Converted DK II assets were not found at " + convertedAssets);
            }
            return convertedAssets;
        }
        if (dkFolder == null || dkFolder.isBlank()) {
            if (!Files.isRegularFile(convertedAssets.resolve("Textures/Thumbnails/MapColours.png"))) {
                throw new IllegalStateException("Converted DK II assets were not found at " + convertedAssets
                        + ". Supply --assets-dir <Converted-folder> to use existing assets or --dk-folder <install> "
                        + "to convert them for this capture run.");
            }
            return convertedAssets;
        }

        if (AssetsConverter.isConversionNeeded(Main.getSettings())) {
            LOGGER.info("Converting DK II assets for the offscreen renderer");
            URL desktopConfig = Thread.currentThread().getContextClassLoader()
                    .getResource("com/jme3/asset/Desktop.cfg");
            if (desktopConfig == null) {
                throw new IllegalStateException("JME desktop asset configuration was not found");
            }
            AssetManager assetManager = JmeSystem.newAssetManager(desktopConfig);
            assetManager.registerLocator(AssetsConverter.getAssetsFolder(), FileLocator.class);
            AtomicReference<Exception> conversionFailure = new AtomicReference<>();
            AssetsConverter converter = new AssetsConverter(normalizedDkFolder, assetManager) {

                @Override
                public void onUpdateStatus(Integer currentProgress, Integer totalProgress,
                        AssetsConverter.ConvertProcess process) {
                    int reportStep = totalProgress == null ? Integer.MAX_VALUE : Math.max(1, totalProgress / 20);
                    if (currentProgress != null && totalProgress != null
                            && (currentProgress == 0 || currentProgress == totalProgress
                            || currentProgress % reportStep == 0)) {
                        LOGGER.info(() -> process + ": " + currentProgress + "/" + totalProgress);
                    }
                }

                @Override
                public void onComplete(AssetsConverter.ConvertProcess process) {
                    LOGGER.info(() -> "Converted " + process);
                }

                @Override
                public void onError(Exception ex, AssetsConverter.ConvertProcess process) {
                    conversionFailure.compareAndSet(null, ex);
                }
            };
            if (!converter.convertAssets() || conversionFailure.get() != null) {
                throw new IllegalStateException("DK II asset conversion failed", conversionFailure.get());
            }
            SettingUtils.getInstance().saveSettings();
        }

        if (!Files.isRegularFile(convertedAssets.resolve("Textures/Thumbnails/MapColours.png"))) {
            throw new IllegalStateException("DK II asset conversion did not produce the expected textures");
        }
        return convertedAssets;
    }

    private static Path inferMapRoot(Path mapPath) {
        for (Path directory = mapPath.getParent(); directory != null; directory = directory.getParent()) {
            Path editor = directory.getParent();
            Path data = editor == null ? null : editor.getParent();
            if (directory.getFileName() != null && "maps".equalsIgnoreCase(directory.getFileName().toString())
                    && editor != null && "editor".equalsIgnoreCase(editor.getFileName().toString())
                    && data != null && "data".equalsIgnoreCase(data.getFileName().toString())
                    && data.getParent() != null) {
                return data.getParent();
            }
        }
        Path parent = mapPath.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("Map path has no parent; supply --map-root");
        }
        return parent;
    }

    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (!key.startsWith("--") || i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("Expected --map <file> --config <properties> "
                        + "[--dk-folder <install>] [--map-root <root>] [--assets-dir <Converted-folder>]");
            }
            if (result.putIfAbsent(key, args[++i]) != null) {
                throw new IllegalArgumentException("Duplicate option: " + key);
            }
        }
        for (String key : result.keySet()) {
            if (!"--map".equals(key) && !"--config".equals(key)
                    && !"--dk-folder".equals(key) && !"--assets-dir".equals(key)
                    && !"--map-root".equals(key)) {
                throw new IllegalArgumentException("Unknown option: " + key);
            }
        }
        return result;
    }

    private static String required(Map<String, String> arguments, String key) {
        String value = arguments.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option " + key);
        }
        return value;
    }

    static final class OffscreenApplication extends SimpleApplication {

        private final IKwdFile map;
        private final ScreenshotCaptureConfig config;
        private final CountDownLatch stopped = new CountDownLatch(1);
        private volatile Throwable failure;

        private OffscreenApplication(IKwdFile map, ScreenshotCaptureConfig config) {
            super();
            this.map = map;
            this.config = config;
        }

        @Override
        public void simpleInitApp() {
            try {
                getViewPort().setBackgroundColor(new ColorRGBA(0.035f, 0.045f, 0.07f, 1f));
                getRootNode().addLight(new AmbientLight(ColorRGBA.White));

                AssetManager assets = getAssetManager();
                getStateManager().attach(new EffectManagerState(map, assets));

                MapLoaderAppState mapLoader = new MapLoaderAppState();
                getStateManager().attach(mapLoader);

                OffscreenPngCaptureProcessor processor = new OffscreenPngCaptureProcessor();
                getGuiViewPort().addProcessor(processor);
                getStateManager().attach(new HeadlessScreenshotCaptureState(
                        map, mapLoader, config, processor, this));
            } catch (Exception ex) {
                setFailure(ex);
                stop();
            }
        }

        @Override
        public void destroy() {
            try {
                super.destroy();
            } finally {
                stopped.countDown();
            }
        }

        private boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return stopped.await(timeout, unit);
        }

        private Throwable getFailure() {
            return failure;
        }

        void setFailure(Throwable failure) {
            this.failure = failure;
        }
    }
}
