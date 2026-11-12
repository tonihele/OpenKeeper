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

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalViewState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.VisualEffect;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/** Portal preparation and arrival presentation, driven by replicated entity subscriptions. */
public final class PortalEffectState extends AbstractAppState {
    private final IKwdFile kwdFile;
    private final Node rootNode;
    private final Node effectsRoot = new Node("Portal effects");
    private final IFogOfWarInformation fog;
    private final PortalEffectContainer preparations;
    private final CreatureEntranceContainer arrivals;
    private final List<TransientEffect> transientEffects = new ArrayList<>();
    private AppStateManager stateManager;

    private record TransientEffect(EntityId source, Node node, Point tile, VisualEffect visualEffect) {
    }

    public PortalEffectState(IKwdFile kwdFile, EntityData entityData, Node rootNode, IFogOfWarInformation fog) {
        this.kwdFile = kwdFile;
        this.rootNode = rootNode;
        this.fog = fog;
        preparations = new PortalEffectContainer(entityData);
        arrivals = new CreatureEntranceContainer(entityData);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.stateManager = stateManager;
        rootNode.attachChild(effectsRoot);
        preparations.start();
        arrivals.start();
    }

    @Override
    public void update(float tpf) {
        preparations.update();
        arrivals.update();
        transientEffects.removeIf(effect -> {
            if (effect.node().getQuantity() == 0) {
                effect.node().removeFromParent();
                return true;
            }
            applyFogVisibility(effect.node(), effect.tile());
            return false;
        });
    }

    @Override
    public void cleanup() {
        preparations.stop();
        arrivals.stop();
        transientEffects.forEach(effect -> effect.visualEffect().removeEffect());
        transientEffects.clear();
        effectsRoot.detachAllChildren();
        effectsRoot.removeFromParent();
        super.cleanup();
    }

    private void applyFogVisibility(Node anchor, Point tile) {
        anchor.setCullHint(fog.isVisible(tile) ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
    }

    private void loadTransientEffect(EntityId source, Vector3f location, int effectId, short ownerId, boolean continuousGeneration) {
        if (effectId == 0) {
            return;
        }
        Node anchor = new Node("Portal effect");
        anchor.setLocalTranslation(location);
        effectsRoot.attachChild(anchor);
        Point tile = WorldUtils.vectorToPoint(location);
        applyFogVisibility(anchor, tile);
        VisualEffect visualEffect = stateManager.getState(EffectManagerState.class).load(anchor, null, effectId, false, ownerId);
        if (continuousGeneration) {
            visualEffect.generateThroughoutLifetime();
        }
        transientEffects.add(new TransientEffect(source, anchor, tile, visualEffect));
    }

    private void removeTransientEffects(EntityId source) {
        transientEffects.removeIf(effect -> {
            if (effect.source().equals(source)) {
                effect.visualEffect().removeEffect();
                effect.node().removeFromParent();
                return true;
            }
            return false;
        });
    }

    private final class PortalEffectContainer extends EntityContainer<double[]> {
        PortalEffectContainer(EntityData ed) {
            super(ed, PortalViewState.class, RoomComponent.class, Owner.class);
        }

        @Override
        protected double[] addObject(Entity e) {
            double[] previous = {-1};
            updateObject(previous, e);
            return previous;
        }

        @Override
        protected void updateObject(double[] previous, Entity e) {
            double preparationTime = e.get(PortalViewState.class).preparationTime;
            RoomComponent room = e.get(RoomComponent.class);
            if (room.destroyed || preparationTime < 0) {
                removeObject(previous, e);
            } else if (preparationTime != previous[0]) {
                List<Integer> effects = kwdFile.getRoomById(room.roomId).getEffects();
                if (effects.size() > 2) {
                    loadTransientEffect(e.getId(), WorldUtils.pointToVector3f(room.location),
                            effects.get(2), e.get(Owner.class).ownerId, true);
                }
            }
            previous[0] = preparationTime;
        }

        @Override
        protected void removeObject(double[] previous, Entity e) {
            removeTransientEffects(e.getId());
        }
    }

    private final class CreatureEntranceContainer extends EntityContainer<double[]> {
        CreatureEntranceContainer(EntityData entityData) {
            super(entityData, Position.class, CreatureViewState.class, Owner.class);
        }

        @Override
        protected double[] addObject(Entity entity) {
            double[] previous = {Double.NaN};
            updateObject(previous, entity);
            return previous;
        }

        @Override
        protected void updateObject(double[] previous, Entity entity) {
            CreatureViewState view = entity.get(CreatureViewState.class);
            if (view.state == Creature.AnimationType.ENTRANCE && view.stateStartTime != previous[0]) {
                loadTransientEffect(entity.getId(), entity.get(Position.class).position.clone()
                        .setY(WorldUtils.UNDERFLOOR_HEIGHT), kwdFile.getCreature(view.creatureId).getEntranceEffectId(),
                        entity.get(Owner.class).ownerId, false);
                previous[0] = view.stateStartTime;
            }
        }

        @Override
        protected void removeObject(double[] previous, Entity entity) {
            removeTransientEffects(entity.getId());
        }
    }
}
