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
package toniarts.openkeeper.view.control;

import com.jme3.asset.AssetManager;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Collection;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.PlayerObjective;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * Unit flower control for creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureFlowerControl extends UnitFlowerControl<Creature> {

    private enum Status {

        LEVEL, STATUS
    }

    private static final float CHANGE_STATUS_INTERVAL = 0.5f;
    private static final float REDRAW_INTERVAL = 0.1f;

    private float timeCurrentStatusVisible = 0;
    private float timeCurrentVisible = 0;
    private Status currentStatus = Status.LEVEL;

    public CreatureFlowerControl(EntityId entityId, EntityData entityData, Creature data, AssetManager assetManager) {
        super(entityId, entityData, data, assetManager);
    }

    @Override
    protected Collection<Class<? extends EntityComponent>> getWatchedComponents() {
        Collection<Class<? extends EntityComponent>> components = super.getWatchedComponents();
        components.add(CreatureAi.class);
        components.add(CreatureComponent.class);
        components.add(TaskComponent.class);
        components.add(PlayerObjective.class);
        components.add(PortalGem.class);
        components.add(CreatureExperience.class);
        return components;
    }

    @Override
    protected float getHeight() {
        CreatureComponent creatureComponent = getEntity().get(CreatureComponent.class);
        if (creatureComponent == null) {
            return 0;
        }
        return creatureComponent.height;
    }

    @Override
    public void onHide() {
        timeCurrentStatusVisible = 0;
        timeCurrentVisible = 0;
        currentStatus = Status.LEVEL;
    }

    @Override
    protected boolean onUpdate(float tpf) {
        timeCurrentStatusVisible += tpf;
        timeCurrentVisible += tpf;
        if (timeCurrentStatusVisible >= CHANGE_STATUS_INTERVAL) {
            timeCurrentStatusVisible = 0;
            changeStatus();
            return true;
        }

        // Redraw often
        if (timeCurrentVisible >= REDRAW_INTERVAL) {
            timeCurrentVisible = 0;
            return true;
        }

        return false;
    }

    private void changeStatus() {
        if (currentStatus == Status.LEVEL) {
            currentStatus = Status.STATUS;
        } else {
            currentStatus = Status.LEVEL;
        }
    }

    @Override
    protected String getCenterIcon() {
        CreatureAi creatureAi = getEntity().get(CreatureAi.class);

        if (currentStatus == Status.STATUS && creatureAi != null) {
            switch (creatureAi.getCreatureState()) {
                case FIGHT:
                case MELEE_ATTACK: {
                    return "Textures/GUI/moods/SJ-Fighting.png";
                }
                case WORK: {
                    TaskComponent taskComponent = getEntity().get(TaskComponent.class);
                    if (taskComponent != null) {
                        String icon = getTaskIcon(taskComponent.taskType);
                        if (icon != null) {
                            return icon;
                        }
                    }
                    break;
                }
                case FLEE: {
                    return "Textures/GUI/moods/ST-Fear.png";
                }
                case DRAGGED:
                case UNCONSCIOUS: {
                    return "Textures/GUI/moods/SJ-Unconscious.png";
                }
                case STUNNED: {
                    return "Textures/GUI/moods/SJ-Stunned.png";
                }
                case SLEEPING:
                case RECUPERATING: {
                    return "Textures/GUI/moods/SJ-Rest.png";
                }
                case TORTURED: {
                    return "Textures/GUI/moods/SJ-Torture.png";
                }
                case IMPRISONED: {
                    return "Textures/GUI/moods/SJ-Prison.png";
                }
            }
        }

        CreatureExperience creatureExperience = getEntity().get(CreatureExperience.class);
        if (creatureExperience == null) {
            return null;
        }

        // Level icon if nothing is found
        return "Textures/GUI/moods/SL-" + String.format("%02d", creatureExperience.level) + ".png";
    }

    private static String getTaskIcon(TaskType taskType) {
        switch (taskType) {
            case CARRY_CREATURE_TO_LAIR:
            case CAPTURE_ENEMY_CREATURE:
            case CARRY_CREATURE_TO_JAIL:
            case RESCUE_CREATURE:
                return "Textures/GUI/moods/SJ-Take_Crate.png";
            case CARRY_GOLD_TO_TREASURY:
                return "Textures/GUI/moods/SJ-Take_Gold.png";
            case CLAIM_TILE:
            case CLAIM_ROOM:
            case FETCH_OBJECT:
                return "Textures/GUI/moods/SJ-Claim.png";
            case REPAIR_WALL:
            case CLAIM_WALL:
                return "Textures/GUI/moods/SJ-Reinforce.png";
            case DIG_TILE:
                return "Textures/GUI/moods/SJ-Dig.png";
            case CLAIM_LAIR:
            case GO_TO_SLEEP:
                return "Textures/GUI/moods/SJ-Rest.png";
            case RESEARCH_SPELL:
                return "Textures/GUI/moods/SJ-Library.png";
            default:
                return null;
        }
    }

    @Override
    protected void onTextureGenerated(Graphics2D g) {
        CreatureExperience creatureExperience = getEntity().get(CreatureExperience.class);
        if (creatureExperience == null) {
            return;
        }

        // Calculate the angle
        int angle = (int) ((float) creatureExperience.experience / creatureExperience.experienceToNextLevel * 360);

        // Draw the experience indicator
        g.setPaint(new Color(0, 0, 0, 100));
        g.fillArc(
                22, 22, 20, 20, 90, 360 - angle);
    }

    @Override
    protected String getObjectiveIcon() {
        PlayerObjective playerObjective = getEntity().get(PlayerObjective.class);
        if (playerObjective != null && playerObjective.objective != null) {
            switch (playerObjective.objective) {
                case CONVERT:
                case IMPRISON:
                    return "Textures/GUI/moods/Imprison.png";
                case KILL:

                    // Hmm, is this so...?
                    PortalGem portalGem = getEntity().get(PortalGem.class);
                    if (portalGem != null) {
                        return "Textures/GUI/moods/Objective.png";
                    } else {
                        return "Textures/GUI/moods/Objective-2.png";
                    }
            }
        }
        return null;
    }

}
