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
package toniarts.openkeeper.world.creature;

import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSpell;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 *
 * Holds a single creature attack ability and its status
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
@Deprecated
public class CreatureAttack {

    private final float rechargeTime;
    private float recharge = 0;
    private final float range;
    private final float shotDelay;
    private final float damage;
    private final boolean playAnimation;
    private final boolean melee;
    private final int levelAvailable;
    private final CreatureSpell creatureSpell;
    private final CreatureControl creature;

    /**
     * Contructs a creature melee base attack
     *
     * @param creature the creature
     */
    public CreatureAttack(CreatureControl creature) {
        this.creature = creature;
        this.rechargeTime = creature.getCreature().getMeleeRecharge();
        this.range = creature.getCreature().getMeleeRange();
        this.shotDelay = 0; // TODO
        this.damage = creature.getCreature().getMeleeDamage();
        this.playAnimation = true; // TODO
        this.melee = true;
        this.levelAvailable = 1;
        this.creatureSpell = null;
    }

    /**
     * Contructs a creature melee base attack
     *
     * @param creature the creature
     * @param spell the creature spell
     * @param kwdFile the data file
     */
    public CreatureAttack(CreatureControl creature, Creature.Spell spell, KwdFile kwdFile) {
        this.creature = creature;
        creatureSpell = kwdFile.getCreatureSpellById(spell.getCreatureSpellId());
        this.rechargeTime = creatureSpell.getRechargeTime();
        this.range = creatureSpell.getRange();
        this.shotDelay = spell.getShotDelay();
        this.damage = creatureSpell.getShotData1(); // FIXME
        this.playAnimation = spell.isPlayAnimation();
        this.melee = false;
        this.levelAvailable = spell.getLevelAvailable();
    }

    /**
     * Recharges the attack by time passed
     *
     * @param tpf the time passed
     */
    protected void recharge(float tpf) {
        if (recharge > 0) {
            recharge -= Math.min(recharge, tpf);
        }
    }

    /**
     * Is the attack available to the creature and ready to be used
     *
     * @return true if available
     */
    public boolean isExecutable() {
        return recharge == 0 && isAvailable();
    }

    /**
     * Is the attack available to the creature
     *
     * @return true if available
     */
    public boolean isAvailable() {
        return creature.getLevel() >= levelAvailable;
    }

    public float getRange() {
        return range;
    }

    public float getDamage() {
        return (melee ? creature.meleeDamage : damage);
    }

    /**
     * Executes the attack
     *
     * @return
     */
    public boolean execute() {
        if (this.isExecutable()) {
            recharge = (melee ? creature.meleeRecharge : rechargeTime);
            return true;
        }

        return false;
    }

    /**
     * Is melee attack
     *
     * @return is melee attack
     */
    public boolean isMelee() {
        return melee;
    }

    /**
     * Is the attack meant for attacking
     *
     * @return is attacking
     */
    public boolean isAttacking() {
        return melee || creatureSpell.getFlags().contains(CreatureSpell.CreatureSpellFlag.IS_ATTACKING);
    }

    public boolean isPlayAnimation() {
        return playAnimation;
    }

}
