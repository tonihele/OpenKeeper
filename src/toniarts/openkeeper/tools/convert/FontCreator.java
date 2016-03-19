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
package toniarts.openkeeper.tools.convert;

import com.jme3.math.FastMath;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.tools.convert.bf4.Bf4Entry;
import toniarts.openkeeper.tools.convert.bf4.Bf4File;

/**
 * Creates AngelFonts out of DK II fonts. Not 100% to the specs, but enough that
 * JME will take it.<br>
 * AngelFont specs:
 * http://www.angelcode.com/products/bmfont/doc/file_format.html<br>
 * JME FontCreator (by Normen Hansen) used as reference
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class FontCreator {

    private final String description;
    private final BufferedImage fontImage;
    private final Set<Integer> insertedChars;

    public FontCreator(Bf4File fontFile) {

        // Create right sized font image
        fontImage = getFontImage(fontFile);

        // Write the header description
        StringBuilder sb = new StringBuilder(1000);
        sb.append("info face=\"");
        sb.append(getFileName().substring(0, getFileName().length() - 4)); // For current Nifty batching the face needs to be unique
        sb.append("\" ");
        sb.append("size=");
        sb.append(getFontSize());
        sb.append(" ");
        sb.append("bold=0 ");
        sb.append("italic=0 ");
        sb.append("unicode=1 ");
        sb.append("stretchH=100 ");
        sb.append("smooth=0 ");
        sb.append("aa=0 ");
        sb.append("padding=0,0,0,0 ");
        sb.append("spacing=1,1 ");
        sb.append("\n");
        sb.append("common lineHeight=");
        sb.append(fontFile.getMaxHeight());
        sb.append(" ");
        sb.append("base=26 ");
        sb.append("scaleW=");
        sb.append(fontImage.getWidth());
        sb.append(" ");
        sb.append("scaleH=");
        sb.append(fontImage.getHeight());
        sb.append(" ");
        sb.append("pages=1 ");
        sb.append("packed=0 ");
        sb.append("\n");
        sb.append("page id=0 file=\"");
        sb.append(getFileName());
        sb.append("\"\n");
        sb.append("chars count=");
        sb.append(fontFile.getMaxCodePoint());
        sb.append("\n");

        // Draw the fonts and write the descriptions
        Graphics2D g = (Graphics2D) fontImage.getGraphics();
        int x = 0;
        int y = 0;
        insertedChars = new HashSet<>(fontFile.getCount());
        for (Bf4Entry entry : fontFile) {
            if (!insertedChars.contains((int) entry.getCharacter())) {
                insertedChars.add((int) entry.getCharacter());
                if (entry.getImage() != null) {

                    // See if we still fit & draw
                    if (x + entry.getWidth() > fontImage.getWidth()) {
                        x = 0;
                        y += fontFile.getMaxHeight();
                    }
                    g.drawImage(entry.getImage(), x, y, null);
                }

                // Update description
                sb.append("char id=");
                sb.append(Integer.toString(entry.getCharacter()));
                sb.append("    x=");
                sb.append(x);
                sb.append("    y=");
                sb.append(y);
                sb.append("    width=");
                sb.append(entry.getWidth());
                sb.append("    height=");
                sb.append(entry.getHeight());
                sb.append("    xoffset=");
                sb.append(entry.getOffsetX());
                sb.append("    yoffset=");
                sb.append(entry.getOffsetY());
                sb.append("    xadvance=");
                sb.append(entry.getOuterWidth());
                sb.append("    page=0");
                sb.append("    chnl=0\n");

                if (entry.getImage() != null) {

                    // Update the x pos
                    x += entry.getWidth();
                }
            }
        }
        g.dispose();

        // Set the description
        description = sb.toString();
    }

    private BufferedImage getFontImage(Bf4File fontFile) {

        // Try to create a nice square (power of two is really waste of space...)
        int area = fontFile.getMaxHeight() * fontFile.getAvgWidth() * fontFile.getGlyphCount(); // This is how many square pixels we need, approximate
        int side = (int) FastMath.ceil(FastMath.sqrt(area)) + 10; // The plus is just a bit padding to make sure everything fits

        // Divisible by two
        if (side % 2 != 0) {
            side++;
        }

        // Create the image
        return new BufferedImage(side, side, BufferedImage.TYPE_BYTE_BINARY, Bf4File.getCm());
    }

    /**
     * This is the textual description of the font image
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * The actual image with the fonts
     *
     * @return font image
     */
    public BufferedImage getFontImage() {
        return fontImage;
    }

    /**
     * The font size in points
     *
     * @return font size
     */
    protected abstract int getFontSize();

    /**
     * The image file name where the fonts are
     *
     * @return font image file name
     */
    protected abstract String getFileName();
}
