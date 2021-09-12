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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
public class FontCreator {

    private static final int MAX_SIZE = 2048;

    private final String description;
    private final List<FontImage> fontImages;

    // TODO: Perfect candidate for being a record :)
    public class FontImage {

        private final String fileName;
        private final BufferedImage fontImage;
        private final int page;

        public FontImage(String fileName, BufferedImage fontImage, int page) {
            this.fileName = fileName;
            this.fontImage = fontImage;
            this.page = page;
        }

        public String getFileName() {
            return fileName;
        }

        public BufferedImage getFontImage() {
            return fontImage;
        }

        public int getPage() {
            return page;
        }

        @Override
        public String toString() {
            return "FontImage{" + "fileName=" + fileName + ", page=" + page + '}';
        }

    }

    public FontCreator(Bf4File fontFile, int fontSize, String fileName) {
        fontImages = new ArrayList<>();
        description = createFonts(fileName, fontSize, fontFile);
    }

    private String createFonts(String fileName, int fontSize, Bf4File fontFile) {
        int size = getFontImageSize(fontFile);

        // Draw the fonts and write the descriptions
        String characterDescriptions = createCharacters(fontFile, size, fileName);

        // Write the header description
        String header = createHeader(fileName, fontSize, fontFile, size);

        return header + characterDescriptions;
    }

    private String createHeader(String fileName, int fontSize, Bf4File fontFile, int size) {
        StringBuilder sb = new StringBuilder(1000);
        sb.append("info face=\"");
        sb.append(fileName.substring(0, fileName.length() - 4)); // For current Nifty batching the face needs to be unique
        sb.append("\" ");
        sb.append("size=");
        sb.append(fontSize);
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
        sb.append(size);
        sb.append(" ");
        sb.append("scaleH=");
        sb.append(size);
        sb.append(" ");
        sb.append("pages=");
        sb.append(fontImages.size());
        sb.append(" ");
        sb.append("packed=0 ");
        sb.append("\n");

        // Add the pages
        for (FontImage fi : fontImages) {
            sb.append("page id=");
            sb.append(fi.page);
            sb.append(" file=\"");
            sb.append(fi.fileName);
            sb.append("\"\n");
        }

        sb.append("chars count=");
        sb.append(fontFile.getMaxCodePoint());
        sb.append("\n");

        return sb.toString();
    }

    private String createCharacters(Bf4File fontFile, int size, String fileName) {
        BufferedImage fontImage = getFontImage(size);
        StringBuilder sb = new StringBuilder(1000);
        Graphics2D g = (Graphics2D) fontImage.getGraphics();
        int x = 0;
        int y = 0;
        int page = 0;

        Set<Integer> insertedChars = new HashSet<>(fontFile.getCount());
        for (Bf4Entry entry : fontFile) {
            if (!insertedChars.contains((int) entry.getCharacter())) {
                insertedChars.add((int) entry.getCharacter());
                if (entry.getImage() != null) {

                    // See if we still fit & draw
                    if (x + entry.getWidth() > fontImage.getWidth()) {
                        x = 0;
                        y += fontFile.getMaxHeight();
                    }
                    if (y + fontFile.getMaxHeight() > fontImage.getHeight()) {

                        // New page entirely
                        g.dispose();
                        fontImages.add(createFontImage(fileName, page, fontImage));
                        fontImage = getFontImage(size);
                        g = (Graphics2D) fontImage.getGraphics();

                        x = 0;
                        y = 0;
                        page++;
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
                sb.append("    page=");
                sb.append(page);
                sb.append("    chnl=0\n");

                if (entry.getImage() != null) {

                    // Update the x pos
                    x += entry.getWidth();
                }
            }
        }
        g.dispose();
        fontImages.add(createFontImage(fileName, page, fontImage));

        return sb.toString();
    }

    private static BufferedImage getFontImage(int size) {
        return new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY, Bf4File.getCm());
    }

    /**
     * Return the optimal(ish) size of the font image. The font image should be
     * a square power of two value. The same size is used in all of the pages.
     *
     * @param fontFile the font file, for approximating the size
     * @return size of the image
     */
    private static int getFontImageSize(Bf4File fontFile) {

        // Calculate the totals
        int area = fontFile.getMaxHeight() * fontFile.getAvgWidth() * fontFile.getGlyphCount(); // This is how many square pixels we need, approximate
        int side = (int) FastMath.ceil(FastMath.sqrt(area));

        // The next power of two
        side = Integer.highestOneBit(side - 1) * 2;
        side = Math.min(MAX_SIZE, side);

        return side;
    }

    private FontImage createFontImage(String fileName, int page, BufferedImage fontImage) {
        return new FontImage(fileName.substring(0, fileName.length() - 4) + "_" + page + fileName.substring(fileName.length() - 4), fontImage, page);
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
    public List<FontImage> getFontImages() {
        return fontImages;
    }
}
