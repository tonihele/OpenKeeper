import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.effects.EffectProperties;
import de.lessvoid.nifty.effects.impl.Hint;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.render.batch.BatchRenderConfiguration;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import toniarts.openkeeper.gui.nifty.MapHoverHint;

/** Standalone GPU integration check of the production map hint and HUD hint lifecycle. */
public class FogHoverHintRenderProbe extends SimpleApplication {
    private final CountDownLatch done = new CountDownLatch(1);
    private int frame;
    private boolean passed;
    private Nifty nifty;
    private MapHoverHint hint;
    private Hint guiHint;

    @Override
    public void simpleInitApp() {
        try {
            flyCam.setEnabled(false);
            setDisplayFps(false);
            setDisplayStatView(false);
            assetManager.registerLocator("assets", FileLocator.class);
            assetManager.registerLocator("assets/Converted", FileLocator.class);
            assetManager.registerLocator("/tmp", FileLocator.class);
            var config = new BatchRenderConfiguration();
            config.atlasWidth = 2048;
            config.atlasHeight = 2048;
            config.useHighQualityTextures = true;
            var display = NiftyJmeDisplay.newNiftyJmeDisplay(assetManager, inputManager,
                    audioRenderer, guiViewPort, config);
            guiViewPort.addProcessor(display);
            nifty = display.getNifty();
            Files.writeString(Path.of("/tmp/fog-hint-probe.xml"), """
                <nifty>
                  <useStyles filename="nifty-default-styles.xml"/>
                  <useStyles filename="Interface/HintStyle.xml"/>
                  <useControls filename="nifty-default-controls.xml"/>
                  <useControls filename="Interface/MyControls.xml"/>
                  <style id="textSmall">
                    <attributes font="Interface/Fonts/Ingame16.fnt" color="#bbbcbb"/>
                  </style>
                  <screen id="test" controller="de.lessvoid.nifty.screen.DefaultScreenController">
                    <layer childLayout="center" backgroundColor="#304050ff">
                      <panel id="anchor" width="100%" height="100%"/>
                    </layer>
                  </screen>
                </nifty>
                """);
            nifty.fromXml("fog-hint-probe.xml", "test");
            nifty.getNiftyMouse().setMousePosition(300, 240);
            hint = new MapHoverHint(nifty.getCurrentScreen().findElementById("anchor"), "Mist");
        } catch (Throwable e) {
            fail(e);
        }
    }

    private Element layer() {
        return nifty.getCurrentScreen().findElementById("niftyHintLayer");
    }

    private boolean containsText(Element element, String text) {
        var renderer = element.getRenderer(TextRenderer.class);
        if (renderer != null && text.equals(renderer.getOriginalText())) {
            return true;
        }
        return element.getChildren().stream().anyMatch(child -> containsText(child, text));
    }

    private void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private void save() throws Exception {
        var bytes = BufferUtils.createByteBuffer(800 * 600 * 4);
        renderer.readFrameBuffer(null, bytes);
        var out = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 600; y++) {
            for (int x = 0; x < 800; x++) {
                int i = ((599 - y) * 800 + x) * 4;
                out.setRGB(x, y, (Byte.toUnsignedInt(bytes.get(i)) << 16)
                        | (Byte.toUnsignedInt(bytes.get(i + 1)) << 8)
                        | Byte.toUnsignedInt(bytes.get(i + 2)));
            }
        }
        ImageIO.write(out, "png", Path.of("/tmp/fog-hover-visible.png").toFile());
    }

    private void fail(Throwable e) {
        e.printStackTrace();
        done.countDown();
        stop();
    }

    @Override
    public void simpleUpdate(float tpf) {
        try {
            frame++;
            switch (frame) {
                case 20 -> {
                    hint.update(true, 1.49f);
                    check(layer() == null, "Hint appeared before 1.5s");
                }
                case 25 -> hint.update(true, .02f);
                case 40 -> {
                    check(layer() != null && layer().isVisible() && containsText(layer(), "Mist"),
                            "Fog hint missing after delay");
                    save();
                }
                case 45 -> {
                    hint.clear();
                    var properties = new Properties();
                    properties.setProperty("hintText", "HUD hint");
                    guiHint = new Hint();
                    var anchor = nifty.getCurrentScreen().findElementById("anchor");
                    guiHint.activate(nifty, anchor, new EffectProperties(properties));
                    guiHint.execute(anchor, 1, null, nifty.getRenderEngine());
                }
                case 65 -> {
                    check(!containsText(layer(), "Mist") && containsText(layer(), "HUD hint"),
                            "Fog hint interfered with HUD hint");
                    guiHint.deactivate();
                    hint.update(true, .75f);
                    hint.update(false, 0);
                }
                case 85 -> {
                    check(layer().getChildren().isEmpty(), "Hint leaked after clearing");
                    hint.update(true, .75f);
                    check(layer().getChildren().isEmpty(), "Cancelled delay was retained");
                }
                case 90 -> hint.update(true, .76f);
                case 105 -> {
                    check(containsText(layer(), "Mist"), "Reentry failed");
                    hint.clear();
                }
                case 125 -> {
                    check(layer().getChildren().isEmpty(), "Repeated hint leaked");
                    passed = true;
                    System.out.println("PASS: 1.5s delay, rendered text, HUD coexistence, cancellation, reentry, cleanup");
                    done.countDown();
                    stop();
                }
            }
        } catch (Throwable e) {
            fail(e);
        }
    }

    public static void main(String[] args) throws Exception {
        var app = new FogHoverHintRenderProbe();
        var settings = new AppSettings(true);
        settings.setResolution(800, 600);
        settings.setFrameRate(60);
        settings.setGammaCorrection(false);
        settings.setAudioRenderer(null);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start(JmeContext.Type.OffscreenSurface);
        if (!app.done.await(45, TimeUnit.SECONDS) || !app.passed) {
            app.stop();
            throw new AssertionError("Fog hint probe failed");
        }
    }
}
