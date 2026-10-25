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

import java.util.EnumSet;

public final class ShotFixtures {

    /**
     * Shot 21 as it ships in the original data
     */
    public static final short THUNDERBOLT_ID = 21;

    /**
     * Shot 1 as it ships in the original data, the Warlock's and the Wizard's
     * fireball
     */
    public static final short FIREBALL_ID = 1;

    private ShotFixtures() {
    }

    public static Shot shot(short shotId, Shot.ProcessType processType, int health, EnumSet<Shot.ShotProcessFlag> processFlags) {
        Shot shot = new Shot();
        shot.setShotId(shotId);
        shot.setName("Shot " + shotId);
        shot.setProcessType(processType);
        shot.setHealth(health);
        shot.setShotProcessFlags(processFlags);
        shot.setFlags(EnumSet.noneOf(Shot.ShotFlag.class));
        return shot;
    }

    /**
     * The fireball, with the values of the original data
     */
    public static Shot fireball() {
        Shot shot = shot(FIREBALL_ID, Shot.ProcessType.NONE, 60, EnumSet.noneOf(Shot.ShotProcessFlag.class));
        shot.setSpeed(0.9f);
        shot.setRadius(0.2f);
        shot.setCollideType(Shot.CollideType.ENEMY_CREATURES);
        shot.setDamageType(Shot.DamageType.HIT_THING);
        shot.setFlags(EnumSet.of(Shot.ShotFlag.DIE_WHEN_HIT_SOLID, Shot.ShotFlag.DIE_WHEN_HIT_THING, Shot.ShotFlag.DIE_OVER_TIME,
                Shot.ShotFlag.MOVE_TOWARDS_TARGET, Shot.ShotFlag.RECOIL));
        return shot;
    }

    public static GameLevel gameLevel(int ticksPerSecond) {
        GameLevel gameLevel = new GameLevel();
        gameLevel.setTicksPerSec(ticksPerSecond);
        return gameLevel;
    }

    public static void setSize(Creature creature, float width, float height) {
        creature.getAttributes().setWidth(width);
        creature.getAttributes().setHeight(height);
    }

    /**
     * The keeper's Thunderbolt, with the values of the original data
     */
    public static Shot thunderbolt() {
        return shot(THUNDERBOLT_ID, Shot.ProcessType.LIGHTNING, 8, EnumSet.of(Shot.ShotProcessFlag.AT_START));
    }

    public static Creature creature(short creatureId, EnumSet<Creature.CreatureFlag> flags, EnumSet<Creature.CreatureFlag3> flags3) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setName("Creature " + creatureId);
        creature.setFlags(flags);
        creature.setFlags3(flags3);
        return creature;
    }

    public static KeeperSpell keeperSpell(KeeperSpell.CastRule castRule, KeeperSpell.TargetRule targetRule, int manaCost) {
        KeeperSpell keeperSpell = new KeeperSpell();
        keeperSpell.setName("Spell");
        keeperSpell.setCastRule(castRule);
        keeperSpell.setTargetRule(targetRule);
        keeperSpell.setManaCost(manaCost);
        return keeperSpell;
    }

}
