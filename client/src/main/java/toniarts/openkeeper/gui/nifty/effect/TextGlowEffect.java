/*
 * Copyright (C) 2014-2024 OpenKeeper
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
package toniarts.openkeeper.gui.nifty.effect;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.effects.EffectImpl;
import de.lessvoid.nifty.effects.EffectProperties;
import de.lessvoid.nifty.effects.Falloff;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.layout.align.HorizontalAlign;
import de.lessvoid.nifty.layout.align.VerticalAlign;
import de.lessvoid.nifty.render.NiftyRenderEngine;
import de.lessvoid.nifty.spi.render.RenderFont;
import de.lessvoid.nifty.tools.Color;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Renders a subtle glow around the element's text. The glow approximates a
 * Gaussian blur by drawing the text multiple times at all offsets within a
 * small radius, with the alpha falling off like a normal distribution as the
 * offset grows. This produces a soft, blurred halo behind the actual text. Use
 * in conjunction with {@code onActive}, {@code timeType="infinite"} and
 * {@code neverStopRendering="true"} to render the effect on every frame while
 * the element's screen is active.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TextGlowEffect implements EffectImpl {

    /**
     * The maximum blur radius supported by the precomputed offset rings.
     */
    private static final int MAX_DISTANCE = 8;

    /**
     * For every ring radius {@code r}, all integer offsets whose Chebyshev
     * distance to the origin is exactly {@code r}. Together the rings cover
     * every point around the text exactly once.
     */
    private static final int[][][] RING_OFFSETS = buildRingOffsets();

    /**
     * Precomputes the ring offsets for all radii up to {@link #MAX_DISTANCE}.
     *
     * @return the ring offsets
     */
    private static int[][][] buildRingOffsets() {
        int[][][] rings = new int[MAX_DISTANCE + 1][][];
        for (int radius = 1; radius <= MAX_DISTANCE; radius++) {
            int[][] offsets = new int[8 * radius][];
            int index = 0;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) == radius) {
                        offsets[index++] = new int[] { dx, dy };
                    }
                }
            }
            rings[radius] = offsets;
        }
        return rings;
    }

    /**
     * The glow color. When {@code null}, the element's own text color is used.
     */
    @Nullable
    private Color glowColor;

    /**
     * The glow opacity.
     */
    private float alpha = 0.7f;

    /**
     * The glow spread in pixels.
     */
    private int distance = 1;

    /**
     * initialize.
     *
     * @param nifty Nifty
     * @param element Element
     * @param parameter Parameter
     */
    @Override
    public void activate(
            @Nonnull final Nifty nifty,
            @Nonnull final Element element,
            @Nonnull final EffectProperties parameter) {
        String color = parameter.getProperty("color");
        glowColor = color != null ? new Color(color) : null;
        alpha = Float.parseFloat(parameter.getProperty("alpha", "0.7"));
        distance = Integer.parseInt(parameter.getProperty("distance", "1"));
    }

    /**
     * execute the effect.
     *
     * @param element the Element
     * @param normalizedTime TimeInterpolator to use
     * @param falloff falloff value
     * @param r RenderDevice to use
     */
    @Override
    public void execute(
            @Nonnull final Element element,
            final float normalizedTime,
            @Nullable final Falloff falloff,
            @Nonnull final NiftyRenderEngine r) {
        TextRenderer textRenderer = element.getRenderer(TextRenderer.class);
        if (textRenderer == null) {
            return;
        }
        RenderFont font = textRenderer.getFont();
        if (font == null) {
            font = r.getFont();
        }
        if (font == null) {
            return;
        }
        String[] lines = textRenderer.getWrappedText().split("\n", -1);
        if (lines.length == 0 || (lines.length == 1 && lines[0].isEmpty())) {
            return;
        }
        Color baseColor = glowColor != null ? glowColor : textRenderer.getColor();
        if (baseColor == null) {
            return;
        }

        int glowDistance = Math.min(Math.max(distance, 1), MAX_DISTANCE);
        // a Gaussian with sigma = half the radius so that the glow reaches zero
        // towards the outermost ring
        float sigmaSquared = 2.0f * (glowDistance / 2.0f) * (glowDistance / 2.0f);

        r.saveStates();
        r.setFont(font);

        int startY = verticalTextOffset(lines.length * font.getHeight(), element.getHeight(),
                textRenderer.getTextVAlign());
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineX = element.getX()
                    + horizontalTextOffset(font.getWidth(line), element.getWidth(), textRenderer.getTextHAlign());
            int lineY = element.getY() + startY + i * font.getHeight();
            // draw the outer rings first so that the inner, brighter rings overlay them
            for (int ring = glowDistance; ring >= 1; ring--) {
                float weight = (float) Math.exp(-(ring * ring) / sigmaSquared);
                if (weight < 0.01f) {
                    continue;
                }
                Color ringColor = new Color(baseColor, alpha * weight);
                r.setColor(ringColor);
                for (int[] offset : RING_OFFSETS[ring]) {
                    r.renderText(line, lineX + offset[0], lineY + offset[1], -1, -1, ringColor);
                }
            }
        }

        r.restoreStates();
    }

    /**
     * deactivate the effect.
     */
    @Override
    public void deactivate() {
    }

    /**
     * Calculates the horizontal offset for the given alignment, the same way as
     * {@link TextRenderer} does when rendering the actual text.
     *
     * @param textWidth the width of the text
     * @param elementWidth the width of the element
     * @param align the horizontal alignment
     * @return the horizontal offset
     */
    private static int horizontalTextOffset(final int textWidth, final int elementWidth,
            final HorizontalAlign align) {
        if (align == HorizontalAlign.center) {
            return (elementWidth - textWidth) / 2;
        } else if (align == HorizontalAlign.right) {
            return elementWidth - textWidth;
        }
        return 0;
    }

    /**
     * Calculates the vertical offset for the given alignment, the same way as
     * {@link TextRenderer} does when rendering the actual text.
     *
     * @param textHeight the height of the text
     * @param elementHeight the height of the element
     * @param align the vertical alignment
     * @return the vertical offset
     */
    private static int verticalTextOffset(final int textHeight, final int elementHeight,
            final VerticalAlign align) {
        if (align == VerticalAlign.center) {
            return (elementHeight - textHeight) / 2;
        } else if (align == VerticalAlign.bottom) {
            return elementHeight - textHeight;
        }
        return 0;
    }

}