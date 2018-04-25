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
package toniarts.openkeeper.world.control;

import com.jme3.scene.control.Control;

/**
 * Simple interface for controls having a flower
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IUnitFlowerControl extends Control {

    /**
     * Get the unit owner id, for the color
     *
     * @return the unit player id
     */
    short getOwnerId();

    /**
     * Get unit max health
     *
     * @return max health
     */
    int getMaxHealth();

    /**
     * Get current unit health
     *
     * @return unit current health
     */
    int getHealth();

    /**
     * Get the center icon resource as a string resource path
     *
     * @return the center icon
     */
    String getCenterIcon();

    /**
     * Get unit height, to correctly position the flower
     *
     * @return the unit height
     */
    float getHeight();

    /**
     * Get percentage of health
     *
     * @return human formatted percentage
     */
    default int getHealthPercentage() {
        return (int) ((getHealth() * 100.0f) / getMaxHealth());
    }

}
