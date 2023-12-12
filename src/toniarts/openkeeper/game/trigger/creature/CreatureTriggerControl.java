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

import java.lang.System.Logger;
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
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 *
 * @author ArchDemon
 */
public class CreatureTriggerControl extends AbstractThingTriggerControl<ICreatureController> {

    private static final Logger LOGGER = System.getLogger(CreatureTriggerControl.class.getName());

    public CreatureTriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final int triggerId, final short playerId,
            final PlayerService playerService) {
        super(gameController, levelInfo, gameTimer, mapController, creaturesController, triggerId, playerId, playerService);
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = false;

        float target = 0;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case CREATURE_CREATED:
                return instanceControl != null;
            case CREATURE_KILLED:
                if (instanceControl != null) {
                    return instanceControl.isDead();
                }
                return false;
            case CREATURE_SLAPPED:
                if (instanceControl != null) {
                    return instanceControl.isSlapped();
                }
                return false;
            case CREATURE_ATTACKED:
                if (instanceControl != null) {
                    return instanceControl.isAttacked();
                }
                return false;
            case CREATURE_IMPRISONED:
                if (instanceControl != null) {
                    return instanceControl.isImprisoned();
                }
                return false;
            case CREATURE_TORTURED:
                if (instanceControl != null) {
                    return instanceControl.isTortured();
                }
                return false;
            case CREATURE_CONVERTED:
                return false;
            case CREATURE_CLAIMED:
                if (instanceControl != null) {
                    return instanceControl.isClaimed();
                }
            case CREATURE_ANGRY:
                return false;
            case CREATURE_AFRAID:
                return false;
            case CREATURE_STEALS:
                return false;
            case CREATURE_LEAVES:
                return false;
            case CREATURE_STUNNED:
                if (instanceControl != null) {
                    return instanceControl.isStunned();
                }
                return false;
            case CREATURE_DYING:
                if (instanceControl != null) {
                    return instanceControl.isUnconscious();
                }
                return false;
            case CREATURE_HEALTH:
                if (instanceControl != null) {
                    target = ((float) instanceControl.getHealth() / instanceControl.getMaxHealth()) * 100; // Percentage
                    break;
                }
                return false;
            case CREATURE_GOLD_HELD:
                if (instanceControl != null) {
                    target = instanceControl.getGold();
                    break;
                }
                return false;
            case CREATURE_EXPERIENCE_LEVEL:
                if (instanceControl != null) {
                    target = instanceControl.getLevel();
                    break;
                }
                return false;
            case CREATURE_HUNGER_SATED:
                return !instanceControl.isHungry();
            case CREATURE_PICKS_UP_PORTAL_GEM:
                if (instanceControl != null) {
                    return instanceControl.isPortalGemInPosession();
                }
                return false;
            case CREATURE_SACKED:
                return false;
            case CREATURE_PICKED_UP:
                if (instanceControl != null) {
                    return instanceControl.isPickedUp();
                }
                return false;
            default:
                return super.isActive(trigger);
        }

        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
            result = compare(target, comparisonType, (int) trigger.getUserData("value"));
        }

        return result;
    }

    @Override
    protected void doAction(TriggerActionData trigger) {
        TriggerAction.ActionType type = trigger.getType();

        // Some triggers are bound to the creature itself
        switch (type) {
            case ATTACH_PORTAL_GEM:
                if (instanceControl != null) {
                    instanceControl.attachPortalGem();
                }
                break;

            case MAKE_HUNGRY:
                instanceControl.makeHungry();
                break;

            case SHOW_HEALTH_FLOWER:
                if (instanceControl != null) {
                    getPlayerService().showUnitFlower(instanceControl.getEntityId(), trigger.getUserData("value", Integer.class), getPlayer().getId());
                }
                break;

            case ALTER_SPEED:
                boolean available = trigger.getUserData("available", short.class) != 0; // 0 = Walk, !0 = Run
                break;

            case REMOVE_FROM_MAP:
                break;

            case SET_FIGHT_FLAG:
                available = trigger.getUserData("available", short.class) != 0; // 0 = Don`t Fight, !0 = Fight
                break;

            case ZOOM_TO:
                if (instanceControl != null) {
                    getPlayerService().zoomViewToEntity(instanceControl.getEntityId(), getPlayer().getId());
                }
                break;

            case SET_OBJECTIVE: // Creature part. Only for Good player
                short playerId = trigger.getUserData("playerId", short.class);
                Thing.HeroParty.Objective jobType = ConversionUtils.parseEnum(trigger.getUserData("type", short.class), Thing.HeroParty.Objective.class);
                int apId = trigger.getUserData("actionPointId", int.class);

                // Assign to creature
                if (instanceControl != null) {
                    if (apId != 0) {
                        instanceControl.setObjectiveTargetActionPointId(apId);
                    }
                    instanceControl.setObjectiveTargetPlayerId(playerId);
                    instanceControl.setObjective(jobType);
                }
                break;

            case MAKE_OBJECTIVE: // Game part
                short targetId = trigger.getUserData("targetId", short.class);
                if (targetId == 0) {
                    super.makeObjectiveOff();
                }
                if (instanceControl != null) {

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
                }
                break;

            default:
                super.doAction(trigger);

        }
    }

}
