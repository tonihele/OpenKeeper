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
package toniarts.openkeeper;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.app.state.VideoRecorderAppState;
import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import de.lessvoid.nifty.render.batch.BatchRenderConfiguration;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import toniarts.openkeeper.audio.plugins.MP2Loader;
import toniarts.openkeeper.cinematics.CameraSweepDataLoader;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.game.state.MainMenuState;
import toniarts.openkeeper.game.state.PlayerState;
import toniarts.openkeeper.game.state.loading.TitleScreenState;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.setup.DKConverter;
import toniarts.openkeeper.setup.DKFolderSelector;
import toniarts.openkeeper.setup.IFrameClosingBehavior;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.utils.SettingUtils;
import toniarts.openkeeper.utils.UTF8Control;
import toniarts.openkeeper.video.MovieState;

/**
 * Main entry point of OpenKeeper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Main extends SimpleApplication {

    private static String dkIIFolder;
    private static boolean folderOk = false;
    private static boolean conversionOk = false;
    public final static String TITLE = "OpenKeeper";
    private final static String USER_HOME_FOLDER = System.getProperty("user.home").concat(File.separator).concat(".").concat(TITLE).concat(File.separator);
    private final static String SCREENSHOTS_FOLDER = USER_HOME_FOLDER.concat("SCRSHOTS").concat(File.separator);
    private static final Object lock = new Object();
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static HashMap<String, String> params;
    private static boolean debug;
    private NiftyJmeDisplay nifty;
    private Settings userSettings;

    private Main() {
        super(new StatsAppState(), new DebugKeysAppState());
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {

        // Create main application instance
        parseArguments(args);
        debug = params.containsKey("debug");
        final Main app = new Main();
        app.setPauseOnLostFocus(false);

        // Read settings and convert resources if needed
        app.showSettings = false;
        initSettings(app);

        // Finally start it if everything went ok
        if (checkSetup(app)) {
            app.start();
        } else {
            logger.warning("Application setup not complete!!");
        }
    }

    /**
     * Parse application parameters
     *
     * @param args the arguments list
     */
    private static void parseArguments(String[] args) {
        params = new HashMap<>(args.length);

        // Go through the params
        int i = 0;
        while (i < args.length) {

            // Skip values etc
            if (!args[i].startsWith("-")) {
                i++;
                continue;
            }

            // If the next parameter doesn't have a "-", it is the value for the param
            String value = null;
            if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                value = args[i + 1];
            }

            // Add it
            params.put(args[i].substring(1).toLowerCase(), value);

            i++;
        }
    }

    /**
     * Check that we have all we need to run this app
     *
     * @param app the app (we need asset managers etc.)
     * @return true if the app is ok for running!
     * @throws InterruptedException lots of threads waiting
     */
    private static boolean checkSetup(final Main app) throws InterruptedException {

        boolean saveSetup = false;

        // First and foremost, the folder
        if (!SettingUtils.checkDkFolder(dkIIFolder)) {
            logger.info("Dungeon Keeper II folder not found or valid! Prompting user!");
            saveSetup = true;

            // Let the user select
            setLookNFeel();
            DKFolderSelector frame = new DKFolderSelector() {
                @Override
                protected void continueOk(String path) {
                    if (!path.endsWith(File.separator)) {
                        dkIIFolder = path.concat(File.separator);
                    }
                    SettingUtils.setDKIIFolder(dkIIFolder);
                    folderOk = true;
                }
            };
            openFrameAndWait(frame);
        } else {
            folderOk = true;
        }

        // If the folder is ok, check the conversion
        if (folderOk && (AssetsConverter.conversionNeeded(app.getSettings()))) {
            logger.info("Need to convert the assets!");
            saveSetup = true;

            // Convert
            setLookNFeel();
            AssetManager assetManager = JmeSystem.newAssetManager(
                    Thread.currentThread().getContextClassLoader()
                    .getResource("com/jme3/asset/Desktop.cfg")); // Get temporary asset manager instance since we not yet have one ourselves
            assetManager.registerLocator(AssetsConverter.getAssetsFolder(), FileLocator.class);
            DKConverter frame = new DKConverter(dkIIFolder, assetManager) {
                @Override
                protected void continueOk() {
                    AssetsConverter.setConversionSettings(app.getSettings());
                    conversionOk = true;
                }
            };
            openFrameAndWait(frame);
        } else if (folderOk) {
            conversionOk = true;
        }

        // If everything is ok, we might need to save the setup
        boolean result = folderOk && conversionOk;
        if (result && saveSetup) {
            SettingUtils.saveSettings();
        }

        return result;
    }

    private static void initSettings(Main app) {

        // Create some folders
        new File(USER_HOME_FOLDER).mkdirs();
        new File(SCREENSHOTS_FOLDER).mkdirs();

        // Init the user settings (which in JME are app settings)
        app.getUserSettings();

        // DKII settings
        dkIIFolder = SettingUtils.getDKIIFolder();
    }

    /**
     * The user settings, main settings
     *
     * @return the user settings
     */
    public Settings getUserSettings() {
        if (userSettings == null) {
            settings = new AppSettings(true);
            userSettings = Settings.getInstance(settings);

            // Assing some app level settings
            userSettings.getAppSettings().setTitle(TITLE);
            userSettings.getAppSettings().setIcons(getApplicationIcons());
        }
        return userSettings;
    }

    /**
     * Get the application settings, global OpenKeeper settings, nothing much
     * here
     *
     * @see #getUserSettings()
     * @return application settings
     */
    public AppSettings getSettings() {
        return SettingUtils.getSettings();
    }

    /**
     * Sets SWING look and feel
     */
    private static void setLookNFeel() {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DKFolderSelector.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DKFolderSelector.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DKFolderSelector.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DKFolderSelector.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
    }

    /**
     * Opens up a given frame and waits for it to finish
     *
     * @param frame the frame to open
     * @throws InterruptedException
     */
    private static void openFrameAndWait(final JFrame frame) throws InterruptedException {
        frame.setVisible(true);

        Thread t = new Thread() {
            @Override
            public void run() {
                synchronized (lock) {
                    while (frame.isVisible()) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            logger.warning("Lock interrupted!");
                        }
                    }
                }
            }
        };
        t.start();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {

                // See if it is allowed
                if (frame instanceof IFrameClosingBehavior && !((IFrameClosingBehavior) frame).canCloseWindow()) {
                    return; // You shall not pass!
                }

                synchronized (lock) {
                    frame.dispose();
                    lock.notify();
                }
            }
        });

        // Special
        if (frame instanceof DKConverter) {
            ((DKConverter) frame).startConversion();
        }

        // Wait the dialog to finish
        t.join();
    }

    @Override
    public void simpleInitApp() {

        // Distribution locator
        getAssetManager().registerLocator(AssetsConverter.getAssetsFolder(), FileLocator.class);

        // Initiate the title screen
        TitleScreenState gameLoader = new TitleScreenState() {
            @Override
            public Void onLoad() {
                try {
                    long startTime = System.currentTimeMillis();

                    // Asset loaders
                    // Sound
                    getAssetManager().registerLoader(MP2Loader.class, "mp2");
                    // Camera sweep files
                    getAssetManager().registerLoader(CameraSweepDataLoader.class, CameraSweepDataLoader.CAMERA_SWEEP_DATA_FILE_EXTENSION);

                    // Set the anisotropy asset listener
                    setAnisotropy();

                    // Allow people to take screenshots
                    ScreenshotAppState screenShotState = new ScreenshotAppState(SCREENSHOTS_FOLDER);
                    stateManager.attach(screenShotState);

                    // Recording video
                    if (params.containsKey("record")) {
                        float quality = getUserSettings().getSettingFloat(Settings.Setting.RECORDER_QUALITY);
                        int frameRate = getUserSettings().getSettingInteger(Settings.Setting.RECORDER_FPS);
                        getSettings().setFrameRate(frameRate);
                        VideoRecorderAppState recorder = new VideoRecorderAppState(quality, frameRate);
                        String folder = params.get("record");
                        if (folder == null) {
                            folder = SCREENSHOTS_FOLDER;
                        }
                        if (!folder.endsWith(File.separator)) {
                            folder = folder.concat(File.separator);
                        }
                        folder = folder.concat(getSettings().getTitle()).concat("-").concat(String.valueOf(System.currentTimeMillis() / 1000)).concat(".avi");
                        recorder.setFile(new File(folder));

                        stateManager.attach(recorder);
                    }

                    // Nifty
                    NiftyJmeDisplay niftyDisplay = getNifty();

                    // Validate the XML, great for debuging purposes
                    List<String> guiXMLs = Arrays.asList("Interface/MainMenu.xml", "Interface/GameHUD.xml");
                    for (String xml : guiXMLs) {
                        try {
//                            niftyDisplay.getNifty().validateXml(xml); <-- Amazingly buggy?
                        } catch (Exception e) {
                            throw new RuntimeException("GUI file " + xml + " failed to validate!", e);
                        }
                    }

                    // Initialize persistent app states
                    MainMenuState mainMenuState = new MainMenuState(!params.containsKey("level"), assetManager);
                    mainMenuState.setEnabled(false);
                    PlayerState playerState = new PlayerState(Keeper.KEEPER1_ID, false);

                    stateManager.attach(mainMenuState);
                    stateManager.attach(playerState);

                    // Eventually we are going to use Nifty, the XML files take some time to parse
                    niftyDisplay.getNifty().registerScreenController(mainMenuState, playerState);
                    for (String xml : guiXMLs) {
                        niftyDisplay.getNifty().addXml(xml);
                    }

                    // It is all a clever ruge, we don't actually load much here
                    if (!params.containsKey("nomovies") && !params.containsKey("level")) {
                        long waitTime = 5000 - (System.currentTimeMillis() - startTime);
                        if (waitTime > 0) {
                            Thread.sleep(waitTime);
                        }
                    }
                } catch (InterruptedException ex) {
                    // Doesn't matter
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to load the game!", e);
                    app.stop();
                }
                return null;
            }

            @Override
            public void onLoadComplete() {

                // FIXME: We need ambient light, but it may be different for different states. There just seems to be a bug in BatchNodes concerning the removal of the light. So this is temporary perhaps
                AmbientLight al = new AmbientLight();
                al.setColor(ColorRGBA.White);
                rootNode.addLight(al);

                if (params.containsKey("nomovies") || params.containsKey("level")) {
                    startGame();
                } else {

                    // The fireworks!
                    playIntro();
                }
            }
        };
        this.stateManager.attach(gameLoader);
    }

    /**
     * Adds an asset listener to the asset manager that automatically sets
     * anisotropy level to any textures loaded
     */
    private void setAnisotropy() {
        AssetEventListener asl = new AssetEventListener() {
            @Override
            public void assetLoaded(AssetKey key) {
            }

            @Override
            public void assetRequested(AssetKey key) {
                if (key.getExtension().equals("png") || key.getExtension().equals("jpg") || key.getExtension().equals("dds")) {
                    TextureKey tkey = (TextureKey) key;
                    tkey.setAnisotropy(getUserSettings().getSettingInteger(Settings.Setting.ANISOTROPY));
                }
            }

            @Override
            public void assetDependencyNotFound(AssetKey parentKey, AssetKey dependentAssetKey) {
            }
        };
        assetManager.addAssetEventListener(asl);
    }

    /**
     * Get the application icons
     *
     * @return array of application icons
     */
    public static BufferedImage[] getApplicationIcons() {
        try {
            return new BufferedImage[]{ImageIO.read(CursorFactory.class.getResource("icons/openkeeper256.png")),
                ImageIO.read(CursorFactory.class.getResource("icons/openkeeper128.png")),
                ImageIO.read(CursorFactory.class.getResource("icons/openkeeper64.png")),
                ImageIO.read(CursorFactory.class.getResource("icons/openkeeper48.png")),
                ImageIO.read(CursorFactory.class.getResource("icons/openkeeper32.png")),
                ImageIO.read(CursorFactory.class.getResource("icons/openkeeper24.png")),
                ImageIO.read(CursorFactory.class.getResource("icons/openkeeper16.png"))};
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to load the application icons!", ex);
        }
        return null;
    }

    /**
     * Gets the DK II folder, convenience method (this is also stored in the
     * settings)
     *
     * @return the DK II folder
     */
    public static String getDkIIFolder() {
        return dkIIFolder;
    }

    @Override
    public void restart() {
        try {
            settings = getUserSettings().getAppSettings();
            super.restart();

            // FIXME: This should go to handle error
            try {

                // Continue to save the settings
                getUserSettings().save();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Can not save the settings!", ex);
            }

        } catch (Exception e) {
            // Failed to restart
//            initSettings(this);
//            restart();
        }
    }

    /**
     * (re-)Sets scene processors to the view port
     */
    public void setViewProcessors() {

        // Clear the old ones
        viewPort.clearProcessors();

        // Add SSAO
        if (getUserSettings().getSettingBoolean(Settings.Setting.SSAO)) {
            FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
            SSAOFilter ssaoFilter = new SSAOFilter(getUserSettings().getSettingFloat(Settings.Setting.SSAO_SAMPLE_RADIUS),
                    getUserSettings().getSettingFloat(Settings.Setting.SSAO_INTENSITY),
                    getUserSettings().getSettingFloat(Settings.Setting.SSAO_SCALE),
                    getUserSettings().getSettingFloat(Settings.Setting.SSAO_BIAS));
            fpp.addFilter(ssaoFilter);
            viewPort.addProcessor(fpp);
        }
    }

    /**
     * Gets current locale's resource bundle (UTF-8)
     *
     * @param baseName base name of the bundle
     * @return the resource bundle
     */
    public static ResourceBundle getResourceBundle(String baseName) {
        File file = new File(AssetsConverter.getAssetsFolder());
        try {
            URL[] urls = {file.toURI().toURL()};
            ClassLoader loader = new URLClassLoader(urls);
            return ResourceBundle.getBundle(baseName, Locale.getDefault(), loader, new UTF8Control());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to locate the resource bundle " + baseName + " in " + file + "!", e);
        }

        // Works only from the IDE
        return ResourceBundle.getBundle(baseName, new UTF8Control());
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    /**
     * Starts the game, opens up the start menu / level
     */
    private void startGame() {
        if (params.containsKey("level")) {
            GameState gameState = new GameState(params.get("level"));
            stateManager.attach(gameState);
        } else {

            // Enable the start menu
            stateManager.getState(MainMenuState.class).setEnabled(true);
        }
    }

    /**
     * Plays the intro movies, after which the game is started
     */
    private void playIntro() {

        // The intro sequence
        Queue<String> introSequence = new LinkedList<>();
        introSequence.add(getDkIIFolder().concat("Data".concat(File.separator).concat("Movies").concat(File.separator).concat("BullfrogIntro.tgq")));
        introSequence.add(getDkIIFolder().concat("Data".concat(File.separator).concat("Movies").concat(File.separator).concat("INTRO.TGQ")));
        playMovie(introSequence);
    }

    /**
     * Plays single movie, and advances to the next or if not available, starts
     * the game
     *
     * @param introSequence movie queue
     */
    private void playMovie(final Queue<String> introSequence) {

        String movieFile = introSequence.poll();
        if (movieFile != null) {
            try {
                MovieState movieState = new MovieState(movieFile) {
                    @Override
                    protected void onPlayingEnd() {
                        playMovie(introSequence);
                    }
                };
                stateManager.attach(movieState);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to initiate playing " + movieFile + "!", e);

                // Continue with the movie list
                playMovie(introSequence);
            }
        } else {
            startGame();
        }
    }

    /**
     * Get Nifty, warning this also initializes and attaches Nifty (if not
     * initialized already) and is not synchronized
     *
     * @return the Nifty instance
     */
    public NiftyJmeDisplay getNifty() {
        if (nifty == null) {

            // Batching
            BatchRenderConfiguration config = new BatchRenderConfiguration();
            config.atlasHeight = 2048;
            config.atlasWidth = 2048;
            config.fillRemovedImagesInAtlas = false;
            config.disposeImagesBetweenScreens = false;
            config.useHighQualityTextures = true;

            // Init Nifty
            nifty = NiftyJmeDisplay.newNiftyJmeDisplay(assetManager,
                    inputManager,
                    getAudioRenderer(),
                    getGuiViewPort(), config);

            // Unfortunate Nifty hack, see https://github.com/nifty-gui/nifty-gui/issues/414
            nifty.getNifty().setLocale(Locale.ROOT);

            // Attach the nifty display to the gui view port as a processor
            getGuiViewPort().addProcessor(nifty);
        }
        return nifty;
    }

    @Override
    public void stop() {
        super.stop();

        // https://github.com/jMonkeyEngine/jmonkeyengine/issues/330 :(
        System.exit(0);
    }

    /**
     * Whether the debug flag is on
     *
     * @return debug
     */
    public static boolean isDebug() {
        return debug;
    }

}
