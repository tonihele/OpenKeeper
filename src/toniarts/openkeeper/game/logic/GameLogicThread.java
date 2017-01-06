/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.logic;

import com.jme3.app.Application;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Runs the game logic. Implements runnable, so supports running from a thread.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameLogicThread implements Runnable {

    private final float tpf;
    private final IGameLogicUpdateable[] updatables;
    private long ticks = 0;
    private final Application app;
    private final WorldState worldState;
    private final Map<CreatureControl, ColorRGBA> creatureDebugColors;
    private static final Logger logger = Logger.getLogger(GameLogicThread.class.getName());

    public GameLogicThread(Application app, WorldState worldState, float tpf, IGameLogicUpdateable... updatables) {
        this.app = app;
        this.tpf = tpf;
        this.worldState = worldState;
        this.updatables = updatables;
        if (Main.isDebug()) {
            creatureDebugColors = new HashMap<>();
        } else {
            creatureDebugColors = null;
        }
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();

        // Before anything is run, update last known positions to our map
        for (int y = 0; y < worldState.getMapData().getHeight(); y++) {
            for (int x = 0; x < worldState.getMapData().getWidth(); x++) {
                worldState.getMapData().getTile(x, y).clearCreatures();
            }
        }
        for (CreatureControl creature : worldState.getThingLoader().getCreatures()) {
            Point p = creature.getCreatureCoordinates();
            if (p != null) {
                TileData tile = worldState.getMapData().getTile(p);
                if (tile != null) {
                    tile.addCreature(creature);
                }
            }
        }
        if (Main.isDebug()) {
            drawCreatureVisibilities();
        }
        //

        // Update updatables
        for (IGameLogicUpdateable updatable : updatables) {
            try {
                updatable.processTick(tpf, app);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in game logic tick on " + updatable.getClass() + "!", e);
            }
        }

        // Increase ticks
        ticks++;

        // Logging
        long tickTime = System.currentTimeMillis() - start;
        logger.log(tickTime < tpf * 1000 ? Level.FINEST : Level.SEVERE, "Tick took {0} ms!", tickTime);
    }

    /**
     * Get the elapsed game time, in seconds
     *
     * @return the game time
     */
    public double getGameTime() {
        return ticks * tpf;
    }

    private void drawCreatureVisibilities() {
        Node node = new Node("Visibilities");
        float elevation = 0.1f;
        for (CreatureControl creature : worldState.getThingLoader().getCreatures()) {
            ColorRGBA creatureColor = creatureDebugColors.get(creature);
            if (creatureColor == null) {
                creatureColor = ColorRGBA.randomColor();
                creatureDebugColors.put(creature, creatureColor);
            }
            if (!creature.getVisibleCreatures().isEmpty()) {
                elevation += 0.02;
                Box box = new Box(0.1f, 0.1f, 0.1f);
                Geometry geometry = new Geometry("Box", box);
                Material orange = new Material(worldState.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                orange.setColor("Color", new ColorRGBA(creatureColor.r, creatureColor.g, creatureColor.b, 0.4f));
                orange.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                orange.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                geometry.setCullHint(Spatial.CullHint.Never);
                geometry.setMaterial(orange);
                geometry.setLocalTranslation(creature.getSpatial().getWorldTranslation());
                node.attachChild(geometry);
                for (CreatureControl visibleCreature : creature.getVisibleCreatures()) {
                    if (!visibleCreature.equals(creature)) {
                        Line line = new Line(creature.getSpatial().getWorldTranslation(), visibleCreature.getSpatial().getWorldTranslation());
                        line.setLineWidth(2);
                        geometry = new Geometry("Bullet", line);
                        orange = new Material(worldState.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                        orange.setColor("Color", creatureColor);
                        orange.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
                        geometry.setCullHint(Spatial.CullHint.Never);
                        geometry.setMaterial(orange);
                        geometry.move(0, elevation, 0);
                        node.attachChild(geometry);
                    }
                }
            }
        }
        app.enqueue(() -> {
            worldState.getWorld().detachChildNamed("Visibilities");
            worldState.getWorld().attachChild(node);
        });
    }

}
