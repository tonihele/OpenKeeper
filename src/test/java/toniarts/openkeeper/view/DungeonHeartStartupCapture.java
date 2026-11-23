/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.view;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.system.*;
import com.simsilica.es.*;
import com.simsilica.es.base.DefaultEntityData;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.utils.*;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.control.ObjectViewControl;

/**
 * Opt-in OpenGL fixture using legally installed DK2 and converted assets.
 * Captures the production presentation on a controlled clock with neutral
 * lighting and disabled audio. It does not run a full level or establish pixel parity.
 */
public final class DungeonHeartStartupCapture extends SimpleApplication {
    private static final double[] CAPTURES = {-.2, 0, 1.3, 2.8, 6.9, 7.5, 7.8, 8.5, 9, 10, 15};
    private final IKwdFile kwd;
    private final String converted;
    private final Path output;
    private final DefaultEntityData entities = new DefaultEntityData();
    private final AtomicLong clock = new AtomicLong(750_000_000L);

    private DungeonHeartStartupCapture(IKwdFile kwd, String converted, Path output) {
        this.kwd = kwd;
        this.converted = converted;
        this.output = output;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || args[0].isBlank()) {
            throw new IllegalArgumentException("Pass DK2 root, converted asset directory, and capture directory");
        }
        String dk2 = Path.of(args[0]).toAbsolutePath() + "/";
        Path converted = Path.of(args[1]).toAbsolutePath();
        if (!Files.isRegularFile(converted.resolve("Models/Dungeon centre.j3o"))) {
            throw new IllegalArgumentException("Dungeon centre model missing in " + converted);
        }
        Path output = Path.of(args[2]).toAbsolutePath();
        Files.createDirectories(output);
        IKwdFile kwd = KwdFile.load(dk2, Path.of(dk2, "Data/editor/maps/level1.kwd"));
        var app = new DungeonHeartStartupCapture(kwd, converted.toString(), output);
        var settings = new AppSettings(true);
        settings.setResolution(960, 720);
        settings.setRenderer(AppSettings.LWJGL_OPENGL3);
        settings.setAudioRenderer(null);
        settings.setFrameRate(0);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start(JmeContext.Type.OffscreenSurface);
    }

    @Override
    public void simpleInitApp() {
        setDisplayFps(false);
        setDisplayStatView(false);
        flyCam.setEnabled(false);
        assetManager.registerLocator(converted, FileLocator.class);
        Map<EntityId, Spatial> models = new HashMap<>();
        EntityId[] parts = new EntityId[5];
        int[] objects = {13, 86, 88, 88, 88};
        for (int i = 0; i < parts.length; i++) {
            parts[i] = entities.createEntity();
            ArtResource art = kwd.getObject(objects[i]).getMeshResource();
            Spatial model = AssetUtils.loadModel(assetManager, art.getName(), art);
            if (i > 2) model.rotate(0, (i == 3 ? -1 : 1) * FastMath.TWO_PI / 3, 0);
            model.setLocalTranslation(0, WorldUtils.FLOOR_HEIGHT, 0);
            var definition = kwd.getObject(objects[i]);
            var state = new ObjectViewState((short) objects[i], definition.getStartState(),
                    ObjectViewState.GameObjectAnimState.MESH_RESOURCE, true);
            entities.setComponents(parts[i], new Owner((short) 3, (short) 3),
                    new Position(0, model.getLocalTranslation().clone()),
                    new ObjectComponent((short) objects[i], null), state);
            model.addControl(new ObjectViewControl(parts[i], entities, definition, state, assetManager, null));
            rootNode.attachChild(model);
            models.put(parts[i], model);
        }
        EntityId room = entities.createEntity();
        var introduction = new DungeonHeartIntroduction(null, parts[0], parts[1], parts[2], parts[3], parts[4]);
        introduction.startTime = 1_000_000_000L;
        introduction.ticksPerSecond = 4;
        entities.setComponents(room, new RoomComponent((short) 5, false, new Point(0, 0)),
                new Owner((short) 3, (short) 3), introduction);
        var effects = new EffectManagerState(kwd, assetManager);
        var presentation = new DungeonHeartIntroductionState(entities, (short) 3, rootNode, effects,
                models::get, point -> true, clock::get);
        var screenshots = new ScreenshotAppState(output + "/", "heart-", 0);
        stateManager.attach(new AbstractAppState() {
            private int capture;
            @Override public void update(float tpf) {
                clock.addAndGet(50_000_000L);
                double age = (clock.get() - introduction.startTime) / 1e9;
                if (capture < CAPTURES.length && age >= CAPTURES[capture]) {
                    screenshots.takeScreenshot();
                    System.out.println("Heart capture " + ++capture + " at " + age + " seconds");
                }
                if (age > 16) {
                    System.out.println("Heart startup render completed: " + output);
                    stop();
                }
            }
        });
        stateManager.attach(effects);
        stateManager.attach(presentation);
        stateManager.attach(screenshots);
        presentation.start();
        cam.setLocation(new Vector3f(6, 8, 6));
        cam.lookAt(new Vector3f(0, 1, 0), Vector3f.UNIT_Y);
        viewPort.setBackgroundColor(new ColorRGBA(.1f, .1f, .1f, 1));
        effects.prewarm(renderManager, 223, 225, 314);
    }

    @Override public void simpleUpdate(float tpf) {
        AssetUtils.applyFullLight(rootNode);
    }

    @Override public void handleError(String message, Throwable error) {
        System.err.println(message);
        error.printStackTrace();
        System.exit(1);
    }

    @Override public void destroy() {
        super.destroy();
        entities.close();
    }
}
