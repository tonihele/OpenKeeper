/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.view.PlayerEntityViewState;
import toniarts.openkeeper.view.animation.AnimationLoader;
import toniarts.openkeeper.view.animation.MotionHistory;

/** Opt-in real local-session integration test. Reflection is confined to test observation/bootstrap. */
public final class CharacterMotionGameSmoke extends com.jme3.app.state.AbstractAppState {
    private final Main app;
    private final CountDownLatch closed = new CountDownLatch(1);
    private volatile Throwable failure;
    private int active;
    private Map<Spatial, Double> pausedAges;
    private Map<Spatial, com.jme3.math.Vector3f> pausedRoots;
    private CharacterMotionGameSmoke(Main app) { this.app = app; }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 3) throw new IllegalArgumentException("Expected installed DK2 root, optional packed KiB and phase variation");
        PathUtils.setDKIIFolder(PathUtils.fixFilePath(args[0]));
        Field params = Main.class.getDeclaredField("params"); params.setAccessible(true);
        params.set(null, new HashMap<>(Map.of("level", "Alcatraz", "nomovies", "")));
        var constructor = Main.class.getDeclaredConstructor(); constructor.setAccessible(true);
        var app = constructor.newInstance();
        var observer = new CharacterMotionGameSmoke(app);
        var init = Main.class.getDeclaredMethod("initSettings", Main.class); init.setAccessible(true); init.invoke(null, app);
        Main.getUserSettings().setSetting(Settings.Setting.ANIMATION_INTERPOLATION, true);
        Main.getUserSettings().setSetting(Settings.Setting.ANIMATION_PACKED_KIB,args.length>1 ? Integer.parseInt(args[1]) : 0);
        Main.getUserSettings().setSetting(Settings.Setting.ANIMATION_PHASE_VARIATION,args.length>2 && Boolean.parseBoolean(args[2]));
        var settings = new AppSettings(true); settings.setResolution(1280,720);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33); settings.setFrameRate(60); settings.setVSync(false);
        settings.setTitle("OpenKeeper motion integration smoke");
        app.setSettings(settings); app.setShowSettings(false); app.setPauseOnLostFocus(false);
        app.getStateManager().attach(observer); app.start();
        if (!observer.closed.await(90, TimeUnit.SECONDS)) { app.stop(); throw new AssertionError("Motion game smoke timed out"); }
        app.stop(true);
        if (observer.failure != null) throw new AssertionError("Motion game smoke failed", observer.failure);
        if (observer.active < 360) throw new AssertionError("Session ended before validation");
    }

    @Override public void update(float tpf) {
        try {
            var view = app.getStateManager().getState(PlayerEntityViewState.class);
            var client = app.getStateManager().getState(GameClientState.class);
            if (view == null || client == null) return;
            Field tracksField = PlayerEntityViewState.class.getDeclaredField("motionTracks"); tracksField.setAccessible(true);
            Map<?,?> tracks = (Map<?,?>)tracksField.get(view);
            if (tracks.size() < 6) return;
            int sampled = 0;
            for (Object track : tracks.values()) {
                Field history = track.getClass().getDeclaredField("history"); history.setAccessible(true);
                if (Double.isFinite(((MotionHistory)history.get(track)).getSampledTime())) sampled++;
            }
            if (sampled < 6) return;
            active++;
            if (active == 90 || active == 300) {
                var capture = app.getStateManager().getState(com.jme3.app.state.ScreenshotAppState.class);
                if (capture != null) capture.takeScreenshot();
            }
            if (active == 120) client.getGameClientService().pauseGame();
            if (active == 180) {
                pausedAges = new LinkedHashMap<>(); pausedRoots = new LinkedHashMap<>();
                for (Object track : tracks.values()) {
                    Field object = track.getClass().getDeclaredField("object"); object.setAccessible(true);
                    Spatial root = (Spatial)object.get(track);
                    double age = AnimationLoader.getPlaybackAge(root);
                    if (Double.isFinite(age)) { pausedAges.put(root, age); pausedRoots.put(root, root.getLocalTranslation().clone()); }
                }
                if (pausedAges.size() < 6) throw new AssertionError("Too few actual playback owners");
            }
            if (active == 210) for (var entry : pausedAges.entrySet()) {
                if (AnimationLoader.getPlaybackAge(entry.getKey()) != entry.getValue()
                        || !entry.getKey().getLocalTranslation().equals(pausedRoots.get(entry.getKey())))
                    throw new AssertionError("Paused root or pose moved");
            }
            if (active == 240) client.getGameClientService().resumeGame();
            if (active == 360) {
                if (pausedAges.entrySet().stream().noneMatch(e -> AnimationLoader.getPlaybackAge(e.getKey()) > e.getValue()))
                    throw new AssertionError("Playback did not resume");
                long primitiveBytes=toniarts.openkeeper.utils.AssetUtils.getAnimationDataPrimitiveBytes(app.getAssetManager());
                int limit=Main.getUserSettings().getInteger(Settings.Setting.ANIMATION_PACKED_KIB)*1024;
                if (primitiveBytes>limit || limit>0 && primitiveBytes==0) throw new AssertionError("Packed context was not exercised or exceeded its primitive budget");
                System.out.println("MOTION_GAME_PASS active_frames="+active+" sampled_owners="+sampled+" paused_owners="+pausedAges.size()+" packed_primitive_bytes="+primitiveBytes);
                app.stop();
            }
        } catch (Throwable error) { failure = error; error.printStackTrace(); app.stop(); }
    }
    @Override public void cleanup() { try { super.cleanup(); } finally { closed.countDown(); } }
}
