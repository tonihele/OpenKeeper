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

/**
 * Constructs dungeon hearts (deals with the animation)
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class DungeonHeartConstruction extends GameTimeCounter {

    private static final float GRAVITY = 9.81f;
    private final float velocity = 7f;
    private final EntityData entityData;
    private final Set<EntityId> dungeonHeartPlugs;
    private final Map<EntityId, Set<PlugPiece>> dungeonHeartPlugPieces;
    private final Map<EntityId, Set<EntityId>> dungeonHeartStairs;
    private final Map<EntityId, Set<EntityId>> dungeonHeartArches;
    private final float delay;
    private boolean done = false;

    public DungeonHeartConstruction(EntityData entityData, float delay) {
        this.entityData = entityData;
        this.delay = delay;

        dungeonHeartPlugs = entityData.findEntities(new FieldFilter(ObjectComponent.class, "objectId", FiveByFiveRotatedController.OBJECT_PLUG_ID), ObjectComponent.class, Position.class, Owner.class);
        dungeonHeartPlugPieces = new HashMap<>();
        for (EntityId entityId : dungeonHeartPlugs) {
            dungeonHeartPlugPieces.put(entityId, new HashSet<>());
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

        if (done || timeElapsed < delay) {
            return;
        }

        // Process ticks
        for (EntityId entityId : dungeonHeartPlugs) {
            if (timeElapsed > 11) {
                //plugDecay.removeFromParent();
                //spatial.removeControl(this);
                showStepsAndArches(entityId);
                done = true;
            } else if (timeElapsed > 9) {

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
            } else if (timeElapsed > 6) {

                // Remove the plug
                entityData.removeEntity(entityId);

                // Show the pieces
                //plugDecay.setCullHint(Spatial.CullHint.Inherit);
            }
        }

        // If done, cleanup
        if (done) {
            dungeonHeartPlugs.clear();
            dungeonHeartPlugPieces.clear();
            dungeonHeartStairs.clear();
            dungeonHeartArches.clear();
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

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
