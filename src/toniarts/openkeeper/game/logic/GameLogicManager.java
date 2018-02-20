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

import java.util.logging.Logger;

/**
 * Runs the game logic tasks, well, doesn't literally run them but wraps them up
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameLogicManager extends AbstractLogicManager {

    private long ticks = 0;
    private double timeElapsed = 0.0;
    private static final Logger LOGGER = Logger.getLogger(GameLogicManager.class.getName());

    public GameLogicManager(IGameLogicUpdatable... updatables) {
        super(updatables);
    }

    @Override
    public void processTick(long delta) {
        super.processTick(delta);
        // Before anything is run, update last known positions to our map
//            for (int y = 0; y < worldState.getMapData().getHeight(); y++) {
//                for (int x = 0; x < worldState.getMapData().getWidth(); x++) {
//                    worldState.getMapData().getTile(x, y).clearCreatures();
//                }
//            }
//            for (CreatureControl creature : worldState.getThingLoader().getCreatures()) {
//                Point p = creature.getCreatureCoordinates();
//                if (p != null) {
//                    TileData tile = worldState.getMapData().getTile(p);
//                    if (tile != null) {
//                        tile.addCreature(creature);
//                    }
//                }
//            }
//            if (Main.isDebug()) {
//                drawCreatureVisibilities();
//            }
        //
        // Update updatables
        float tpf = delta / 1000000000f;

        // Increase ticks
        ticks++;
        timeElapsed += tpf;
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
