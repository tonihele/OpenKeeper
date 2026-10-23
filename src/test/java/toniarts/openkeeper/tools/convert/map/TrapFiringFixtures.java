/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.tools.convert.map;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * Builds the data a firing trap needs in tests: traps, their shots, the
 * creatures that set them off and level variables. Lives in the data classes'
 * own package because their setters are {@code protected} (parser-only).
 */
public final class TrapFiringFixtures {

    private TrapFiringFixtures() {
    }

    /**
     * A trap that needs no mana and makes no threat
     *
     * @param trapId the trap ID
     * @param triggerType how the trap notices its victims
     * @param triggerData the trigger range in tiles, for line of sight traps
     * @param rechargeTime seconds between shots
     * @param shotTypeId the shot the trap fires
     * @return the trap
     */
    public static Trap trap(short trapId, Trap.TriggerType triggerType, int triggerData, float rechargeTime, short shotTypeId) {
        Trap trap = new Trap();
        trap.setTrapId(trapId);
        trap.setTriggerType(triggerType);
        trap.setTriggerData(triggerData);
        trap.setRechargeTime(rechargeTime);
        trap.setShotTypeId(shotTypeId);
        trap.setFlags(EnumSet.noneOf(Trap.TrapFlag.class));
        return trap;
    }

    /**
     * Makes the trap pay mana for every shot
     *
     * @param trap the trap
     * @param manaCostToFire the mana a shot costs
     * @return the same trap
     */
    public static Trap withManaCost(Trap trap, int manaCostToFire) {
        trap.getFlags().add(Trap.TrapFlag.REQUIRE_MANA);
        trap.setManaCostToFire(manaCostToFire);
        return trap;
    }

    /**
     * Sets the values the trap hands to its shot
     *
     * @param trap the trap
     * @param shotData1 the first value, damage for damaging shots
     * @param shotData2 the second value, a radius in tiles for area shots
     * @return the same trap
     */
    public static Trap withShotData(Trap trap, int shotData1, int shotData2) {
        trap.setShotData1(shotData1);
        trap.setShotData2(shotData2);
        return trap;
    }

    /**
     * Makes the trap threaten its enemies after it fires
     *
     * @param trap the trap
     * @param threat the threat
     * @param threatDuration seconds the threat lasts
     * @return the same trap
     */
    public static Trap withThreat(Trap trap, int threat, float threatDuration) {
        trap.setThreat(threat);
        trap.setThreatDuration(threatDuration);
        return trap;
    }

    /**
     * Makes the trap wait before it fires at a victim
     *
     * @param trap the trap
     * @param chargeTime seconds a victim has to be seen before the shot
     * @return the same trap
     */
    public static Trap withChargeTime(Trap trap, float chargeTime) {
        trap.setChargeTime(chargeTime);
        return trap;
    }

    /**
     * A shot
     *
     * @param shotId the shot ID
     * @param processType what the shot does
     * @param damageType how the shot deals damage
     * @return the shot
     */
    public static Shot shot(short shotId, Shot.ProcessType processType, Shot.DamageType damageType) {
        Shot shot = new Shot();
        shot.setShotId(shotId);
        shot.setProcessType(processType);
        shot.setDamageType(damageType);
        return shot;
    }

    /**
     * A creature
     *
     * @param creatureId the creature ID
     * @param flags the creature flags
     * @return the creature
     */
    public static Creature creature(short creatureId, Creature.CreatureFlag... flags) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        EnumSet<Creature.CreatureFlag> creatureFlags = EnumSet.noneOf(Creature.CreatureFlag.class);
        creatureFlags.addAll(Arrays.asList(flags));
        creature.setFlags(creatureFlags);
        return creature;
    }

    /**
     * A level variable
     *
     * @param type the variable
     * @param value its value
     * @return the variable
     */
    public static Variable.MiscVariable miscVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

}
