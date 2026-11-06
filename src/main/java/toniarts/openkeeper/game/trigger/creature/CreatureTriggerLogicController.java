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
package toniarts.openkeeper.game.trigger.creature;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.trigger.AbstractThingTriggerControl;
import toniarts.openkeeper.game.trigger.AbstractThingTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 * A state for handling creature triggers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureTriggerLogicController extends AbstractThingTriggerLogicController<ICreatureController> {

    private final EntityData entityData;
    private final CreatureTriggerEventQueue pendingEvents;
    private final ICreaturesController creaturesController;

    public CreatureTriggerLogicController(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final PlayerService playerService, final EntityData entityData) {
        this(gameController, levelInfo, gameTimer, mapController, creaturesController, playerService,
                entityData, new CreatureTriggerEventQueue());
    }

    public CreatureTriggerLogicController(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final PlayerService playerService, final EntityData entityData,
            final CreatureTriggerEventQueue pendingEvents) {
        super(initTriggers(levelInfo.getLevelData(), gameController, levelInfo, gameTimer, mapController,
                creaturesController, playerService),
                entityData.getEntities(CreatureComponent.class, Trigger.class),
                creaturesController);
        this.entityData = entityData;
        this.pendingEvents = pendingEvents;
        this.creaturesController = creaturesController;
    }

    /** Runs the event now, independently of the ordinary trigger traversal. */
    public void onAngry(EntityId entityId) {
        onEvent(entityId, TriggerGeneric.TargetType.CREATURE_ANGRY);
    }

    /** Runs a qualifying fear event now, even if no escape follows. */
    public void onAfraid(EntityId entityId) {
        onEvent(entityId, TriggerGeneric.TargetType.CREATURE_AFRAID);
    }

    /** Called by hand/session threads; keep every accepted request, even at the same time. */
    public void enqueueSlapped(EntityId entityId) {
        pendingEvents.enqueueSlapped(entityId);
    }

    /** Lightning can arrive on a session thread, so only enqueue after state is stored. */
    public void enqueueStunned(EntityId entityId) {
        pendingEvents.enqueueStunned(entityId);
    }

    @Override
    public void processTick(float tpf) {
        pendingEvents.drain(this::onEvent);
        super.processTick(tpf);
    }

    @Override
    public void stop() {
        pendingEvents.clear();
        super.stop();
    }

    private void onEvent(EntityId entityId, TriggerGeneric.TargetType eventType) {
        Trigger binding = entityData.getComponent(entityId, Trigger.class);
        if (binding == null || entityData.getComponent(entityId, CreatureComponent.class) == null) {
            return;
        }
        CreatureTriggerControl control = (CreatureTriggerControl) getTriggerControl(binding.triggerId);
        if (control != null) {
            control.onEvent(eventType, creaturesController.createController(entityId));
        }
    }

    private static Map<Integer, AbstractThingTriggerControl<ICreatureController>> initTriggers(IKwdFile kwdFile, final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final PlayerService playerService) {
        Map<Integer, AbstractThingTriggerControl<ICreatureController>> creatureTriggers = new HashMap<>();
        for (Thing.GoodCreature creature : kwdFile.getThings(Thing.GoodCreature.class)) {
            if (creature.getTriggerId() != 0) {
                creatureTriggers.put(creature.getTriggerId(), new CreatureTriggerControl(gameController, levelInfo, gameTimer, mapController,
                        creaturesController, creature.getTriggerId(), Player.GOOD_PLAYER_ID, playerService));
            }
        }
        for (Thing.KeeperCreature creature : kwdFile.getThings(Thing.KeeperCreature.class)) {
            if (creature.getTriggerId() != 0) {
                creatureTriggers.put(creature.getTriggerId(), new CreatureTriggerControl(gameController, levelInfo, gameTimer, mapController,
                        creaturesController, creature.getTriggerId(), creature.getPlayerId(), playerService));
            }
        }
        for (Thing.NeutralCreature creature : kwdFile.getThings(Thing.NeutralCreature.class)) {
            if (creature.getTriggerId() != 0) {
                creatureTriggers.put(creature.getTriggerId(), new CreatureTriggerControl(gameController, levelInfo, gameTimer, mapController,
                        creaturesController, creature.getTriggerId(), Player.NEUTRAL_PLAYER_ID, playerService));
            }
        }
        for (Thing.HeroParty heroParty : kwdFile.getThings(Thing.HeroParty.class)) {
            for (Thing.GoodCreature creature : heroParty.getHeroPartyMembers()) {
                if (creature.getTriggerId() != 0) {
                    creatureTriggers.put(creature.getTriggerId(), new CreatureTriggerControl(gameController, levelInfo, gameTimer, mapController,
                            creaturesController, creature.getTriggerId(), Player.GOOD_PLAYER_ID, playerService));
                }
            }
        }
        return creatureTriggers;
    }

}
