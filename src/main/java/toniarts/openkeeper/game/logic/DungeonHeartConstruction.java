/*
 * Copyright (C) 2014-2018 OpenKeeper
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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.utils.GameTimeCounter;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Constructs dungeon hearts (deals with the animation)
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class DungeonHeartConstruction extends GameTimeCounter {

    private static final float GRAVITY = 9.81f;

    /**
     * Seconds from the start of the construction to the removal of the plug
     */
    private static final double PLUG_REMOVAL_TIME = 6;

    /**
     * Seconds from the start of the construction to the plug pieces falling
     */
    private static final double PLUG_PIECES_FALL_TIME = 9;

    /**
     * Seconds from the start of the construction to the end of it, when the
     * steps and the arches show up
     */
    private static final double CONSTRUCTION_TIME = 11;

    /**
     * Seconds from the start of the construction to the arches starting to
     * fade in
     */
    private static final double ARCHES_FADE_IN_TIME = 7;

    /**
     * The effects DKII.EXE's dungeon heart tick (0x4D9A60) sets off at the
     * heart: the static on the plug while the level waits to begin, the start
     * of the level and the arches fading in
     */
    private static final int PLUG_STATIC_LIT_EFFECT_ID = 223;
    private static final int START_LEVEL_EFFECT_ID = 225;
    private static final int ARCHES_FADE_IN_EFFECT_ID = 314;

    private final float velocity = 7f;
    private final EntityData entityData;
    private final Set<EntityId> dungeonHeartPlugs;
    private final Map<EntityId, Set<PlugPiece>> dungeonHeartPlugPieces;
    private final Map<EntityId, Set<EntityId>> dungeonHeartStairs;
    private final Map<EntityId, Set<EntityId>> dungeonHeartArches;
    private final Map<EntityId, Vector3f> dungeonHeartPositions;
    private final float delay;
    private final int plugEffectId;
    private final double tickTime;
    private double nextPlugStaticTime = 0;
    private boolean started = false;
    private boolean plugsRemoved = false;
    private boolean archesFadingIn = false;
    private boolean done = false;

    /**
     * Constructs the dungeon hearts
     *
     * @param entityData the entity data
     * @param delay seconds before the construction begins
     * @param plugEffectId the plug's effect when it breaks apart
     * @param ticksPerSecond the game ticks per second, the static on the plug
     * is set off every tick
     */
    public DungeonHeartConstruction(EntityData entityData, float delay, int plugEffectId, int ticksPerSecond) {
        this.entityData = entityData;
        this.delay = delay;
        this.plugEffectId = plugEffectId;
        this.tickTime = 1.0 / ticksPerSecond;

        dungeonHeartPlugs = entityData.findEntities(new FieldFilter(ObjectComponent.class, "objectId", FiveByFiveRotatedController.OBJECT_PLUG_ID), ObjectComponent.class, Position.class, Owner.class);
        dungeonHeartPlugPieces = new HashMap<>();
        dungeonHeartPositions = new HashMap<>();
        for (EntityId entityId : dungeonHeartPlugs) {
            dungeonHeartPlugPieces.put(entityId, new HashSet<>());

            // The plug sits in the middle of the heart, the effects play on the floor there
            Vector3f position = new Vector3f(entityData.getComponent(entityId, Position.class).position);
            position.y = WorldUtils.FLOOR_HEIGHT;
            dungeonHeartPositions.put(entityId, position);
        }

        // We remove these from sight before the start
        // Stairs
        dungeonHeartStairs = new HashMap<>();
        Set<EntityId> stairs = entityData.findEntities(
                new FieldFilter(ObjectComponent.class, "objectId", FiveByFiveRotatedController.OBJECT_BIG_STEPS_ID),
                ObjectComponent.class, Position.class, Owner.class);
        for (EntityId entityId : dungeonHeartPlugs) {
            short dHeartOwner = entityData.getComponent(entityId, Owner.class).ownerId;
            Set<EntityId> dHeartStairs = new HashSet<>();
            for (EntityId stairsEntityId : stairs) {
                if (entityData.getComponent(stairsEntityId, Owner.class).ownerId == dHeartOwner) {
                    ObjectViewState originalState = entityData.getComponent(stairsEntityId, ObjectViewState.class);
                    entityData.setComponent(stairsEntityId, new ObjectViewState(originalState.objectId, originalState.state, originalState.animState, false));

                    dHeartStairs.add(stairsEntityId);
                }
            }
            dungeonHeartStairs.put(entityId, dHeartStairs);
        }

        // Arches
        dungeonHeartArches = new HashMap<>();
        Set<EntityId> arches = entityData.findEntities(
                new FieldFilter(ObjectComponent.class, "objectId", FiveByFiveRotatedController.OBJECT_ARCHES_ID),
                ObjectComponent.class, Position.class, Owner.class);
        for (EntityId entityId : dungeonHeartPlugs) {
            short dHeartOwner = entityData.getComponent(entityId, Owner.class).ownerId;
            Set<EntityId> dHeartArches = new HashSet<>();
            for (EntityId archesEntityId : arches) {
                if (entityData.getComponent(archesEntityId, Owner.class).ownerId == dHeartOwner) {
                    ObjectViewState originalState = entityData.getComponent(archesEntityId, ObjectViewState.class);
                    entityData.setComponent(archesEntityId, new ObjectViewState(originalState.objectId, originalState.state, originalState.animState, false));

                    dHeartArches.add(archesEntityId);
                }
            }
            dungeonHeartArches.put(entityId, dHeartArches);
        }
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);

        if (done) {
            return;
        }

        // While the level waits to begin, the plug crackles with static every game tick
        if (timeElapsed < delay) {
            if (timeElapsed >= nextPlugStaticTime) {
                triggerAtHearts(PLUG_STATIC_LIT_EFFECT_ID);
                nextPlugStaticTime = timeElapsed + tickTime;
            }
            return;
        }

        if (!started) {
            triggerAtHearts(START_LEVEL_EFFECT_ID);
            started = true;
        }

        // The construction steps are timed from the start of the construction, the level may delay it.
        // The plug always goes before the steps and arches show, even if a tick skips past its time
        double constructionTimeElapsed = timeElapsed - delay;
        if (!plugsRemoved && constructionTimeElapsed > PLUG_REMOVAL_TIME) {
            for (EntityId entityId : dungeonHeartPlugs) {

                // Remove the plug, it breaks apart with its own effect
                entityData.removeEntity(entityId);
                EffectTriggers.trigger(entityData, plugEffectId, dungeonHeartPositions.get(entityId), timeElapsed);
            }
            plugsRemoved = true;
        }

        if (!archesFadingIn && constructionTimeElapsed >= ARCHES_FADE_IN_TIME) {
            triggerAtHearts(ARCHES_FADE_IN_EFFECT_ID);
            archesFadingIn = true;
        }

        // Process ticks
        for (EntityId entityId : dungeonHeartPlugs) {
            if (constructionTimeElapsed > CONSTRUCTION_TIME) {
                //plugDecay.removeFromParent();
                //spatial.removeControl(this);
                showStepsAndArches(entityId);
                done = true;
            } else if (constructionTimeElapsed > PLUG_PIECES_FALL_TIME) {

                // This I think is effect rather than real objects, so purely on client then?
//                    velocity -= GRAVITY * tpf;
//                    for (Spatial piece : plugDecay.getChildren()) {
//                        float rotate = (float) piece.getUserData("rotate") * 10 * tpf;
//                        float pv = (float) piece.getUserData("velocity") - GRAVITY * tpf;
//                        piece.setUserData("velocity", pv);
//                        piece.move(0, pv * tpf, 0);
//                        //float step = (float) piece.getUserData("yAngle");
//                        //piece.move(tpf * FastMath.cos(step), velocity * tpf, tpf * FastMath.sin(step));
//                        piece.rotate(rotate, rotate, rotate);
//                    }
            }
        }

        // If done, cleanup
        if (done) {
            dungeonHeartPlugs.clear();
            dungeonHeartPlugPieces.clear();
            dungeonHeartStairs.clear();
            dungeonHeartArches.clear();
            dungeonHeartPositions.clear();
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    private void triggerAtHearts(int effectId) {
        for (Vector3f position : dungeonHeartPositions.values()) {
            EffectTriggers.trigger(entityData, effectId, position, timeElapsed);
        }
    }

    private void showStepsAndArches(EntityId entityId) {
        for (EntityId stairsEntityId : dungeonHeartStairs.get(entityId)) {
            ObjectViewState originalState = entityData.getComponent(stairsEntityId, ObjectViewState.class);
            entityData.setComponent(stairsEntityId, new ObjectViewState(originalState.objectId, originalState.state, originalState.animState, true));
        }
        for (EntityId archesEntityId : dungeonHeartArches.get(entityId)) {
            ObjectViewState originalState = entityData.getComponent(archesEntityId, ObjectViewState.class);
            entityData.setComponent(archesEntityId, new ObjectViewState(originalState.objectId, originalState.state, originalState.animState, true));
        }
    }

    private static final class PlugPiece {

        public PlugPiece() {
        }
    }

}
