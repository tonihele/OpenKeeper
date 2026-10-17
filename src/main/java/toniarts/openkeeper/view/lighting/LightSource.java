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
 * by the simulation (design goal 3) -- {@link LightRules} is the only
 * creator/destroyer of these. See {@code lighting_design.md} &sect;5.
 *
 * @author Toni Helenius &lt;helenius.toni@gmail.com&gt;
 */
public final class LightSource {

    public enum FadeKind {
        IN, OUT, FLASH
    }

    private final LightKind kind;
    private final Object owner;
    private final boolean isStatic;
    private Vector3f position;
    private final int[] baseColour = new int[3];
    private final int[] colour = new int[3];
    private float intensity;
    private float radius;
    private boolean flickers;
    private boolean flickerCapable;
    private boolean objectLight;
    private final Modulator modulator = new Modulator();
    private int lifetimeTicks = -1;
    private float flickerScale = 1.0f;
    private boolean enabled = true;

    public LightSource(LightKind kind, Object owner, boolean isStatic, Vector3f position,
            int red, int green, int blue, float intensity, float radius) {
        this.kind = kind;
        this.owner = owner;
        this.isStatic = isStatic;
        this.position = position;
        this.baseColour[0] = red;
        this.baseColour[1] = green;
        this.baseColour[2] = blue;
        this.colour[0] = red;
        this.colour[1] = green;
        this.colour[2] = blue;
        this.intensity = intensity;
        this.radius = Math.min(radius, LightConstants.MAX_LIGHT_RADIUS_TILES);
    }

    public LightKind getKind() {
        return kind;
    }

    /**
     * Whether this light's falloff may not use the original's out-of-range
     * distance table (design decision D7) and must stay monotonic instead.
     * Only the cursor light needs this: its 7-tile radius would otherwise
     * re-saturate into the dark-gap + second full-brightness band beyond 4
     * tiles. Mirrors the {@code smoothFalloff} selection in
     * {@code DkFalloff.glsllib}/{@code SceneLights}.
     *
     * @return {@code true} for the cursor light, {@code false} for every
     * other source
     */
    public boolean needsSmoothFalloff() {
        return kind == LightKind.CURSOR;
    }

    public Object getOwner() {
        return owner;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    /**
     * When {@code false}, this source contributes nothing regardless of
     * position/radius -- used by the cursor light to "hide" per design
     * &sect;7.3 (build mode, off-map, not over the world) without needing
     * a sentinel position.
     */
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
     * modulator (design &sect;7.3).
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

    public int getBaseRed() {
        return baseColour[0];
    }

    public int getBaseGreen() {
        return baseColour[1];
    }

    public int getBaseBlue() {
        return baseColour[2];
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
     * flickers (design &sect;7.2).
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
     * Design &sect;10: cheap lighting turns flicker off for every capable
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
     * Advances lifetime and the colour modulator by one simulation tick
     * (design &sect;7.1).
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
     * One random-walk step of the flicker clock (design &sect;7.2), meant
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
