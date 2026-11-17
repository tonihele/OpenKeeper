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
     * Shot 15 as it ships in the original data
     */
    public static final short INFERNO_ID = 15;

    /**
     * Shot 1 as it ships in the original data, the Warlock's and the Wizard's
     * fireball
     */
    public static final short FIREBALL_ID = 1;

    /**
     * The fireball's effect when it hits a thing, "Spot Hit 4"
     */
    public static final int FIREBALL_HIT_EFFECT_ID = 234;

    /**
     * Shot 3 as it ships in the original data, the Bile Demon's Gas Cloud
     */
    public static final short GAS_CLOUD_ID = 3;

    /**
     * Shot 23 as it ships in the original data, the Bile Demon's Gas Missile
     */
    public static final short GAS_MISSILE_ID = 23;

    /**
     * Shot 55 as it ships in the original data, the cloud the Gas Missile
     * leaves
     */
    public static final short GAS_MISSILE_CLOUD_ID = 55;

    /**
     * Shot 18 as it ships in the original data, the Elven Archer's Grenade
     */
    public static final short GRENADE_ID = 18;

    /**
     * Shot 19 as it ships in the original data, the Grenade's explosion
     */
    public static final short GRENADE_EXPLOSION_ID = 19;

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
        shot.setHitThingEffectId(FIREBALL_HIT_EFFECT_ID);
        shot.setFlags(EnumSet.of(Shot.ShotFlag.DIE_WHEN_HIT_SOLID, Shot.ShotFlag.DIE_WHEN_HIT_THING, Shot.ShotFlag.DIE_OVER_TIME,
                Shot.ShotFlag.MOVE_TOWARDS_TARGET, Shot.ShotFlag.RECOIL));
        return shot;
    }

    /**
     * The Gas Cloud or the Gas Missile's cloud, with the values of the
     * original data
     *
     * @param shotId {@link #GAS_CLOUD_ID} or {@link #GAS_MISSILE_CLOUD_ID}
     */
    public static Shot gasCloud(short shotId) {
        Shot shot = shot(shotId, Shot.ProcessType.GAS, 5, EnumSet.noneOf(Shot.ShotProcessFlag.class));
        shot.setSpeed(1f);
        shot.setRadius(0.2f);
        shot.setCollideType(Shot.CollideType.ENEMY_CREATURES);
        shot.setDamageType(Shot.DamageType.RANGE_FROM_ORIGIN);
        shot.setFlags(EnumSet.of(Shot.ShotFlag.DIE_OVER_TIME));
        return shot;
    }

    /**
     * The Gas Missile, with the values of the original data
     */
    public static Shot gasMissile() {
        Shot shot = shot(GAS_MISSILE_ID, Shot.ProcessType.NONE, 16, EnumSet.noneOf(Shot.ShotProcessFlag.class));
        shot.setSpeed(0.5f);
        shot.setRadius(0.2f);
        shot.setCollideType(Shot.CollideType.ENEMY_CREATURES);
        shot.setDamageType(Shot.DamageType.NONE);
        shot.setHitSolidShotId(GAS_MISSILE_CLOUD_ID);
        shot.setHitThingShotId(GAS_MISSILE_CLOUD_ID);
        shot.setFlags(EnumSet.of(Shot.ShotFlag.DIE_WHEN_HIT_SOLID, Shot.ShotFlag.DIE_WHEN_HIT_THING, Shot.ShotFlag.MOVE_TOWARDS_TARGET,
                Shot.ShotFlag.RECOIL));
        return shot;
    }

    /**
     * The Grenade, with the values of the original data
     */
    public static Shot grenade() {
        Shot shot = shot(GRENADE_ID, Shot.ProcessType.NONE, 16, EnumSet.noneOf(Shot.ShotProcessFlag.class));
        shot.setSpeed(0.3f);
        shot.setRadius(0.2f);
        shot.setCollideType(Shot.CollideType.NONE);
        shot.setDamageType(Shot.DamageType.NONE);
        shot.setDeathShotId(GRENADE_EXPLOSION_ID);
        shot.setFlags(EnumSet.of(Shot.ShotFlag.DIE_OVER_TIME, Shot.ShotFlag.BOUNCE, Shot.ShotFlag.THROWN_PROJECTILE,
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

    /**
     * The keeper's Inferno, with the values of the original data
     */
    public static Shot inferno() {
        return shot(INFERNO_ID, Shot.ProcessType.INFERNO, 30, EnumSet.of(Shot.ShotProcessFlag.CONSTANT));
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
