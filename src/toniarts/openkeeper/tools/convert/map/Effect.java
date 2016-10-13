/*
 * Copyright (C) 2014-2015 OpenKeeper
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
import java.util.List;
import toniarts.openkeeper.tools.convert.IFlagEnum;
import toniarts.openkeeper.tools.convert.IValueEnum;
import toniarts.openkeeper.world.effect.IEffect;

/**
 * Container class for Effects.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Effect implements Comparable<Effect>, IEffect {

    /**
     * Effect flags
     */
    public enum EffectFlag implements IFlagEnum {

        DIE_WHEN_HIT_SOLID(0x0001),
        SHRINK(0x0002),
        EXPAND(0x0004),
        DIRECTIONAL_FRICTION(0x0010),
        EXPAND_THEN_SHRINK(0x0020),
        GENERATE_EFFECTS(0x0040), // Generate types
        GENERATE_EFFECT_ELEMENTS(0x0080), // Generate types
        PASS_ON_VELOCITY(0x0100),
        UNIFORM_DISTRIBUTION(0x0200), // Generate types
        RANDOM_DISTRIBUTION(0x0400), // Generate types
        FADE_IN(0x0800),
        FADE_OUT(0x1000),
        FADE_IN_OUT(0x2000),
        USE_GENERATION_SCALE(0x4000),
        /**
         * Triggers even when not in player's view
         */
        ALWAYS_TRIGGER(0x8000);
        private final long flagValue;

        private EffectFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };

    public enum GenerationType implements IValueEnum {

        NONE(0),
        DEFAULT(1),
        ADJACENT_EXPLOSION(2),
        STORM(3),
        CUBE_GEN(4),
        EXPLOSION_2(5),
        SPRAY(6),
        EXPLOSION(7),
        SPIRAL(8),
        EVERYTHING(9),
        CHICKEN_SLAP(10),
        DIG(11),
        SMOKE_PUFF(12),
        FIRE(13),
        GOLD_COST(14),
        TEST_0(15),
        TEST_1(16),
        TEST_2(17),
        TEST_3(18);

        private GenerationType(int id) {
            this.id = id;
        }

        @Override
        public int getValue() {
            return id;
        }
        private final int id;
    }
//    struct EffectBlock {
//        char m_sName[32];
//        ArtResource m_kArtResource;
//        LightBlock m_kLight;
//        int32_t mass; /* 8c 0x1000 = 1.0 */
//        uint32_t air_friction; /* 90 0x10000 = 1.0 */
//        uint32_t elasticity; /* 94 0x10000 = 1.0 */
//        uint32_t radius; /* 98 0x1000 = 1.0 */
//        int32_t min_speed_xy; /* 9c same */
//        int32_t max_speed_xy; /* a0 same */
//        int32_t min_speed_yz; /* a4 same */
//        int32_t max_speed_yz; /* a8 same */
//        uint32_t min_scale; /* ac same */
//        uint32_t max_scale; /* b0 same */
//        uint32_t flags; /* b4 */
//        uint16_t m_wId; /* b8 */
//        uint16_t min_hp; /* ba number of particles emitted in sequence */
//        uint16_t max_hp; /* bc */
//        uint16_t fade_duration; /* be */
//        /* might be a repeatable block */
//        uint16_t next_effect; /* c0 */
//        uint16_t death_effect; /* c2 */
//        uint16_t hit_solid_effect; /* c4 */
//        uint16_t hit_water_effect; /* c6 */
//        uint16_t hit_lava_effect; /* c8 */
//        uint16_t generate_ids[8]; /* ca elements or effects, depending on flags */
//        uint16_t generation_data[12];
//        #if 0
//        uint16_t outer_origin_range; /* da */
//        uint16_t lower_height_limit; /* dc */
//        uint16_t upper_height_limit; /* de */
//        uint16_t orientation_range; /* e0 range of rotation */
//        uint16_t sprite_spin_rate_range; /* e2 range of rotation speed */
//        uint16_t whirlpool_rate; /* e4 */
//        uint16_t directional_spread; /* e6 */
//        uint16_t circular_path_rate; /* e8 possibly sin along path */
//        uint16_t inner_origin_range; /* ea */
//        uint16_t generate_randomness; /* ec */
//        uint16_t misc[2]; /* ee */
//        #endif
//        uint8_t unknown1; /* f2 */
//        uint8_t elements_per_turn; /* f3 */
//        uint16_t unknown3; /* f4 pad? */
//        };
    private String name;
    private ArtResource artResource;
    private Light light;
    private float mass; // 8c 0x1000 = 1.0
    private float airFriction; // 90 0x10000 = 1.0
    private float elasticity; // 94 0x10000 = 1.0
    private float radius; // 98 0x1000 = 1.0
    private float minSpeedXy; // 9c same
    private float maxSpeedXy; // a0 same
    private float minSpeedYz; // a4 same
    private float maxSpeedYz; // a8 same
    private float minScale; // ac same
    private float maxScale; // b0 same
    private EnumSet<EffectFlag> flags; // b4
    private int effectId; // b8
    private int minHp; // ba number of particles emitted in sequence
    private int maxHp; // bc
    private int fadeDuration; // be
// might be a repeatable block
    private int nextEffectId; // c0
    private int deathEffectId; // c2
    private int hitSolidEffectId; // c4
    private int hitWaterEffectId; // c6
    private int hitLavaEffectId; // c8
    // This is all generation data
    private List<Integer> generateIds; // ca elements or effects, depending on flags
    private int outerOriginRange; // da
    private int lowerHeightLimit; // dc
    private int upperHeightLimit; // de
    private int orientationRange; // e0 range of rotation
    private int spriteSpinRateRange; // e2 range of rotation speed
    private int whirlpoolRate; // e4
    private int directionalSpread; // e6
    private int circularPathRate; // e8 possibly sin along path
    private int innerOriginRange; // ea
    private int generateRandomness; // ec
    private int misc2; // ee
    private int misc3;
    private GenerationType generationType; // f2
    // End of generation data
    private short elementsPerTurn; // f3
    private int unknown3; // f4 pad?

    @Override
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    @Override
    public ArtResource getArtResource() {
        return artResource;
    }

    protected void setArtResource(ArtResource artResource) {
        this.artResource = artResource;
    }

    public Light getLight() {
        return light;
    }

    protected void setLight(Light light) {
        this.light = light;
    }

    @Override
    public float getMass() {
        return mass;
    }

    protected void setMass(float mass) {
        this.mass = mass;
    }

    @Override
    public float getAirFriction() {
        return airFriction;
    }

    protected void setAirFriction(float airFriction) {
        this.airFriction = airFriction;
    }

    @Override
    public float getElasticity() {
        return elasticity;
    }

    protected void setElasticity(float elasticity) {
        this.elasticity = elasticity;
    }

    public float getRadius() {
        return radius;
    }

    protected void setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    public float getMinSpeedXy() {
        return minSpeedXy;
    }

    protected void setMinSpeedXy(float minSpeedXy) {
        this.minSpeedXy = minSpeedXy;
    }

    @Override
    public float getMaxSpeedXy() {
        return maxSpeedXy;
    }

    protected void setMaxSpeedXy(float maxSpeedXy) {
        this.maxSpeedXy = maxSpeedXy;
    }

    @Override
    public float getMinSpeedYz() {
        return minSpeedYz;
    }

    protected void setMinSpeedYz(float minSpeedYz) {
        this.minSpeedYz = minSpeedYz;
    }

    @Override
    public float getMaxSpeedYz() {
        return maxSpeedYz;
    }

    protected void setMaxSpeedYz(float maxSpeedYz) {
        this.maxSpeedYz = maxSpeedYz;
    }

    @Override
    public float getMinScale() {
        return minScale;
    }

    protected void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    @Override
    public float getMaxScale() {
        return maxScale;
    }

    protected void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public EnumSet<EffectFlag> getFlags() {
        return flags;
    }

    protected void setFlags(EnumSet<EffectFlag> flags) {
        this.flags = flags;
    }

    public int getEffectId() {
        return effectId;
    }

    protected void setEffectId(int effectId) {
        this.effectId = effectId;
    }

    @Override
    public int getMinHp() {
        return minHp;
    }

    protected void setMinHp(int minHp) {
        this.minHp = minHp;
    }

    @Override
    public int getMaxHp() {
        return maxHp;
    }

    protected void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getFadeDuration() {
        return fadeDuration;
    }

    protected void setFadeDuration(int fadeDuration) {
        this.fadeDuration = fadeDuration;
    }

    public int getNextEffectId() {
        return nextEffectId;
    }

    protected void setNextEffectId(int nextEffectId) {
        this.nextEffectId = nextEffectId;
    }

    public int getDeathEffectId() {
        return deathEffectId;
    }

    protected void setDeathEffectId(int deathEffectId) {
        this.deathEffectId = deathEffectId;
    }

    public int getHitSolidEffectId() {
        return hitSolidEffectId;
    }

    protected void setHitSolidEffectId(int hitSolidEffectId) {
        this.hitSolidEffectId = hitSolidEffectId;
    }

    public int getHitWaterEffectId() {
        return hitWaterEffectId;
    }

    protected void setHitWaterEffectId(int hitWaterEffectId) {
        this.hitWaterEffectId = hitWaterEffectId;
    }

    public int getHitLavaEffectId() {
        return hitLavaEffectId;
    }

    protected void setHitLavaEffectId(int hitLavaEffectId) {
        this.hitLavaEffectId = hitLavaEffectId;
    }

    public List<Integer> getGenerateIds() {
        return generateIds;
    }

    protected void setGenerateIds(List<Integer> generateIds) {
        this.generateIds = generateIds;
    }

    public int getOuterOriginRange() {
        return outerOriginRange;
    }

    protected void setOuterOriginRange(int outerOriginRange) {
        this.outerOriginRange = outerOriginRange;
    }

    public int getLowerHeightLimit() {
        return lowerHeightLimit;
    }

    protected void setLowerHeightLimit(int lowerHeightLimit) {
        this.lowerHeightLimit = lowerHeightLimit;
    }

    public int getUpperHeightLimit() {
        return upperHeightLimit;
    }

    protected void setUpperHeightLimit(int upperHeightLimit) {
        this.upperHeightLimit = upperHeightLimit;
    }

    public int getOrientationRange() {
        return orientationRange;
    }

    protected void setOrientationRange(int orientationRange) {
        this.orientationRange = orientationRange;
    }

    public int getSpriteSpinRateRange() {
        return spriteSpinRateRange;
    }

    protected void setSpriteSpinRateRange(int spriteSpinRateRange) {
        this.spriteSpinRateRange = spriteSpinRateRange;
    }

    public int getWhirlpoolRate() {
        return whirlpoolRate;
    }

    protected void setWhirlpoolRate(int whirlpoolRate) {
        this.whirlpoolRate = whirlpoolRate;
    }

    public int getDirectionalSpread() {
        return directionalSpread;
    }

    protected void setDirectionalSpread(int directionalSpread) {
        this.directionalSpread = directionalSpread;
    }

    public int getCircularPathRate() {
        return circularPathRate;
    }

    protected void setCircularPathRate(int circularPathRate) {
        this.circularPathRate = circularPathRate;
    }

    public int getInnerOriginRange() {
        return innerOriginRange;
    }

    protected void setInnerOriginRange(int innerOriginRange) {
        this.innerOriginRange = innerOriginRange;
    }

    public int getGenerateRandomness() {
        return generateRandomness;
    }

    protected void setGenerateRandomness(int generateRandomness) {
        this.generateRandomness = generateRandomness;
    }

    public int getMisc2() {
        return misc2;
    }

    protected void setMisc2(int misc2) {
        this.misc2 = misc2;
    }

    public int getMisc3() {
        return misc3;
    }

    protected void setMisc3(int misc3) {
        this.misc3 = misc3;
    }

    public GenerationType getGenerationType() {
        return generationType;
    }

    protected void setGenerationType(GenerationType generationType) {
        this.generationType = generationType;
    }

    public short getElementsPerTurn() {
        return elementsPerTurn;
    }

    protected void setElementsPerTurn(short elementsPerTurn) {
        this.elementsPerTurn = elementsPerTurn;
    }

    public int getUnknown3() {
        return unknown3;
    }

    protected void setUnknown3(int unknown3) {
        this.unknown3 = unknown3;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(Effect o) {
        return Integer.compare(effectId, o.effectId);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.effectId;
        return hash;
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Effect other = (Effect) obj;

        return this.effectId == other.effectId;
    }
}
