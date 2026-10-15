/*
 * Copyright (C) 2014-2015 OpenKeeper
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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
import java.util.Map;
import java.util.function.Predicate;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.ObjectiveType;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.trigger.AbstractThingTriggerControl;
import toniarts.openkeeper.game.trigger.TriggerActionData;
import toniarts.openkeeper.game.trigger.TriggerData;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 * The creature scope: the trigger tree of a single creature. Forwards what it
 * does not know to the owner player's scope, and from there to the level.
 *
 * @author ArchDemon
 */
public final class CreatureTriggerControl extends AbstractThingTriggerControl<ICreatureController> {

    /**
     * The creature events that we notice by the creature's state. Each fires
     * when the state turns on.
     */
    private final Map<TriggerGeneric.TargetType, Predicate<ICreatureController>> eventStates = new EnumMap<>(TriggerGeneric.TargetType.class);
    private final Map<TriggerGeneric.TargetType, Boolean> lastEventStates = new EnumMap<>(TriggerGeneric.TargetType.class);
    private boolean createdPending = false;
    private boolean seedPending = false;
    private final Set<TriggerGeneric.TargetType> watchedEvents = EnumSet.noneOf(TriggerGeneric.TargetType.class);
    private final boolean heroPartyMember;

    /**
     * @param heroPartyMember is this a tree of a hero party member, the party
     * member trees are armed when the member is spawned
     */
    public CreatureTriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final int triggerId, final short playerId,
            final PlayerService playerService, final boolean heroPartyMember) {
        super(gameController, levelInfo, gameTimer, mapController, creaturesController, triggerId, playerId, playerService);

        eventStates.put(TriggerGeneric.TargetType.CREATURE_SLAPPED, ICreatureController::isSlapped);
        eventStates.put(TriggerGeneric.TargetType.CREATURE_ATTACKED, ICreatureController::isAttacked);
        eventStates.put(TriggerGeneric.TargetType.CREATURE_IMPRISONED, ICreatureController::isImprisoned);
        eventStates.put(TriggerGeneric.TargetType.CREATURE_TORTURED, ICreatureController::isTortured);
        eventStates.put(TriggerGeneric.TargetType.CREATURE_CONVERTED, creature -> creature.getOwnerId() != getPlayerId());
        eventStates.put(TriggerGeneric.TargetType.CREATURE_CLAIMED, ICreatureController::isClaimed);
        eventStates.put(TriggerGeneric.TargetType.CREATURE_STUNNED, ICreatureController::isStunned);
        eventStates.put(TriggerGeneric.TargetType.CREATURE_DYING, ICreatureController::isUnconscious);
        eventStates.put(TriggerGeneric.TargetType.CREATURE_HUNGER_SATED, creature -> !creature.isHungry());
        eventStates.put(TriggerGeneric.TargetType.CREATURE_PICKS_UP_PORTAL_GEM, ICreatureController::isPortalGemInPosession);
        eventStates.put(TriggerGeneric.TargetType.CREATURE_PICKED_UP, ICreatureController::isPickedUp);

        // Events are only matched at the top level of the tree, so those are the only ones to watch
        for (TriggerData node : root.getChildren().getArray()) {
            if (!(node instanceof TriggerGenericData condition)) {
                break;
            }
            if (eventStates.containsKey(condition.getType())) {
                watchedEvents.add(condition.getType());
            }
        }

        this.heroPartyMember = heroPartyMember;
        if (heroPartyMember) {

            // Dormant until the member is spawned, see setThing
            setTreeArmed(false);
        }
    }

    @Override
    protected void setThing(ICreatureController thingInstance) {
        super.setThing(thingInstance);

        // What the creature is like when it appears is not an event, only the changes after that are.
        // The states are read on the first poll, the creature trees get their creatures while the game
        // is still being put together, and the creature may not be ready to be asked yet
        lastEventStates.clear();
        if (thingInstance != null) {
            seedPending = true;
            createdPending = true;
            if (heroPartyMember) {
                setTreeArmed(true);
            }
        }
    }

    @Override
    protected void pollEvents() {
        super.pollEvents();

        boolean dead = instanceControl.isDead();
        if (seedPending) {
            seedPending = false;
            lastEventStates.put(TriggerGeneric.TargetType.CREATURE_KILLED, dead);
            if (!dead) {
                watchedEvents.forEach(event -> lastEventStates.put(event, eventStates.get(event).test(instanceControl)));
            }
        }

        if (createdPending) {
            createdPending = false;
            fire(TriggerGeneric.TargetType.CREATURE_CREATED);
        }

        // The death is the last event, the data of a dead creature is gone
        Boolean wasDead = lastEventStates.put(TriggerGeneric.TargetType.CREATURE_KILLED, dead);
        if (dead) {
            if (!Boolean.TRUE.equals(wasDead)) {
                fire(TriggerGeneric.TargetType.CREATURE_KILLED);
            }
            return;
        }

        // Only the events that the tree listens to are watched
        for (TriggerGeneric.TargetType event : watchedEvents) {
            boolean now = eventStates.get(event).test(instanceControl);
            Boolean before = lastEventStates.put(event, now);
            if (now && !Boolean.TRUE.equals(before)) {
                fire(event);
            }
        }
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        float target;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case CREATURE_HEALTH:
            case CREATURE_GOLD_HELD:
            case CREATURE_EXPERIENCE_LEVEL:
                if (instanceControl.isDead()) {
                    return false; // Nothing left to measure
                }
                break;
            default:
                break;
        }

        switch (targetType) {
            case CREATURE_HEALTH:
                target = ((float) instanceControl.getHealth() / instanceControl.getMaxHealth()) * 100; // Percentage
                break;
            case CREATURE_GOLD_HELD:
                target = (instanceControl.getMaxGold() > 0) ? ((float) instanceControl.getGold() / instanceControl.getMaxGold()) * 100 : 0; // Percentage
                break;
            case CREATURE_EXPERIENCE_LEVEL:
                target = instanceControl.getLevel();
                break;

            // The events, false when walked, see pollEvents()
            case CREATURE_CREATED:
            case CREATURE_KILLED:
            case CREATURE_SLAPPED:
            case CREATURE_ATTACKED:
            case CREATURE_IMPRISONED:
            case CREATURE_TORTURED:
            case CREATURE_CONVERTED:
            case CREATURE_CLAIMED:
            case CREATURE_STUNNED:
            case CREATURE_DYING:
            case CREATURE_HUNGER_SATED:
            case CREATURE_PICKS_UP_PORTAL_GEM:
            case CREATURE_PICKED_UP:
                return false;

            case CREATURE_ANGRY:
            case CREATURE_AFRAID:
                // STUB, events: the creature became angry / afraid. The state changes can not be told
                // from the outside: the anger is a set of counters (CreatureMood) with no "became
                // angry" threshold, and the fear of the creature is a state of its AI
                logStub(trigger, "The state changes can not be told from the outside: the anger is a set of counters (CreatureMood) with no 'became angry' threshold, and the fear of the creature is a state of its AI");
                return false;

            case CREATURE_STEALS:
                // STUB, event: the creature stole gold / a spell / a crate. The stealing job does not
                // report to anybody
                logStub(trigger, "The stealing job does not report to anybody");
                return false;

            case CREATURE_LEAVES:
            case CREATURE_SACKED:
                // STUB, events: the creature left the dungeon (leaves has no producer in the original
                // either) / was sacked. A creature leaving is not announced in the game logic
                logStub(trigger, "A creature leaving is not announced in the game logic");
                return false;

            default:
                return super.isActive(trigger);
        }

        return compare(trigger, target, trigger.getUserData("value", int.class));
    }

    @Override
    protected boolean doAction(TriggerActionData trigger) {
        TriggerAction.ActionType type = trigger.getType();

        // Some triggers are bound to the creature itself
        switch (type) {
            case ATTACH_PORTAL_GEM:
                instanceControl.attachPortalGem();
                return true;

            case FORCE_FIRST_PERSON:
                // Possess the creature, for the local player (the one who can possess anything)
                getPlayerService().setPossession(instanceControl.getEntityId(), getLocalPlayerId());
                return true;

            case MAKE_HUNGRY:
                instanceControl.makeHungry();
                return true;

            case SHOW_HEALTH_FLOWER:
                getPlayerService().showUnitFlower(instanceControl.getEntityId(), trigger.getUserData("value", Integer.class), getPlayer().getId());
                return true;

            case ALTER_SPEED:
                // STUB: 0 = walk, otherwise run. The creature controller has no movement speed to
                // set, the speed comes from the creature's state and its attributes
                logStub(trigger, "The creature controller has no movement speed to set, the speed comes from the creature's state and its attributes");
                return true;

            case REMOVE_FROM_MAP:
                // STUB: mode 0 removes the creature silently, 1 kills it. The creature controller
                // has no way to remove or kill a creature from the outside, it dies by its health
                logStub(trigger, "The creature controller has no way to remove or kill a creature from the outside, it dies by its health");
                return true;

            case SET_FIGHT_FLAG:
                // STUB: 0 = don't fight, otherwise fight. Whether to fight is decided by the
                // creature's AI, there is no flag to override it
                logStub(trigger, "Whether to fight is decided by the creature's AI, there is no flag to override it");
                return true;

            case ZOOM_TO:
                getPlayerService().zoomViewToEntity(instanceControl.getEntityId(), getPlayer().getId());
                return true;

            case SET_OBJECTIVE: // Creature part. Only for Good player
                short playerId = trigger.getUserData("playerId", short.class);
                Thing.HeroParty.Objective jobType = ConversionUtils.parseEnum(trigger.getUserData("type", short.class), Thing.HeroParty.Objective.class);
                int apId = trigger.getUserData("actionPointId", int.class);

                // Heroes only, and not the ones that are prisoners or out cold
                if (instanceControl.getOwnerId() == Player.GOOD_PLAYER_ID
                        && !instanceControl.isImprisoned() && !instanceControl.isUnconscious()) {
                    if (apId != 0) {
                        instanceControl.setObjectiveTargetActionPointId(apId);
                    }
                    instanceControl.setObjectiveTargetPlayerId(playerId);
                    instanceControl.setObjective(jobType);
                }
                return true;

            case MAKE_OBJECTIVE: // Game part
                short targetId = trigger.getUserData("targetId", short.class);
                if (targetId == 0) {
                    super.makeObjectiveOff(trigger);
                }

                //0 = Off, 1 = Kill, 2 = Imprison, 3 = Convert;
                switch (targetId) {
                    case 0:
                        instanceControl.setPlayerObjective(null);
                        break;
                    case 1:
                        instanceControl.setPlayerObjective(ObjectiveType.KILL);
                        break;
                    case 2:
                        instanceControl.setPlayerObjective(ObjectiveType.IMPRISON);
                        break;
                    case 3:
                        instanceControl.setPlayerObjective(ObjectiveType.CONVERT);
                        break;
                }
                return true;

            default:
                return super.doAction(trigger);
        }
    }

}
