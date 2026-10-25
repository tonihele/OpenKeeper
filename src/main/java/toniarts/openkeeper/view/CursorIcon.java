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
package toniarts.openkeeper.view;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.texture.Texture;
import com.jme3.ui.Picture;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState.Type;

/**
 * The small icon next to the hand while building or selling, as in the
 * original (DKII.EXE 0x40CAE0): placing a room, a door or a trap and selling
 * each have their icon from gui\cursors. While the action isn't possible
 * there, the no go sign is drawn over it and both move a bit to the left.
 * The plain hand, digging included, has no icon.
 * <p>
 * Next to the icon the original writes the price with its in game digits
 * (0x415650): the cost of building, light red if the keeper can afford it,
 * red if not, or the value of selling.
 */
public final class CursorIcon {

    public enum Icon {

        PLACE_ROOM("I-Place_Room"),
        PLACE_DOOR("I-Place_Door"),
        PLACE_TRAP("I-Place_Trap"),
        SELL("I-Sell");

        private final String texture;

        private Icon(String texture) {
            this.texture = texture;
        }
    }

    /**
     * How a price is shown, the colors are the original's
     */
    public enum PriceKind {

        AFFORDABLE(new ColorRGBA(1f, 0.5f, 0.5f, 1f)),
        TOO_EXPENSIVE(new ColorRGBA(1f, 0f, 0f, 1f)),
        SALE(new ColorRGBA(1f, 1f, 1f, 0.5f));

        private final ColorRGBA color;

        private PriceKind(ColorRGBA color) {
            this.color = color;
        }
    }

    /**
     * A price next to the icon
     *
     * @param amount the gold
     * @param kind how it is shown
     */
    public record Price(int amount, PriceKind kind) {

        /**
         * The cost of building
         *
         * @param cost the cost
         * @param gold the gold the keeper has
         * @return the price, {@code null} if there is nothing to pay
         */
        public static Price cost(long cost, int gold) {
            if (cost <= 0) {
                return null;
            }
            int amount = (int) Math.min(cost, Integer.MAX_VALUE);
            return new Price(amount, cost <= gold ? PriceKind.AFFORDABLE : PriceKind.TOO_EXPENSIVE);
        }

        /**
         * The value of selling
         *
         * @param value the gold the sale gives back
         * @return the price, {@code null} if there is nothing to sell
         */
        public static Price sale(int value) {
            return value > 0 ? new Price(value, PriceKind.SALE) : null;
        }
    }

    private static final String TEXTURES = "Textures/GUI/Cursors/";
    private static final String NO_GO = "I-No_Go";
    private static final int SIZE = 32;

    /**
     * From the click point of the hand to the top left corner of the icon, in
     * the original's 640x480 pixels. Our hand cursor isn't scaled, so neither
     * is the icon.
     */
    private static final float OFFSET_X = 82;
    private static final float OFFSET_Y = 10;

    /**
     * How far the icon moves to the left while the action isn't possible
     */
    private static final float NO_GO_SHIFT = 40;

    /**
     * The digits of the price: where they start from the icon's top left
     * corner, their size and the step from one to the next
     */
    private static final float PRICE_OFFSET_X = 30;
    private static final float PRICE_OFFSET_Y = 10;
    private static final int DIGIT_SIZE = 12;
    private static final int DIGIT_STEP = 10;
    private static final int MAX_DIGITS = 10;

    private final Node node = new Node("CursorIcon");
    private final Map<Icon, Picture> icons = new EnumMap<>(Icon.class);
    private final Picture noGo;
    private final Texture[] digitTextures = new Texture[10];
    private final Picture[] digits = new Picture[MAX_DIGITS];
    private Price shownPrice;

    public CursorIcon(AssetManager assetManager, Node guiNode) {
        for (Icon icon : Icon.values()) {
            Picture picture = createPicture(assetManager, TEXTURES + icon.texture + ".png", SIZE);
            icons.put(icon, picture);
            node.attachChild(picture);
        }

        // Drawn after the icon, on top of it
        noGo = createPicture(assetManager, TEXTURES + NO_GO + ".png", SIZE);
        node.attachChild(noGo);

        for (int i = 0; i < digitTextures.length; i++) {
            digitTextures[i] = assetManager.loadTexture("Textures/InGameDigit" + i + ".png");
        }
        for (int i = 0; i < digits.length; i++) {
            digits[i] = createPicture(assetManager, "Textures/InGameDigit0.png", DIGIT_SIZE);
            digits[i].setLocalTranslation(PRICE_OFFSET_X + i * DIGIT_STEP, SIZE - PRICE_OFFSET_Y - DIGIT_SIZE, 0);
            digits[i].setCullHint(CullHint.Always);
            node.attachChild(digits[i]);
        }

        node.setCullHint(CullHint.Always);
        guiNode.attachChild(node);
    }

    private static Picture createPicture(AssetManager assetManager, String texture, int size) {
        Picture picture = new Picture(texture);
        picture.setImage(assetManager, texture, true);
        picture.setWidth(size);
        picture.setHeight(size);
        return picture;
    }

    /**
     * The icon the original shows next to the hand for an interaction
     *
     * @param type the interaction
     * @return the icon, {@code null} for none
     */
    static Icon getIcon(Type type) {
        return switch (type) {
            case ROOM ->
                Icon.PLACE_ROOM;
            case DOOR ->
                Icon.PLACE_DOOR;
            case TRAP ->
                Icon.PLACE_TRAP;
            case SELL ->
                Icon.SELL;
            default ->
                null;
        };
    }

    /**
     * Shows the icon for the interaction next to the cursor, or hides it
     *
     * @param cursorPosition the cursor on the screen
     * @param type the interaction
     * @param visible whether the cursor is over the map
     * @param possible whether the action is possible there, the selection
     * isn't red
     * @param price the price to show next to the icon, {@code null} for none
     */
    public void update(Vector2f cursorPosition, Type type, boolean visible, boolean possible, Price price) {
        Icon shown = visible ? getIcon(type) : null;
        if (shown == null) {
            node.setCullHint(CullHint.Always);
            return;
        }

        for (Map.Entry<Icon, Picture> entry : icons.entrySet()) {
            entry.getValue().setCullHint(entry.getKey() == shown ? CullHint.Inherit : CullHint.Always);
        }
        noGo.setCullHint(possible ? CullHint.Always : CullHint.Inherit);
        if (!Objects.equals(price, shownPrice)) {
            showPrice(price);
        }

        // The screen's y goes up here, the original's down
        float x = cursorPosition.x + OFFSET_X - (possible ? 0 : NO_GO_SHIFT);
        float y = cursorPosition.y - OFFSET_Y - SIZE;
        node.setLocalTranslation(x, y, 0);
        node.setCullHint(CullHint.Inherit);
    }

    /**
     * Sets the digits of the price, only when it changes
     */
    private void showPrice(Price price) {
        shownPrice = price;
        String text = price != null ? Integer.toString(price.amount()) : "";
        for (int i = 0; i < digits.length; i++) {
            if (i >= text.length()) {
                digits[i].setCullHint(CullHint.Always);
                continue;
            }
            Material material = digits[i].getMaterial();
            material.setTexture("Texture", digitTextures[text.charAt(i) - '0']);
            material.setColor("Color", price.kind().color);
            digits[i].setCullHint(CullHint.Inherit);
        }
    }

    public void cleanup() {
        node.removeFromParent();
    }

}
