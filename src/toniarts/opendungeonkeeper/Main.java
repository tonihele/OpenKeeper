package toniarts.opendungeonkeeper;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
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

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {

        // Create main application instance
        final Main app = new Main();

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
            DKConverter frame = new DKConverter(dkIIFolder, app.getAssetManager()) {
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
        Box b = new Box(1, 1, 1);
        Geometry geom = new Geometry("Box", b);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);
//        AssetInfo ai = new AssetInfo(this.getAssetManager(), null) {
//            @Override
//            public InputStream openStream() {
//                try {
//                    final File file = new File("C:\\temp\\OpenDungeonKeeper\\meshes\\Imp.kmf");
//                    key = new AssetKey() {
//                        @Override
//                        public String getName() {
//                            return file.toPath().getFileName().toString();
//                        }
//                    };
//                    return new FileInputStream(file);
//                } catch (FileNotFoundException ex) {
////                    Logger.getLogger(KmfModelLoader.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                return null;
//            }
//        };
//        KmfModelLoader kmfModelLoader = new KmfModelLoader();
//        Node n;
//        try {
//            n = (Node) kmfModelLoader.load(ai);
//
//
//            //Export
//            BinaryExporter exporter = BinaryExporter.getInstance();
//            String currentFolder = Paths.get("").toAbsolutePath().toString();
//
//            //Create an assets folder
//            if (!currentFolder.endsWith(File.separator)) {
//                currentFolder = currentFolder.concat(File.separator);
//            }
//            currentFolder = currentFolder.concat("assets").concat(File.separator).concat("Models").concat(File.separator).concat("Imp.j3o");
//            exporter.save(n, new File(currentFolder));
//        } catch (IOException ex) {
////            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//        }
        // Convert the assets
//        this.getAssetManager().registerLoader(KmfModelLoader.class, "kmf");
//        AssetsConverter.convertAssets(dkIIFolder, assetManager);
//
//        String key = "Models/Imp.j3o";
//
//        //Create an imp on the map
//        Spatial dg = this.getAssetManager().loadModel(new ModelKey(key));
//        rootNode.attachChild(dg);
//
//        //Sound
//        this.getAssetManager().registerLoader(MP2Loader.class, "mp2");
//        audioSource = new AudioNode(assetManager, "Sounds/horng014.mp2", false);
//        audioSource.setLooping(false);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
