/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.map;

/**
 * Container class for Effects.kwd
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Effect {
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
    private int flags; // b4
    private int effectId; // b8
    private int minHp; // ba number of particles emitted in sequence
    private int maxHp; // bc
    private int fadeDuration; // be
// might be a repeatable block
    private int nextEffect; // c0
    private int deathEffect; // c2
    private int hitSolidEffect; // c4
    private int hitWaterEffect; // c6
    private int hitLavaEffect; // c8
    private int generateIds[]; // ca elements or effects, depending on flags
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
    private short unknown1; // f2
    private short elementsPerTurn; // f3
    private int unknown3; // f4 pad?

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

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

    public float getMass() {
        return mass;
    }

    protected void setMass(float mass) {
        this.mass = mass;
    }

    public float getAirFriction() {
        return airFriction;
    }

    protected void setAirFriction(float airFriction) {
        this.airFriction = airFriction;
    }

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

    public float getMinSpeedXy() {
        return minSpeedXy;
    }

    protected void setMinSpeedXy(float minSpeedXy) {
        this.minSpeedXy = minSpeedXy;
    }

    public float getMaxSpeedXy() {
        return maxSpeedXy;
    }

    protected void setMaxSpeedXy(float maxSpeedXy) {
        this.maxSpeedXy = maxSpeedXy;
    }

    public float getMinSpeedYz() {
        return minSpeedYz;
    }

    protected void setMinSpeedYz(float minSpeedYz) {
        this.minSpeedYz = minSpeedYz;
    }

    public float getMaxSpeedYz() {
        return maxSpeedYz;
    }

    protected void setMaxSpeedYz(float maxSpeedYz) {
        this.maxSpeedYz = maxSpeedYz;
    }

    public float getMinScale() {
        return minScale;
    }

    protected void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    public float getMaxScale() {
        return maxScale;
    }

    protected void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public int getFlags() {
        return flags;
    }

    protected void setFlags(int flags) {
        this.flags = flags;
    }

    public int getEffectId() {
        return effectId;
    }

    protected void setEffectId(int effectId) {
        this.effectId = effectId;
    }

    public int getMinHp() {
        return minHp;
    }

    protected void setMinHp(int minHp) {
        this.minHp = minHp;
    }

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

    public int getNextEffect() {
        return nextEffect;
    }

    protected void setNextEffect(int nextEffect) {
        this.nextEffect = nextEffect;
    }

    public int getDeathEffect() {
        return deathEffect;
    }

    protected void setDeathEffect(int deathEffect) {
        this.deathEffect = deathEffect;
    }

    public int getHitSolidEffect() {
        return hitSolidEffect;
    }

    protected void setHitSolidEffect(int hitSolidEffect) {
        this.hitSolidEffect = hitSolidEffect;
    }

    public int getHitWaterEffect() {
        return hitWaterEffect;
    }

    protected void setHitWaterEffect(int hitWaterEffect) {
        this.hitWaterEffect = hitWaterEffect;
    }

    public int getHitLavaEffect() {
        return hitLavaEffect;
    }

    protected void setHitLavaEffect(int hitLavaEffect) {
        this.hitLavaEffect = hitLavaEffect;
    }

    public int[] getGenerateIds() {
        return generateIds;
    }

    protected void setGenerateIds(int[] generateIds) {
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

    public short getUnknown1() {
        return unknown1;
    }

    protected void setUnknown1(short unknown1) {
        this.unknown1 = unknown1;
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
}
