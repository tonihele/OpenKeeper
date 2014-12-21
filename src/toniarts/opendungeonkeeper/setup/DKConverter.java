/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.setup;

import com.jme3.asset.AssetManager;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import javax.swing.JOptionPane;
import toniarts.opendungeonkeeper.Main;
import toniarts.opendungeonkeeper.tools.convert.AssetsConverter;

/**
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class DKConverter extends javax.swing.JFrame implements IFrameClosingBehavior {

    private static volatile boolean convertDone = false;
    private final String dungeonKeeperFolder;
    private final AssetManager assetManager;

    /**
     * Creates new form DKConverter
     */
    public DKConverter(String dungeonKeeperFolder, AssetManager assetManager) {
        initComponents();

        this.dungeonKeeperFolder = dungeonKeeperFolder;
        this.assetManager = assetManager;
        setIconImages(Arrays.asList(Main.getApplicationIcons()));
    }

    /**
     * Call to start the conversion
     */
    public void startConversion() {

        // Start conversion
        Converter converter = new Converter(dungeonKeeperFolder, assetManager, this);
        converter.start();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        conversionProgressBar = new javax.swing.JProgressBar();
        conversionStatusLabel = new javax.swing.JLabel();
        totalProgressLabel = new javax.swing.JLabel();
        totalProgressBar = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Converting files... Please wait...");
        setResizable(false);

        jLabel1.setText("<html>Open Dungeon Keeper needs the original Dungeon Keeper II files in order to work. Some of these files needs to be converted by the Open Dungeon Keeper to make them usable. This process might take awhile. It is however not done every time you start Open Dungeon Keeper, only when the process is changed due a version change etc.</html>");

        conversionStatusLabel.setText("Converting files:");

        totalProgressLabel.setText("Total progress:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(conversionProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                    .addComponent(totalProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(totalProgressLabel)
                            .addComponent(conversionStatusLabel))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(conversionStatusLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(conversionProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(totalProgressLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(totalProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(112, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws InterruptedException, InvocationTargetException {
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
            java.util.logging.Logger.getLogger(DKConverter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DKConverter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DKConverter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DKConverter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                new DKConverter(null, null) {
                    @Override
                    protected void continueOk() {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                }.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JProgressBar conversionProgressBar;
    private javax.swing.JLabel conversionStatusLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JProgressBar totalProgressBar;
    private javax.swing.JLabel totalProgressLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public boolean canCloseWindow() {
        return convertDone;
    }

    protected abstract void continueOk();

    private void onError(Exception e) {
        JOptionPane.showMessageDialog(this,
                "Failed to convert the resources! " + e, "Error converting resources!", JOptionPane.ERROR_MESSAGE);

        // Lift the curse and exit
        convertDone = true;
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        dispose();
    }

    private void onComplete() {

        // Lift the curse and exit
        convertDone = true;
        continueOk();
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        dispose();
    }

    private void updateStatus(Integer currentProgress, Integer totalProgress, AssetsConverter.ConvertProcess process) {
        totalProgressBar.setMaximum(AssetsConverter.ConvertProcess.values().length);
        totalProgressBar.setValue(process.getProcessNumber() - 1);
        String progress = "Converting " + process.toString().toLowerCase();
        if (currentProgress != null && totalProgress != null) {
            progress += " (" + currentProgress + " / " + totalProgress + ")";
            conversionProgressBar.setMaximum(totalProgress);
            conversionProgressBar.setValue(currentProgress);
        } else {
            conversionProgressBar.setMaximum(1);
            conversionProgressBar.setValue(0);
        }
        progress += "...";
        conversionStatusLabel.setText(progress);
    }

    /**
     * A thread that handles the conversion process
     */
    private static class Converter extends Thread {

        private final String dungeonKeeperFolder;
        private final AssetManager assetManager;
        private final DKConverter frame;

        public Converter(String dungeonKeeperFolder, AssetManager assetManager, DKConverter frame) {
            this.dungeonKeeperFolder = dungeonKeeperFolder;
            this.assetManager = assetManager;
            this.frame = frame;
        }

        @Override
        public void run() {
            try {

                // Create a converter
                AssetsConverter assetsConverter = new AssetsConverter(dungeonKeeperFolder, assetManager) {
                    @Override
                    protected void updateStatus(Integer currentProgress, Integer totalProgress, AssetsConverter.ConvertProcess process) {
                        frame.updateStatus(currentProgress, totalProgress, process);
                    }
                };
                assetsConverter.convertAssets();
                frame.onComplete();
            } catch (Exception e) {

                // Fug
                frame.onError(e);
            } finally {

                // Let the window to be closed, no matter what
                convertDone = true;
            }
        }
    }
}
