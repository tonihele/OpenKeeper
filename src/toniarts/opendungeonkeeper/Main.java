package toniarts.opendungeonkeeper;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import toniarts.opendungeonkeeper.audio.plugins.MP2Loader;
import toniarts.opendungeonkeeper.cinematics.CameraSweepDataLoader;
import toniarts.opendungeonkeeper.game.state.GameState;
import toniarts.opendungeonkeeper.game.state.MainMenuState;
import toniarts.opendungeonkeeper.gui.CursorFactory;
import toniarts.opendungeonkeeper.setup.DKConverter;
import toniarts.opendungeonkeeper.setup.DKFolderSelector;
import toniarts.opendungeonkeeper.setup.IFrameClosingBehavior;

/**
 * Main entry point of Open Dungeon Keeper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Main extends SimpleApplication {

    private static String dkIIFolder;
    private static boolean conversionDone = false;
    private static int conversionVersion = 0;
    private static boolean folderOk = false;
    private static boolean conversionOk = false;
    private final static String SETTINGS_FILE = "odk.properties";
    private final static String TITLE = "Open Dungeon Keeper";
    private final static int MAX_FPS = 90;
    private final static String DKII_FOLDER_KEY = "Dungeon Keeper II folder";
    private final static String CONVERSION_DONE_KEY = "Conversion done";
    private final static String CONVERSION_VERSION_KEY = "Conversion version";
    private final static int CONVERSION_VERSION = 1;
    private final static String TEST_FILE = "Data".concat(File.separator).concat("editor").concat(File.separator).concat("maps").concat(File.separator).concat("FrontEnd3DLevel.kwd");
    private static final Object lock = new Object();
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private final HashMap<String, String> params;

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {

        // Create main application instance
        final Main app = new Main(parseArguments(args));

        // Read settings and convert resources if needed
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
     * @return parameters as parameter / value -map
     */
    public static HashMap<String, String> parseArguments(String[] args) {
        HashMap<String, String> params = new HashMap<>(args.length);

        // Go through the params
        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {

            // If the next parameter doesn't have a "-", it is the value for the param
            String value = null;
            if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                value = args[i + 1];
            }

            // Add it
            params.put(args[i].substring(1).toLowerCase(), value);

            i++;
        }

        return params;
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
        if (!checkDkFolder(dkIIFolder)) {
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
                    app.settings.putString(DKII_FOLDER_KEY, dkIIFolder);
                    folderOk = true;
                }
            };
            openFrameAndWait(frame);
        } else {
            folderOk = true;
        }

        // If the folder is ok, check the conversion
        if (folderOk && (conversionVersion < CONVERSION_VERSION || !conversionDone)) {
            logger.info("Need to convert the assets!");
            saveSetup = true;

            // Convert
            setLookNFeel();
            AssetManager assetManager = JmeSystem.newAssetManager(
                    Thread.currentThread().getContextClassLoader()
                    .getResource("com/jme3/asset/Desktop.cfg")); // Get temporary asset manager instance since we not yet have one ourselves
            DKConverter frame = new DKConverter(dkIIFolder, assetManager) {
                @Override
                protected void continueOk() {
                    app.settings.putInteger(CONVERSION_VERSION_KEY, CONVERSION_VERSION);
                    app.settings.putBoolean(CONVERSION_DONE_KEY, true);
                    conversionOk = true;
                }
            };
            openFrameAndWait(frame);
        } else if (folderOk) {
            conversionOk = true;
        }

        // If everything is ok, we might need to save the setup
        boolean result = (folderOk && conversionOk);
        if (result && saveSetup) {
            try {
                app.settings.save(new FileOutputStream(new File(SETTINGS_FILE)));
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Settings file failed to save!", ex);
            }
        }

        return result;
    }

    public Main(HashMap<String, String> params) {
        super();

        this.params = params;
    }

    private static void initSettings(Main app) {
        AppSettings setup = new AppSettings(true);

        //Default resolution
        setup.setResolution(800, 600);
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            try {
                setup.load(new FileInputStream(settingsFile));
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Settings file failed to load!", ex);
            }
        }
        setup.setTitle(TITLE);
        setup.setFrameRate(Math.max(MAX_FPS, setup.getFrequency()));

        //FIXME: These currently just destroy everything
        setup.setRenderer(AppSettings.LWJGL_OPENGL2);
        setup.setSamples(1);
        setup.setStereo3D(false);

        // DKII settings
        dkIIFolder = setup.getString(DKII_FOLDER_KEY);
        conversionDone = setup.getBoolean(CONVERSION_DONE_KEY);
        conversionVersion = setup.getInteger(CONVERSION_VERSION_KEY);

        // The icons
        setup.setIcons(getApplicationIcons());

        app.settings = setup;
    }

    /**
     * Checks the DK 2 folder validity
     *
     * @param folder the supposed DK II folder
     * @return true if the folder is valid
     */
    public static boolean checkDkFolder(String folder) {

        // Throw a simple test to the folder, try to find a test file
        if (folder != null && !folder.isEmpty() && new File(folder).exists()) {
            if (!folder.endsWith(File.separator)) {
                folder = folder.concat(File.separator);
            }
            File testFile = new File(folder.concat(TEST_FILE));
            return testFile.exists();
        }

        // Better luck next time
        return false;
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
                if (frame instanceof IFrameClosingBehavior) {
                    if (!((IFrameClosingBehavior) frame).canCloseWindow()) {
                        return; // You shall not pass!
                    }
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

        // Asset loaders
        // Sound
        this.getAssetManager().registerLoader(MP2Loader.class, "mp2");
        // Camera sweep files
        this.getAssetManager().registerLoader(CameraSweepDataLoader.class, CameraSweepDataLoader.CAMERA_SWEEP_DATA_FILE_EXTENSION);

        if (params.containsKey("level")) {
            GameState gameState = new GameState(params.get("level"), this.getAssetManager());
            stateManager.attach(gameState);
        } else {

            // Initialize the main menu state
            MainMenuState mainMenu = new MainMenuState();
            stateManager.attach(mainMenu);
        }
    }

    /**
     * Get the application icons
     *
     * @return array of application icons
     */
    public static BufferedImage[] getApplicationIcons() {
        try {
            return new BufferedImage[]{ImageIO.read(CursorFactory.class.getResource("icons/odk256.png")),
                ImageIO.read(CursorFactory.class.getResource("icons/odk128.png")),
                ImageIO.read(CursorFactory.class.getResource("icons/odk32.png")),
                ImageIO.read(CursorFactory.class.getResource("icons/odk16.png"))};
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
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
