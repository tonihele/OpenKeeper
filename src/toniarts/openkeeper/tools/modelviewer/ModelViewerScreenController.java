/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.tools.modelviewer;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.TextureKey;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.controls.DropDownSelectionChangedEvent;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.ListBoxSelectionChangedEvent;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.nifty.spi.render.RenderFont;
import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.sound.SoundFile;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.Utils;

/**
 *
 * @author archdemon
 */
public class ModelViewerScreenController implements ScreenController {

    public final static String ID_SCREEN = "start";

    private final static String ID_ITEMS = "items";
    private final static String ID_TYPES = "types";
    private final static String ID_ITEM = "item";
    private final static String ID_SOUNDS = "sounds";

    private final ModelViewer app;
    private final Nifty nifty;
    private Screen screen;

    public ModelViewerScreenController(ModelViewer app) {
        this.app = app;

        nifty = app.getNifty();
        screen = nifty.getCurrentScreen();

        // Set default font
        RenderFont font = nifty.createFont("Interface/Fonts/Frontend14.fnt");
        nifty.getRenderEngine().setFont(font);
        nifty.registerMouseCursor("pointer", "Interface/Cursors/Idle.png", 4, 4);

        // Set some Nifty stuff
        nifty.addResourceBundle("menu", Main.getResourceBundle("Interface/Texts/Text"));
        nifty.addResourceBundle("speech", Main.getResourceBundle("Interface/Texts/Speech"));
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        this.screen = screen;
        // Fill types
        app.fillTypes();
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onEndScreen() {
    }

    @NiftyEventSubscriber(id = ID_ITEMS)
    public void onListBoxSelectionChanged(final String id, final ListBoxSelectionChangedEvent<Object> event) {

        List<Object> selection = event.getSelection();

        if (selection.size() == 1) {
            app.onSelectionChanged(selection.get(0));
        }
    }

    @NiftyEventSubscriber(id = ID_TYPES)
    public void onTypeChanged(final String id, final DropDownSelectionChangedEvent<ModelViewer.Types> event) {
        app.fillList(event.getSelection());
    }

    @NiftyEventSubscriber(id = ID_SOUNDS)
    public void onSoundSelectionChanged(final String id, final DropDownSelectionChangedEvent<SoundFile> event) {
        app.onSoundChanged(event.getSelection());
    }

    /**
     * Get the listbox holding the models
     *
     * @return the listbox
     */
    protected ListBox getItemsControl() {
        ListBox<Object> listBox = (ListBox<Object>) screen.findNiftyControl(ID_ITEMS, ListBox.class);

        return listBox;
    }

    /**
     * Get the dropdown selection for type
     *
     * @return the dropdown
     */
    protected DropDown<ModelViewer.Types> getTypeControl() {
        DropDown<ModelViewer.Types> dropDown = (DropDown<ModelViewer.Types>) screen.findNiftyControl(ID_TYPES, DropDown.class);

        return dropDown;
    }

    void goToScreen(String screen) {
        nifty.gotoScreen(screen);
    }

    protected void setupItem(Object item, List<SoundFile> audio) {
        Element panel = screen.findElementById(ID_ITEM);
        for (Element element : panel.getChildren()) {
            element.markForRemoval();
        }

        Element el = null;
        if (item instanceof Creature) {
            ControlBuilder cb = createCreatureControl((Creature) item);
            el = cb.build(panel);
        } else if (item instanceof Room) {
            ControlBuilder cb = createRoomControl((Room) item);
            el = cb.build(panel);
        } else if (item instanceof Terrain) {
            ControlBuilder cb = createTerrainControl((Terrain) item);
            el = cb.build(panel);
        } else if (item instanceof Shot) {
            ControlBuilder cb = createShotControl((Shot) item);
            el = cb.build(panel);
        } else if (item instanceof GameLevel) {
            ControlBuilder cb = createGameLevelControl((GameLevel) item);
            el = cb.build(panel);
        } else if (item instanceof GameObject) {
            ControlBuilder cb = createObjectControl((GameObject) item);
            el = cb.build(panel);
        } else if (item instanceof Trap) {
            ControlBuilder cb = createTrapControl((Trap) item);
            el = cb.build(panel);
        } else if (item instanceof Door) {
            ControlBuilder cb = createDoorControl((Door) item);
            el = cb.build(panel);
        } else if (item instanceof Effect) {
            ControlBuilder cb = createEffectControl((Effect) item);
            el = cb.build(panel);
        }

        if (el != null) {
            DropDown<SoundFile> dropDown = (DropDown<SoundFile>) el.findNiftyControl(ID_SOUNDS, DropDown.class);
            if (dropDown != null) {
                dropDown.clear();
                dropDown.addAllItems(audio);
            }
        }
        //panel.addChild(el);
        panel.layoutElements();
        panel.reactivate();
    }

    private ControlBuilder createCreatureControl(final Creature item) {
        String name = item.getName();

        ControlBuilder result = new ControlBuilder(name, "creature") {
            {
                parameter("title", name);

                parameter("iconOne", getResourceImageName(item.getIcon1Resource()));
                parameter("iconTwo", getResourceImageName(item.getIcon2Resource()));
                parameter("portrait", getResourceImageName(item.getPortraitResource()));
                parameter("unique", getResourceImageName(item.getUniqueResource()));
                parameter("fpMelee", getResourceImageName(item.getFirstPersonMeleeResource()));
                parameter("fpFilter", getResourceImageName(item.getFirstPersonFilterResource()));

                parameter("namestring", getResourceString(item.getNameStringId()));
                parameter("description", getResourceString(item.getGeneralDescriptionStringId()));
                parameter("tooltip", getResourceString(item.getTooltipStringId()));

                parameter("introduction", getResourceString(item.getIntroductionStringId()));
                parameter("strength", getResourceString(item.getStrengthStringId()));
                parameter("uniqueName", getResourceString(item.getUniqueNameTextId()));
                parameter("weakness", getResourceString(item.getWeaknessStringId()));

                parameter("angerFood", getResourceString(item.getAngerStringIdFood()));
                parameter("angerGeneral", getResourceString(item.getAngerStringIdGeneral()));
                parameter("angerHatred", getResourceString(item.getAngerStringIdHatred()));
                parameter("angerHeld", getResourceString(item.getAngerStringIdHeld()));
                parameter("angerLair", getResourceString(item.getAngerStringIdLair()));
                parameter("angerLonely", getResourceString(item.getAngerStringIdLonely()));
                parameter("angerPay", getResourceString(item.getAngerStringIdPay()));
                parameter("angerSlap", getResourceString(item.getAngerStringIdSlap()));
                parameter("angerTorture", getResourceString(item.getAngerStringIdTorture()));
                parameter("angerWork", getResourceString(item.getAngerStringIdWork()));
            }
        };

        return result;
    }

    private ControlBuilder createRoomControl(final Room item) {
        String name = item.getName();

        ControlBuilder result = new ControlBuilder(name, "room") {
            {
                parameter("title", name);
                parameter("iconGui", getResourceImageName(item.getGuiIcon()));

                parameter("namestring", getResourceString(item.getNameStringId()));
                parameter("genDesk", getResourceString(item.getGeneralDescriptionStringId()));
                parameter("strength", getResourceString(item.getStrengthStringId()));
                parameter("tooltip", getResourceString(item.getTooltipStringId()));
                parameter("construction", item.getTileConstruction().toString());

                parameter("ceilingHeight", String.valueOf(item.getCeilingHeight()));
                parameter("cost", String.valueOf(item.getCost()));
                parameter("createdCreatureId", String.valueOf(item.getCreatedCreatureId()));
                parameter("healthGain", String.valueOf(item.getHealthGain()));
                parameter("size", item.getRecommendedSizeX() + " x " + item.getRecommendedSizeY());
                parameter("researchTime", String.valueOf(item.getResearchTime()));
                parameter("returnPercentage", String.valueOf(item.getReturnPercentage()));
            }
        };

        return result;
    }

    private ControlBuilder createTerrainControl(final Terrain item) {
        String name = item.getName();

        ControlBuilder result = new ControlBuilder(name, "terrain") {
            {
                parameter("title", name);

                parameter("namestring", getResourceString(item.getNameStringId()));
                parameter("genDesk", getResourceString(item.getGeneralDescriptionStringId()));
                parameter("strength", getResourceString(item.getStrengthStringId()));
                parameter("tooltip", getResourceString(item.getTooltipStringId()));

                parameter("damage", String.valueOf(item.getDamage()));
                parameter("depth", String.valueOf(item.getDepth()));
                parameter("goldValue", String.valueOf(item.getGoldValue()));
                parameter("lightHeight", String.valueOf(item.getLightHeight()));
                parameter("manaGain", String.valueOf(item.getManaGain()));
                parameter("maxHealth", String.valueOf(item.getMaxHealth()));
                parameter("maxManaGain", String.valueOf(item.getMaxManaGain()));
                parameter("startingHealth", String.valueOf(item.getStartingHealth()));
                parameter("textureFrames", String.valueOf(item.getTextureFrames()));
            }
        };

        return result;
    }

    private ControlBuilder createShotControl(final Shot item) {
        String name = item.getName();

        ControlBuilder result = new ControlBuilder(name, "shot") {
            {
                parameter("title", name);

                parameter("airFriction", String.valueOf(item.getAirFriction()));
                parameter("radius", String.valueOf(item.getRadius()));
                parameter("speed", String.valueOf(item.getSpeed()));
                parameter("threat", String.valueOf(item.getThreat()));
                parameter("timedDelay", String.valueOf(item.getTimedDelay()));
                parameter("burnDuration", String.valueOf(item.getBurnDuration()));
                parameter("health", String.valueOf(item.getHealth()));
                parameter("mass", String.valueOf(item.getMass()));

                parameter("attackCategory", item.getAttackCategory().toString());
                parameter("collideType", item.getCollideType().toString());
                parameter("damageType", item.getDamageType().toString());
                parameter("processType", item.getProcessType().toString());

            }
        };

        return result;
    }

    private ControlBuilder createGameLevelControl(final GameLevel item) {
        String name = item.getLevelName();
        ResourceBundle bundle = item.getResourceBundle();

        ControlBuilder result = new ControlBuilder(name, "level") {
            {
                parameter("title", name);

                parameter("author", item.getAuthor());
                parameter("description", item.getDescription());
                parameter("email", item.getEmail());
                parameter("titlestring", item.getTitle());
            }
        };

        return result;
    }

    private ControlBuilder createObjectControl(final GameObject item) {
        String name = getResourceString(item.getNameStringId());

        ControlBuilder result = new ControlBuilder(name, "object") {
            {
                parameter("title", name);

                parameter("iconGui", getResourceImageName(item.getGuiIconResource()));
                parameter("iconInHand", getResourceImageName(item.getInHandIconResource()));
            }
        };

        return result;
    }

    private ControlBuilder createTrapControl(Trap item) {
        String name = getResourceString(item.getNameStringId());

        ControlBuilder result = new ControlBuilder(name, "trap") {
            {
                parameter("title", name);

                parameter("iconGui", getResourceImageName(item.getGuiIcon()));
                parameter("iconFlower", getResourceImageName(item.getFlowerIcon()));
                parameter("iconEditor", getResourceImageName(item.getEditorIcon()));
            }
        };

        return result;
    }

    private ControlBuilder createDoorControl(Door item) {
        String name = getResourceString(item.getNameStringId());

        ControlBuilder result = new ControlBuilder(name, "door") {
            {
                parameter("title", name);

                parameter("iconGui", getResourceImageName(item.getGuiIcon()));
                parameter("iconFlower", getResourceImageName(item.getFlowerIcon()));
                parameter("iconEditor", getResourceImageName(item.getEditorIcon()));
            }
        };

        return result;
    }

    private ControlBuilder createEffectControl(Effect item) {
        String name = item.getName();

        ControlBuilder result = new ControlBuilder(name, "effect") {
            {
                parameter("title", name);

                parameter("airFriction", String.valueOf(item.getAirFriction()));
                parameter("circularPathRate", String.valueOf(item.getCircularPathRate()));
                parameter("directionalSpread", String.valueOf(item.getDirectionalSpread()));

                parameter("elasticity", String.valueOf(item.getElasticity()));
                parameter("elementsPerTurn", String.valueOf(item.getElementsPerTurn()));
                parameter("fadeDuration", String.valueOf(item.getFadeDuration()));
                parameter("generateRandomness", String.valueOf(item.getGenerateRandomness()));
                parameter("originRange", item.getInnerOriginRange() + " - " + item.getOuterOriginRange());
                parameter("heightLimit", item.getLowerHeightLimit() + " - " + item.getUpperHeightLimit());
                parameter("mass", String.valueOf(item.getMass()));

                parameter("generationType", item.getGenerationType().toString());

                parameter("hp", item.getMinHp() + " - " + item.getMaxHp());
                parameter("scale", item.getMinScale() + " - " + item.getMaxScale());
                parameter("speedXy", item.getMinSpeedXy() + " - " + item.getMaxSpeedXy());
                parameter("speedYz", item.getMinSpeedYz() + " - " + item.getMaxSpeedYz());

                parameter("misc2", String.valueOf(item.getMisc2()));
                parameter("misc3", String.valueOf(item.getMisc3()));
                parameter("orientationRange", String.valueOf(item.getOrientationRange()));
                parameter("radius", String.valueOf(item.getRadius()));
                parameter("spriteSpinRateRange", String.valueOf(item.getSpriteSpinRateRange()));
                parameter("whirlpoolRate", String.valueOf(item.getWhirlpoolRate()));
            }
        };

        return result;
    }

    private String getResourceImageName(ArtResource resource) {
        String result = (resource != null && resource.getName() != null) ? resource.getName() + ".png" : "&mask&transparent.png";

        String textureName = ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                + File.separator + result);

        TextureKey textureKey = new TextureKey(textureName, false);
        AssetInfo assetInfo = app.getAssetManager().locateAsset(textureKey);

        return (assetInfo != null ? textureName : ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                + File.separator + "&mask&transparent.png"));
    }

    private String getResourceString(int id) {
        String result = "";

        if (id != 0) {
            result = Utils.getMainTextResourceBundle().getString(Integer.toString(id));
        }

        return result;
    }
}
