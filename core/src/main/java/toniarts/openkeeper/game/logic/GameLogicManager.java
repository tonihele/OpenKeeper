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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Runs the game logic tasks, well, doesn't literally run them but wraps them up
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class GameLogicManager implements IGameLogicUpdatable {

    private static final Logger logger = System.getLogger(GameLogicManager.class.getName());

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
    public void processTick(float tpf) {
        // Update updatables
        for (IGameLogicUpdatable updatable : updatables) {
            try {
                updatable.processTick(tpf);
            } catch (Exception e) {
                logger.log(Level.ERROR, "Error in game logic tick on " + updatable.getClass() + "!", e);
            }
        }
    }

    @Override
    public void stop() {
        for (IGameLogicUpdatable updatable : updatables) {
            updatable.stop();
        }
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
}
