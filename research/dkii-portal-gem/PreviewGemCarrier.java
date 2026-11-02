import com.jme3.app.state.AbstractAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.app.state.ScreenshotAppState;
import com.simsilica.es.base.DefaultEntityData;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.tools.modelviewer.ModelViewer;
import toniarts.openkeeper.view.control.CreatureViewControl;
import toniarts.openkeeper.view.effect.EffectManagerState;

/** Repeatable ModelViewer comparison of the production carrier effect. */
class PreviewGemCarrier {
    public static void main(String[] args) {
        boolean compare = java.util.Arrays.asList(args).contains("--compare");
        String folder = "/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/";
        var app = new ModelViewer(null, folder);
        var settings = new AppSettings(true);
        settings.setTitle("Native portal gem carrier");
        settings.setResolution(1280, 800);
        settings.setFrameRate(60);
        settings.setVSync(true);
        settings.setAudioRenderer(null);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        var screenshot = new ScreenshotAppState("/tmp/", "gem-native-preview");
        app.getStateManager().attach(screenshot);
        app.getStateManager().attach(new AbstractAppState() {
            DefaultEntityData data;
            com.simsilica.es.EntityId id;
            CreatureViewControl control;
            Node model;
            boolean ready, moving, gem = true, captured, startedWalk, capturedWalk;
            float elapsed;

            @Override
            public void update(float tpf) {
                if (!ready) {
                    try {
                        var level = KwdFile.load(folder, "Level1");
                        data = new DefaultEntityData();
                        id = data.createEntity();
                        data.setComponents(id, new Owner((short) 3, (short) 3), new PortalGem());
                        model = new Node("Lord carrying portal gem");
                        control = new CreatureViewControl(id, data, level.getCreature((short) 29),
                                Creature.AnimationType.STAND_STILL, app.getAssetManager(), null);
                        control.setEffectManager(new EffectManagerState(level, app.getAssetManager()));
                        model.addControl(control);
                        var setup = ModelViewer.class.getDeclaredMethod("setupModel", Node.class, boolean.class);
                        setup.setAccessible(true);
                        setup.invoke(app, model, false);
                        app.getCamera().setLocation(compare ? new Vector3f(2.8f, 3.8f, 4.2f) : new Vector3f(1.8f, 1.5f, 2.6f));
                        app.getCamera().lookAt(new Vector3f(0, 0.55f, 0), Vector3f.UNIT_Y);
                        var caption = new com.jme3.font.BitmapText(app.getAssetManager().loadFont("Interface/Fonts/Default.fnt"));
                        caption.setText("Native carrier effect 257 / model marker 10001\nF5: gem on/off   F6: walk   F7: combat   F8: idle");
                        caption.setSize(18);
                        caption.setLocalTranslation(350, 770, 0);
                        app.getGuiNode().attachChild(caption);
                        for (int key : new int[]{KeyInput.KEY_F5, KeyInput.KEY_F6, KeyInput.KEY_F7, KeyInput.KEY_F8}) {
                            app.getInputManager().addMapping("gem-preview-" + key, new KeyTrigger(key));
                        }
                        app.getInputManager().addListener((ActionListener) (name, pressed, dt) -> {
                            if (!pressed) return;
                            int key = Integer.parseInt(name.substring(12));
                            if (key == KeyInput.KEY_F5) {
                                gem = !gem;
                                if (gem) data.setComponent(id, new PortalGem());
                                else data.removeComponent(id, PortalGem.class);
                            } else {
                                moving = key == KeyInput.KEY_F6;
                                control.setTargetState(moving ? Creature.AnimationType.WALK
                                        : key == KeyInput.KEY_F7 ? Creature.AnimationType.MELEE_ATTACK
                                        : Creature.AnimationType.STAND_STILL);
                            }
                        }, "gem-preview-" + KeyInput.KEY_F5, "gem-preview-" + KeyInput.KEY_F6,
                                "gem-preview-" + KeyInput.KEY_F7, "gem-preview-" + KeyInput.KEY_F8);
                        ready = true;
                        System.out.println("PREVIEW READY: native carrier effect; F5 gem, F6 walk, F7 combat, F8 idle");
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        app.stop();
                    }
                }
                elapsed += tpf;
                if (ready && moving) model.setLocalTranslation((float) Math.sin(elapsed) * 0.7f, 0, 0);
                if (ready && compare && elapsed > 8 && !startedWalk) {
                    moving = true;
                    control.setTargetState(Creature.AnimationType.WALK);
                    startedWalk = true;
                }
                if (ready && compare && elapsed > 12 && !capturedWalk) {
                    screenshot.takeScreenshot();
                    capturedWalk = true;
                }
                if (ready && elapsed > 5 && !captured) {
                    screenshot.takeScreenshot();
                    captured = true;
                }
            }

            @Override
            public void cleanup() {
                if (control != null) control.cleanup();
                if (data != null) data.close();
                super.cleanup();
            }
        });
        app.start();
    }
}
