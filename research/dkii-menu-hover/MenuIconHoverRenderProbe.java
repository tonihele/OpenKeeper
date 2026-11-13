import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.render.batch.BatchRenderConfiguration;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import toniarts.openkeeper.gui.nifty.icontext.IconTextControl;

/** GPU integration check using the production iconText control and jME Nifty batch backend. */
public class MenuIconHoverRenderProbe extends SimpleApplication {
    private Nifty nifty;
    private int frame;
    private byte[] normal;
    private byte[] hover;
    private Element icon;
    private Element neighbor;
    private boolean passed;
    private final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
    private final Path output = Path.of("/tmp/openkeeper-menu-hover-gpu");

    @Override
    public void simpleInitApp() {
        try {
            Files.createDirectories(output);
            // Dark, colored, black and saturated texels expose clamping and blend errors.
            BufferedImage swatches = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            int[] colors = {0x102010, 0x204060, 0xa0b0c0, 0x000000};
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++)
                swatches.setRGB(x, y, colors[x / 16]);
            ImageIO.write(swatches, "png", output.resolve("swatches.png").toFile());
            assetManager.registerLocator(output.toString(), FileLocator.class);
            assetManager.registerLocator("assets", FileLocator.class);
            assetManager.registerLocator("assets/Converted", FileLocator.class);
            flyCam.setEnabled(false);
            setDisplayFps(false);
            setDisplayStatView(false);
            BatchRenderConfiguration config = new BatchRenderConfiguration();
            config.atlasWidth = 2048;
            config.atlasHeight = 2048;
            config.fillRemovedImagesInAtlas = false;
            config.disposeImagesBetweenScreens = false;
            config.useHighQualityTextures = true;
            var display = NiftyJmeDisplay.newNiftyJmeDisplay(assetManager, inputManager,
                    audioRenderer, guiViewPort, config);
            guiViewPort.addProcessor(display);
            nifty = display.getNifty();
            String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <nifty xmlns="http://nifty-gui.lessvoid.com/nifty-gui">
                  <useStyles filename="nifty-default-styles.xml"/>
                  <useControls filename="nifty-default-controls.xml"/>
                  <useControls filename="Interface/MyControls.xml"/>
                  <style id="textNormal"><attributes font="Interface/Fonts/Ingame28.fnt" color="#bbbcbb"/></style>
                  <registerEffect name="menuIconHighlight" class="toniarts.openkeeper.gui.nifty.effect.MenuIconHighlightEffect"/>
                  <screen id="test" controller="de.lessvoid.nifty.screen.DefaultScreenController">
                    <layer childLayout="absolute" backgroundColor="#203040ff">
                      <control id="subject" name="iconText" x="10px" y="10px" width="170px" height="90px" icon="swatches.png" text="" click=""/>
                      <control id="neighbor" name="iconText" x="210px" y="10px" width="170px" height="90px" icon="swatches.png" text="" click=""/>
                      <control id="real" name="iconText" x="10px" y="110px" width="170px" height="90px" icon="Textures/GUI/Options/i-load.png" text="" click=""/>
                    </layer>
                  </screen>
                </nifty>
                """;
            Files.writeString(output.resolve("probe.xml"), xml);
            nifty.fromXml("probe.xml", "test");
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private byte[] pixels() {
        ByteBuffer buffer = BufferUtils.createByteBuffer(settings.getWidth() * settings.getHeight() * 4);
        renderer.readFrameBuffer(null, buffer);
        byte[] bytes = new byte[buffer.capacity()];
        buffer.rewind(); buffer.get(bytes);
        return bytes;
    }

    private int channel(byte[] pixels, int x, int y, int channel) {
        return Byte.toUnsignedInt(pixels[((settings.getHeight() - 1 - y) * settings.getWidth() + x) * 4 + channel]);
    }

    private void save(byte[] pixels, String name) throws Exception {
        BufferedImage im = new BufferedImage(settings.getWidth(), settings.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < im.getHeight(); y++) for (int x = 0; x < im.getWidth(); x++)
            im.setRGB(x, y, (channel(pixels, x, y, 0) << 16) | (channel(pixels, x, y, 1) << 8) | channel(pixels, x, y, 2));
        ImageIO.write(im, "png", output.resolve(name).toFile());
    }

    @Override
    public void simpleUpdate(float tpf) {
        try {
            frame++;
            if (frame == 20) {
                icon = nifty.getCurrentScreen().findElementById("subject").findElementById("#image");
                neighbor = nifty.getCurrentScreen().findElementById("neighbor").findElementById("#image");
                normal = pixels(); save(normal, "normal.png");
                nifty.getCurrentScreen().findElementById("subject").getControl(IconTextControl.class).startHover();
                nifty.getCurrentScreen().findElementById("real").getControl(IconTextControl.class).startHover();
            } else if (frame == 30) {
                hover = pixels(); save(hover, "hover.png");
                for (int swatch = 0; swatch < 4; swatch++) {
                    int x = icon.getX() + 8 + swatch * 16, y = icon.getY() + 32;
                    for (int c = 0; c < 3; c++) {
                        int n = channel(normal, x, y, c), h = channel(hover, x, y, c);
                        if (Math.abs(h - Math.min(255, n * 2)) > 2)
                            throw new AssertionError("GPU brightness: " + n + " -> " + h + " at swatch " + swatch);
                    }
                }
                for (int c = 0; c < 3; c++) {
                    int x = neighbor.getX() + 8, y = neighbor.getY() + 32;
                    if (channel(normal, x, y, c) != channel(hover, x, y, c))
                        throw new AssertionError("Hover color leaked to adjacent control");
                }
                Element real = nifty.getCurrentScreen().findElementById("real").findElementById("#image");
                for (int y = real.getY(); y < real.getY() + real.getHeight(); y++) {
                    for (int x = real.getX(); x < real.getX() + real.getWidth(); x++) {
                        for (int c = 0; c < 3; c++) {
                            int n = channel(normal, x, y, c), h = channel(hover, x, y, c);
                            if (Math.abs(h - Math.min(255, n * 2)) > 2)
                                throw new AssertionError("Load artwork brightness: " + n + " -> " + h);
                        }
                    }
                }
                nifty.getCurrentScreen().findElementById("subject").getControl(IconTextControl.class).endHover();
                nifty.getCurrentScreen().findElementById("real").getControl(IconTextControl.class).endHover();
            } else if (frame == 40) {
                byte[] restored = pixels(); save(restored, "restored.png");
                if (!java.util.Arrays.equals(normal, restored))
                    throw new AssertionError("Hover did not fully restore normal rendering");
                passed = true;
                System.out.println("MENU_ICON_GPU_PASS: RGB doubles, saturation clips, black stays opaque, neighboring control unchanged, endHover restores exact framebuffer");
                done.countDown(); stop();
            }
        } catch (Throwable e) {
            e.printStackTrace(); done.countDown(); stop();
        }
    }

    public static void main(String[] args) throws Exception {
        MenuIconHoverRenderProbe app = new MenuIconHoverRenderProbe();
        AppSettings settings = new AppSettings(true);
        settings.setResolution(400, 220);
        // Match production Settings: native menu colors are not gamma-corrected.
        settings.setGammaCorrection(false);
        settings.setAudioRenderer(null);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start(JmeContext.Type.OffscreenSurface);
        if (!app.done.await(45, java.util.concurrent.TimeUnit.SECONDS)) { app.stop(); throw new AssertionError("GPU probe timed out"); }
        if (!app.passed) throw new AssertionError("Menu icon GPU probe failed");
    }
}
