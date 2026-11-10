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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Runs the game logic tasks, well, doesn't literally run them but wraps them up
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class GameLogicManager implements IGameLogicUpdatable {

    private static final Logger logger = System.getLogger(GameLogicManager.class.getName());
    private static final int MAX_QUEUED_ACTIONS = 1024;

    protected final IGameLogicUpdatable[] updatables;
    private final BlockingQueue<Runnable> actions = new LinkedBlockingQueue<>(MAX_QUEUED_ACTIONS);
    private final ConcurrentHashMap<Short, Runnable> latestInputs = new ConcurrentHashMap<>();

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
        runQueuedActions();

        // Update updatables
        for (IGameLogicUpdatable updatable : updatables) {
            try {
                updatable.processTick(tpf);
            } catch (Exception e) {
                logger.log(Level.ERROR, "Error in game logic tick on " + updatable.getClass() + "!", e);
            }
        }
    }

    /**
     * Queues a state-changing player action for the start of the next logic
     * tick. Adding never waits for the simulation thread. The bounded queue
     * rejects excess requests instead of letting a stalled server grow memory
     * without limit.
     *
     * @param action action to run
     * @return whether the action was accepted
     */
    public boolean enqueueAction(Runnable action) {
        Objects.requireNonNull(action, "action");
        boolean accepted = actions.offer(action);
        if (!accepted) {
            logger.log(Level.WARNING, "Rejecting game action because the logic queue is full ({0})", MAX_QUEUED_ACTIONS);
        }
        return accepted;
    }

    /**
     * Stores the latest high-rate input for a player, replacing an older value
     * until the next simulation tick. Players are applied in ID order so
     * simultaneous inputs have a stable simulation order.
     *
     * @param playerId input stream player ID
     * @param input action that applies the latest value
     */
    public void enqueueLatestInput(short playerId, Runnable input) {
        latestInputs.put(playerId, Objects.requireNonNull(input, "input"));
    }

    private void runQueuedActions() {
        List<Runnable> queued = new ArrayList<>();
        actions.drainTo(queued);
        List<Short> playerIds = new ArrayList<>(latestInputs.keySet());
        Collections.sort(playerIds);
        for (short playerId : playerIds) {
            Runnable input = latestInputs.get(playerId);
            if (input != null && latestInputs.remove(playerId, input)) {
                queued.add(input);
            }
        }

        for (Runnable action : queued) {
            try {
                action.run();
            } catch (Exception e) {
                logger.log(Level.ERROR, "Error in queued game action!", e);
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
