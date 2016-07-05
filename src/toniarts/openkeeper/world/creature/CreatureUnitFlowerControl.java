/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.world.creature;

import com.jme3.asset.AssetManager;
import java.awt.Color;
import java.awt.Graphics2D;
import toniarts.openkeeper.world.control.AbstractUnitFlowerControl;

/**
 * Unit flower control for creatures
 *
 * @author toni
 */
public class CreatureUnitFlowerControl extends AbstractUnitFlowerControl {

    private enum Status {
        LEVEL, STATUS
    }

    private static final float CHANGE_STATUS_INTERVAL = 0.5f;
    private static final float REDRAW_INTERVAL = 0.1f;
    private float timeCurrentStatusVisible = 0;
    private float timeCurrentVisible = 0;
    private Status currentStatus = Status.LEVEL;
    private final CreatureControl creatureControl;

    public CreatureUnitFlowerControl(AssetManager assetManager, CreatureControl creatureControl) {
        super(assetManager);
        this.creatureControl = creatureControl;
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
        if (currentStatus == Status.STATUS) {
            switch (creatureControl.stateMachine.getCurrentState()) {
                case FIGHT: {
                    return "Textures/GUI/moods/SJ-Fighting.png";
                }
                case WORK: {
                    String icon = creatureControl.getAssignedTask().getTaskIcon();
                    if (icon != null) {
                        return icon;
                    }
                    break;
                }
            }
        }

        // Level icon if nothing is found
        return "Textures/GUI/moods/SL-" + String.format("%02d", creatureControl.getLevel()) + ".png";
    }

    @Override
    protected void onTextureGenerated(Graphics2D g) {

        // Calculate the angle
        int angle = (int) ((float) creatureControl.getExperience() / creatureControl.getExperienceToNextLevel() * 360);

        // Draw the experience indicator
        g.setPaint(new Color(0, 0, 0, 100));
        g.fillArc(
                22, 22, 20, 20, 90, 360 - angle);
    }

    @Override
    protected short getOwnerId() {
        return creatureControl.getOwnerId();
    }

    @Override
    protected int getHealthMax() {
        return creatureControl.getCreature().getHp();
    }

    @Override
    protected int getHealthCurrent() {
        return creatureControl.getHealth();
    }

    @Override
    protected float getHeight() {
        return creatureControl.getCreature().getHeight();
    }

}
