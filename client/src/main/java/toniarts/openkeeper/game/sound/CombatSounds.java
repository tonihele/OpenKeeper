/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.sound;

import javax.annotation.Nullable;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Material;

/**
 * The sounds of combat: a melee hit is the impact, picked from the attack type
 * of the attacker and the {@link Material} of the victim, plus the combat grunt
 * of the attacker. A shot that hits a creature has its own impact, split in
 * flesh and everything else. See {@link GameEventSounds} for how they are played.
 */
public final class CombatSounds {

    // Impacts, by the material of the victim
    public static final int IMPACT_METAL2 = 0x112;
    public static final int IMPACT_WOOD = 0x113;
    public static final int IMPACT_FLESH = 0x114;
    public static final int IMPACT_METAL1 = 0x1BB;
    public static final int IMPACT_ROCK = 0x1BC;
    public static final int IMPACT_MAGIC = 0x1BD;

    private CombatSounds() {
        // Static helper
    }

    /**
     * @return the global weapon category of the melee attack type, {@code null}
     * if the attack type has no weapon sounds (e.g. the Wizard's fireball)
     */
    @Nullable
    public static String getWeaponCategory(@Nullable Creature.AttackType attackType) {
        if (attackType == null) {
            return null;
        }
        return switch (attackType) {
            case MELEE_BLADE ->
                GlobalCategory.WEAPON_METAL;
            case MELEE_BLUNT ->
                GlobalCategory.WEAPON_WOOD;
            case MELEE_BODY ->
                GlobalCategory.WEAPON_FLESH;
            case MELEE_SCYTHE ->
                GlobalCategory.WEAPON_SCYTHE;
            default ->
                null;
        };
    }

    /**
     * @return the melee impact event for the material of the victim, {@code -1}
     * if there is none
     */
    public static int getMeleeImpactEvent(@Nullable Material material) {
        if (material == null) {
            return -1;
        }
        return switch (material) {
            case FLESH ->
                IMPACT_FLESH;
            case ROCK ->
                IMPACT_ROCK;
            case WOOD ->
                IMPACT_WOOD;
            case METAL1 ->
                IMPACT_METAL1;
            case METAL2, GLASS ->
                IMPACT_METAL2;
            case MAGIC ->
                IMPACT_MAGIC;
            default ->
                -1;
        };
    }

    /**
     * @return the impact event of a shot that hits a creature: flesh has its
     * own, every other material (also none) takes the second one
     */
    public static int getShotImpactEvent(@Nullable Material material) {
        return material == Material.FLESH ? IMPACT_FLESH : IMPACT_METAL1;
    }
}
