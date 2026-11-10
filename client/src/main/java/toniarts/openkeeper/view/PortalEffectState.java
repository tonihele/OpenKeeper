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
import com.simsilica.es.WatchedEntity;
import com.simsilica.es.filter.FieldFilter;
import com.simsilica.event.EventBus;
import com.simsilica.event.EventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.event.ClientEvents;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.effect.EffectManagerState;
import toniarts.openkeeper.view.effect.VisualEffect;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/** Portal presentation, driven by game events and replicated creature entrance state. */
public final class PortalEffectState extends AbstractAppState {
    private final IKwdFile kwdFile;
    private final Node rootNode;
    private final Node effectsRoot = new Node("Portal effects");
    private final IFogOfWarInformation fog;
    private final EntityData entityData;
    private final ConcurrentLinkedQueue<GameEvent> preparationEvents = new ConcurrentLinkedQueue<>();
    private final List<PendingPreparation> pendingPreparations = new ArrayList<>();
    private final EventListener<GameEvent> eventListener = (type, event) -> {
        if (event.type == GameEventType.PORTAL_PREPARING && event.entity != null
                && event.subjectKind == GameEvent.ThingFlag.ROOM && event.position != null) {
            preparationEvents.add(event);
        }
    };
    private final CreatureEntranceContainer arrivals;
    private final List<TransientEffect> transientEffects = new ArrayList<>();
    private AppStateManager stateManager;

    private record TransientEffect(WatchedEntity source, short ownerId, boolean preparation, Node node, Point tile, VisualEffect visualEffect) {
    }

    private static final class PendingPreparation {
        final GameEvent event;
        final WatchedEntity source;
        float remaining = 3;

        PendingPreparation(GameEvent event, WatchedEntity source) {
            this.event = event;
            this.source = source;
        }
    }

    public PortalEffectState(IKwdFile kwdFile, EntityData entityData, Node rootNode, IFogOfWarInformation fog) {
        this.kwdFile = kwdFile;
        this.rootNode = rootNode;
        this.fog = fog;
        this.entityData = entityData;
        arrivals = new CreatureEntranceContainer(entityData);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.stateManager = stateManager;
        rootNode.attachChild(effectsRoot);
        EventBus.addListener(ClientEvents.GAME_EVENT, eventListener);
        arrivals.start();
    }

    @Override
    public void update(float tpf) {
        for (GameEvent event; (event = preparationEvents.poll()) != null;) {
            pendingPreparations.add(new PendingPreparation(event,
                    entityData.watchEntity(event.entity, RoomComponent.class, Owner.class)));
        }
        pendingPreparations.removeIf(pending -> {
            pending.source.applyChanges();
            RoomComponent room = pending.source.get(RoomComponent.class);
            Owner owner = pending.source.get(Owner.class);
            if (room != null && owner != null) {
                if (!room.destroyed && room.roomId == pending.event.subjectId) {
                    List<Integer> effects = kwdFile.getRoomById(room.roomId).getEffects();
                    if (effects.size() > 2) {
                        loadTransientEffect(pending.event.entity, WorldUtils.pointToVector3f(pending.event.position),
                                effects.get(2), owner.ownerId, true);
                    }
                }
                pending.source.release();
                return true;
            }
            // Events can arrive before their room components replicate. Do not replay stale build-ups.
            pending.remaining -= tpf;
            if (pending.remaining <= 0) {
                pending.source.release();
                return true;
            }
            return false;
        });
        arrivals.update();
        transientEffects.removeIf(effect -> {
            effect.source().applyChanges();
            Owner owner = effect.source().get(Owner.class);
            RoomComponent room = effect.source().get(RoomComponent.class);
            if (owner == null || (effect.preparation()
                    ? room == null || room.destroyed || owner.ownerId != effect.ownerId()
                    : effect.source().get(Position.class) == null)
                    || effect.node().getQuantity() == 0) {
                effect.visualEffect().removeEffect();
                effect.node().removeFromParent();
                effect.source().release();
                return true;
            }
            applyFogVisibility(effect.node(), effect.tile());
            return false;
        });
    }

    @Override
    public void cleanup() {
        EventBus.removeListener(ClientEvents.GAME_EVENT, eventListener);
        preparationEvents.clear();
        pendingPreparations.forEach(pending -> pending.source.release());
        pendingPreparations.clear();
        arrivals.stop();
        transientEffects.forEach(effect -> {
            effect.visualEffect().removeEffect();
            effect.source().release();
        });
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
        WatchedEntity watchedSource = continuousGeneration
                ? entityData.watchEntity(source, RoomComponent.class, Owner.class)
                : entityData.watchEntity(source, Position.class, Owner.class);
        transientEffects.add(new TransientEffect(watchedSource, ownerId, continuousGeneration, anchor, tile, visualEffect));
    }

    private final class CreatureEntranceContainer extends EntityContainer<double[]> {
        CreatureEntranceContainer(EntityData entityData) {
            super(entityData, new FieldFilter(CreatureViewState.class, "state", Creature.AnimationType.ENTRANCE),
                    Position.class, CreatureViewState.class, Owner.class);
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
            // Leaving ENTRANCE releases arrival tracking. The active effect watches its source
            // separately so chained effects can finish, with cleanup if the source disappears.
        }
    }
}
