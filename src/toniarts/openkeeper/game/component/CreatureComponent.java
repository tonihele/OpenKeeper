/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;

/**
 * Simple creature component
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureComponent implements EntityComponent {

    public String name;
    public String bloodType;
    public int level;
    public int experience;
    public float height;
    public int fear;
    public int meleeDamage;
    public int pay;
    public int maxGoldHeld;
    public int hungerFill;
    public int manaGenPrayer;
    public int experienceToNextLevel;
    public int experiencePerSecond;
    public int experiencePerSecondTraining;
    public int researchPerSecond; // TODO: Researcher component?
    public int manufacturePerSecond; // TODO: Manufacturer component?
    public int decomposeValue;
    public float speed;
    public float runSpeed;
    public float tortureTimeToConvert;
    public int posessionManaCost;
    public float meleeRecharge;
    public float stunDuration;
    public boolean worker;
    public short creatureId;

}
