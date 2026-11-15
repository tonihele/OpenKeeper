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
package toniarts.openkeeper.view.lighting;

import com.jme3.math.Vector3f;
import java.util.Random;

/**
 * A presentation-only point light source. Never saved, checksummed or read
 * by the simulation -- {@link LightRules} is the only
 * creator/destroyer of these.
 */
public final class LightSource {

    public enum FadeKind {
        IN, OUT, FLASH
    }

    /**
     * The kinds of light source the lighting system creates.
     * {@link #SLAB} is the per-terrain {@code Light} block;
     * no stock terrain uses it, it exists for custom terrain.
     */
    public enum Kind {
        THING,
        TORCH,
        TERRAIN,
        SLAB,
        PORTAL,
        CURSOR;

        /**
         * Whether lights of this kind belong to a map tile (and so follow that
         * tile's fog of war), as opposed to a thing, a room or the cursor.
         */
        public boolean isTileOwned() {
            return this == TORCH || this == TERRAIN || this == SLAB;
        }
    }

    /**
     * The groups of light sources that can be switched on and off independently
     * (see {@link LightRules#setGroupEnabled(Group, boolean)}), a coarser
     * view of {@link Kind} that also tells object lights from creature and
     * effect lights.
     */
    public enum Group {
        TORCH,
        TERRAIN,
        ROOM,
        CREATURE,
        OBJECT,
        CURSOR;

        public static Group of(LightSource light) {
            switch (light.getKind()) {
                case TORCH:
                    return TORCH;
                case TERRAIN:
                case SLAB:
                    return TERRAIN;
                case PORTAL:
                    return ROOM;
                case CURSOR:
                    return CURSOR;
                default:
                    return light.isObjectLight() ? OBJECT : CREATURE;
            }
        }
    }

    private final Kind kind;
    private final Object owner;
    private Vector3f position;
    private final int[] baseColour = new int[3];
    private final int[] colour = new int[3];
    private float intensity;
    private float radius;
    private boolean flickers;
    private boolean flickerCapable;
    private boolean objectLight;
    private boolean yieldsToTorches;
    private final Modulator modulator = new Modulator();
    private int lifetimeTicks = -1;
    private float flickerScale = 1.0f;
    private boolean enabled = true;

    public LightSource(Kind kind, Object owner, Vector3f position,
            int red, int green, int blue, float intensity, float radius) {
        this.kind = kind;
        this.owner = owner;
        this.position = position;
        this.baseColour[0] = red;
        this.baseColour[1] = green;
        this.baseColour[2] = blue;
        this.colour[0] = red;
        this.colour[1] = green;
        this.colour[2] = blue;
        this.intensity = intensity;
        // Terrain lights are exempt from the radius cap
        this.radius = kind == Kind.TERRAIN ? radius : Math.min(radius, LightConstants.MAX_LIGHT_RADIUS_TILES);
    }

    public Kind getKind() {
        return kind;
    }

    /**
     * Whether this light's falloff may not use the original's out-of-range
     * distance table and must stay monotonic instead.
     * Only the cursor light needs this: its 7-tile radius would otherwise
     * re-saturate into the dark-gap + second full-brightness band beyond 4
     * tiles. Mirrors the {@code smoothFalloff} selection in
     * {@code DkFalloff.glsllib}/{@code SceneLights}.
     *
     * @return {@code true} for the cursor light, {@code false} for every
     * other source
     */
    public boolean needsSmoothFalloff() {
        return kind == Kind.CURSOR;
    }

    public Object getOwner() {
        return owner;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    /**
     * Whether this light is carried by a game object (the dungeon heart glow,
     * ...), as opposed to a creature, effect, torch or terrain
     */
    public boolean isObjectLight() {
        return objectLight;
    }

    public void setObjectLight(boolean objectLight) {
        this.objectLight = objectLight;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * Whether torches overrule this light: the more strongly a torch lights a
     * point, the less this light adds there. For the glow of water and lava,
     * which would otherwise tint a torch's colour.
     */
    public boolean yieldsToTorches() {
        return yieldsToTorches;
    }

    public void setYieldsToTorches(boolean yieldsToTorches) {
        this.yieldsToTorches = yieldsToTorches;
    }

    /**
     * When {@code false}, this source contributes nothing regardless of
     * position/radius -- used by the cursor light to "hide" it
     * (build mode, off-map, not over the world) without needing
     * a sentinel position.
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Overwrites the base colour in place (and the current colour
     * immediately, since a source with no active modulator otherwise only
     * refreshes it on the next {@link #tick()}). Used by the cursor light,
     * whose colour depends on the tile under the mouse rather than any
     * modulator.
     */
    public void setBaseColour(int red, int green, int blue) {
        baseColour[0] = red;
        baseColour[1] = green;
        baseColour[2] = blue;
        if (modulator.getType() == Modulator.Type.NONE) {
            colour[0] = red;
            colour[1] = green;
            colour[2] = blue;
        }
    }

    public int getRed() {
        return colour[0];
    }

    public int getGreen() {
        return colour[1];
    }

    public int getBlue() {
        return colour[2];
    }

    public float getIntensity() {
        return intensity;
    }

    /**
     * Intensity scaled by the current flicker step, if this source
     * flickers.
     */
    public float getEffectiveIntensity() {
        return flickers ? intensity * flickerScale : intensity;
    }

    public float getRadius() {
        return radius;
    }

    public boolean isFlickering() {
        return flickers;
    }

    /**
     * Sets whether this source flickers right now and remembers it as
     * flicker-capable, so a later cheap-lighting toggle can re-evaluate it.
     */
    public void setFlickers(boolean flickers) {
        this.flickers = flickers;
        this.flickerCapable |= flickers;
    }

    /**
     * Marks the source as flicker-capable (torch or {@code FLICKER}-flagged
     * light) independent of the current cheap-lighting state.
     */
    public void setFlickerCapable(boolean flickerCapable) {
        this.flickerCapable = flickerCapable;
    }

    /**
     * Cheap lighting turns flicker off for every capable
     * source; turning it back off restores it.
     */
    public void applyCheapLighting(boolean cheap) {
        this.flickers = flickerCapable && !cheap;
    }

    public void setLifetime(int ticks) {
        this.lifetimeTicks = ticks;
    }

    public void startFade(FadeKind kind, int ticks) {
        switch (kind) {
            case IN -> modulator.fadeIn(ticks);
            case OUT -> modulator.fadeOut(ticks);
            case FLASH -> modulator.flash(ticks);
        }
    }

    public void startPulse() {
        modulator.pulse();
    }

    /**
     * Advances lifetime and the colour modulator by one simulation tick.
     *
     * @return {@code false} once this source's lifetime has expired and it
     * should be destroyed; {@code true} otherwise
     */
    public boolean tick() {
        if (lifetimeTicks > 0) {
            lifetimeTicks--;
            if (lifetimeTicks == 0) {
                return false;
            }
        }
        modulator.apply(baseColour, colour);
        return true;
    }

    /**
     * One random-walk step of the flicker clock, meant
     * to be called once per 1/{@link LightConstants#FLICKER_HZ} second of
     * presentation time by {@link LightModulators}. No-op if this source
     * doesn't flicker.
     */
    public void flickerStep(Random random) {
        if (!flickers) {
            return;
        }
        flickerScale += (random.nextFloat() * 2f - 1f) * LightConstants.FLICKER_STEP;
        flickerScale = Math.clamp(flickerScale, LightConstants.FLICKER_MIN, LightConstants.FLICKER_MAX);
    }
}
