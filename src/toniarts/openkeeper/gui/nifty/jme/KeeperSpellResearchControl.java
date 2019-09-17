/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.gui.nifty.jme;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetKey;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.ui.Picture;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Draws a kinda progress effect image on top of a Nifty element
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KeeperSpellResearchControl extends AbstractNiftyJmeControl {

    public static final String CONTROL_NAME = "keeperSpellResearch";
    private static final Logger LOGGER = Logger.getLogger(KeeperSpellResearchControl.class.getName());

    private Picture picture;
    private boolean initialized = false;
    private float research = 0;
    private Color color;
    private String image;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        super.bind(nifty, screen, element, parameter);

        String colorString = parameter.get("color");
        if (colorString != null && !"".equals(colorString)) {
            color = new Color(Integer.parseInt(colorString), true);
        }
        image = parameter.getWithDefault("image", "");
        if ("".equals(image)) {
            image = null;
        }

        // Sanity check
        if (color == null && image == null) {
            throw new IllegalArgumentException("Need to specify either color or image!");
        }
    }


    @Override
    public void initJme(SimpleApplication app) {
        super.initJme(app);

        picture = new Picture("SpellResearchIndicator");
        picture.setHeight(getControlHeight());
        picture.setWidth(getControlWidth());
        picture.setPosition(getControlX(), getControlY());
        picture.addControl(new SimpleJmeControl(this));
    }

    @Override
    protected void cleanup() {
        if (initialized) {
            getApp().getGuiNode().detachChild(picture);
        }
    }

    public void setResearch(float research) {
        this.research = research;

        if (!initialized && research > 0) {
            initialize();
        } else {

            // TODO: should maybe limit the drawing when visible and only on some occasions etc.
            drawTexture();
        }
    }

    private void initialize() {
        initialized = true;

        // We need to have a texture before we attach the image
        drawTexture();

        // Attach to scene
        getApp().getGuiNode().attachChild(picture);
    }

    private void drawTexture() {
        BufferedImage img = null;
        if (image != null) {
            try {
                img = ImageIO.read(getApp().getAssetManager().locateAsset(new AssetKey(image)).openStream());
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Failed to load the backdrop image with " + image + "!", ex);
            }
        }
        BufferedImage newImage = new BufferedImage(getControlWidth(), getControlHeight(), img != null ? img.getType() : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.clip(getProgressShape(newImage.getWidth(), newImage.getHeight(), research));
        if (img != null) {

            // Draw the wanted image to the new image
            g.drawImage(img, 0, 0, newImage.getWidth(), newImage.getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
        } else {
            g.setPaint(color);
            g.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());
        }
        g.dispose();

        // Assign the texture
        AWTLoader loader = new AWTLoader();
        Texture2D tex = new Texture2D(loader.load(newImage, false));
        picture.setTexture(getApp().getAssetManager(), tex, true);
    }

    private static Shape getProgressShape(int width, int height, float research) {
        float angle = research * 360;
        return new Arc2D.Float(-0.5f * width, -0.5f * height, 2 * width, 2 * height, -90, angle, Arc2D.PIE);
    }

}
