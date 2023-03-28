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

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.utils.IGameLoopManager;

/**
 * Runs the game logic tasks, well, doesn't literally run them but wraps them up
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameLogicManager implements IGameLoopManager {
    
    private static final Logger LOGGER = Logger.getLogger(GameLogicManager.class.getName());

    private long ticks = 0;
    private double timeElapsed = 0.0;
    protected final IGameLogicUpdatable[] updatables;

    public GameLogicManager(IGameLogicUpdatable... updatables) {
        this.updatables = updatables;
    }

    @Override
    public void start() {
        for (IGameLogicUpdatable updatable : updatables) {
            updatable.start();
        }
    }

    @Override
    public void processTick(long delta) {

        // Update game time
        long start = System.nanoTime();
        float tpf = delta / 1000000000f;

        // Update updatables
        for (IGameLogicUpdatable updatable : updatables) {
            try {
                updatable.processTick(tpf, timeElapsed);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in game logic tick on " + updatable.getClass() + "!", e);
            }
        }

        // Logging
        long tickTime = System.nanoTime() - start;
        LOGGER.log(tickTime < delta ? Level.FINEST : Level.SEVERE, "Tick took {0} ms!", TimeUnit.MILLISECONDS.convert(tickTime, TimeUnit.NANOSECONDS));

        // Increase ticks & time
        timeElapsed += tpf;
        ticks++;
    }

    @Override
    public void stop() {
        for (IGameLogicUpdatable updatable : updatables) {
            updatable.stop();
        }
    }

    /**
     * Get the elapsed game time, in seconds
     *
     * @return the game time
     */
    public double getGameTime() {
        return timeElapsed;
    }

    /**
     * Get the amount of game ticks ticked over
     *
     * @param ticks the ticks
     */
    public void setTicks(long ticks) {
        this.ticks = ticks;
    }

//    private void drawCreatureVisibilities() {
//        Node node = new Node("Visibilities");
//        float elevation = 0.1f;
//        for (CreatureControl creature : worldState.getThingLoader().getCreatures()) {
//            ColorRGBA creatureColor = creatureDebugColors.get(creature);
//            if (creatureColor == null) {
//                creatureColor = ColorRGBA.randomColor();
//                creatureDebugColors.put(creature, creatureColor);
//            }
//            if (!creature.getVisibleCreatures().isEmpty()) {
//                elevation += 0.02;
//                Box box = new Box(0.1f, 0.1f, 0.1f);
//                Geometry geometry = new Geometry("Box", box);
//                Material orange = new Material(worldState.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//                orange.setColor("Color", new ColorRGBA(creatureColor.r, creatureColor.g, creatureColor.b, 0.4f));
//                orange.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
//                orange.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
//                geometry.setCullHint(Spatial.CullHint.Never);
//                geometry.setMaterial(orange);
//                geometry.setLocalTranslation(creature.getSpatial().getWorldTranslation());
//                node.attachChild(geometry);
//                for (CreatureControl visibleCreature : creature.getVisibleCreatures()) {
//                    if (!visibleCreature.equals(creature)) {
//                        Line line = new Line(creature.getSpatial().getWorldTranslation(), visibleCreature.getSpatial().getWorldTranslation());
//
//                        orange = new Material(worldState.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//                        orange.setColor("Color", creatureColor);
//                        orange.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
//                        orange.getAdditionalRenderState().setLineWidth(2);
//
//                        geometry = new Geometry("Bullet", line);
//                        geometry.setCullHint(Spatial.CullHint.Never);
//                        geometry.setMaterial(orange);
//                        geometry.move(0, elevation, 0);
//                        node.attachChild(geometry);
//                    }
//                }
//            }
//        }
//        app.enqueue(() -> {
//            worldState.getWorld().detachChildNamed("Visibilities");
//            worldState.getWorld().attachChild(node);
//        });
//    }
    public long getTicks() {
        return ticks;
    }
}
