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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.awt.Color;
import java.awt.Graphics2D;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
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
    protected float getHeight() {
        CreatureComponent creatureComponent = getEntityData().getComponent(getEntityId(), CreatureComponent.class);
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
        CreatureAi creatureAi = getEntityData().getComponent(getEntityId(), CreatureAi.class);

        if (currentStatus == Status.STATUS && creatureAi != null) {
            switch (creatureAi.getCreatureState()) {
                case FIGHT: {
                    return "Textures/GUI/moods/SJ-Fighting.png";
                }
                case WORK: {
                    //String icon = creatureControl.getAssignedTask().getTaskIcon();
//                    if (icon != null) {
//                        return icon;
//                    }
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

        CreatureComponent creatureComponent = getEntityData().getComponent(getEntityId(), CreatureComponent.class);
        if (creatureComponent == null) {
            return null;
        }

        // Level icon if nothing is found
        return "Textures/GUI/moods/SL-" + String.format("%02d", creatureComponent.level) + ".png";
    }

    @Override
    protected void onTextureGenerated(Graphics2D g) {
        CreatureComponent creatureComponent = getEntityData().getComponent(getEntityId(), CreatureComponent.class);
        if (creatureComponent == null) {
            return;
        }

        // Calculate the angle
        int angle = (int) ((float) creatureComponent.experience / creatureComponent.experienceToNextLevel * 360);

        // Draw the experience indicator
        g.setPaint(new Color(0, 0, 0, 100));
        g.fillArc(
                22, 22, 20, 20, 90, 360 - angle);
    }

    @Override
    protected String getObjectiveIcon() {
//        if (creatureControl.getPlayerObjective() != null) {
//            switch (creatureControl.getPlayerObjective()) {
//                case CONVERT:
//                case IMPRISON:
//                    return "Textures/GUI/moods/Imprison.png";
//                case KILL:
//
//                    // Hmm, is this so...?
//                    if (creatureControl.isPortalGem()) {
//                        return "Textures/GUI/moods/Objective.png";
//                    } else {
//                        return "Textures/GUI/moods/Objective-2.png";
//                    }
//            }
//        }
        return null;
    }

}
