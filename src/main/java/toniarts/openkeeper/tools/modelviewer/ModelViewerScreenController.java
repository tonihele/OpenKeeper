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
import de.lessvoid.nifty.NiftyMethodInvoker;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.TextBuilder;
import de.lessvoid.nifty.controls.scrollpanel.builder.ScrollPanelBuilder;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.controls.DropDownSelectionChangedEvent;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.ListBoxSelectionChangedEvent;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.controls.TextFieldChangedEvent;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.nifty.spi.render.RenderFont;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.sound.SoundFile;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.EffectElement;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Utils;

/**
 *
 * @author archdemon
 */
public final class ModelViewerScreenController implements ScreenController {

    public final static String ID_SCREEN = "start";

    private final static String ID_ITEMS = "items";
    private final static String ID_TYPES = "types";
    private final static String ID_ITEM = "item";
    private final static String ID_SOUNDS = "sounds";
    private final static String ID_FILTER = "filter";
    private final static String ID_AUTO_ROTATE = "autoRotate";
    private final static String ID_SHOW_NORMALS = "showNormals";
    private final static String ID_WIREFRAME = "wireframe";
    private final static String ID_SHOW_CEILING = "showCeiling";
    private final static String ID_OVERLAY = "enableOverlay";
    private final static String ID_EFFECT_DETAILS = "showEffectDetails";

    private final ModelViewer app;
    private final Nifty nifty;
    private Screen screen;
    private final RenderFont detailsFont;
    private Object selectedItem;
    private Object inspectedItem;
    private List<SoundFile> selectedAudio;
    private boolean showEffectDetails;

    public ModelViewerScreenController(ModelViewer app) {
        this.app = app;

        nifty = app.getNifty();
        screen = nifty.getCurrentScreen();

        // Set default font
        RenderFont font = nifty.createFont("Interface/Fonts/Frontend14.fnt");
        detailsFont = font;
        nifty.getRenderEngine().setFont(font);
        nifty.registerMouseCursor("pointer", "Interface/Cursors/Idle.png", 4, 4);

        // Set some Nifty stuff
        nifty.addResourceBundle("menu", Main.getResourceBundle("Interface/Texts/Text"));
        nifty.addResourceBundle("speech", Main.getResourceBundle("Interface/Texts/Speech"));
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        this.screen = screen;
        // Nifty dispatches rapid clicks separately from single clicks.
        for (String option : List.of(ID_AUTO_ROTATE, ID_SHOW_NORMALS, ID_WIREFRAME, ID_SHOW_CEILING, ID_EFFECT_DETAILS, ID_OVERLAY)) {
            CheckBox checkBox = screen.findNiftyControl(option, CheckBox.class);
            checkBox.getElement().getElementInteraction().getPrimary()
                    .setOnMultiClickMethod(new NiftyMethodInvoker(nifty, "onClick()", checkBox));
        }
        DropDown<String> overlays = screen.findNiftyControl("overlayModel", DropDown.class);
        overlays.addAllItems(app.getOverlayModels());
        if (overlays.itemCount() > 0) {
            overlays.selectItemByIndex(0);
        }
        // Fill types
        app.fillTypes();
    }

    @Override
    public void onStartScreen() {
        setAutoRotateChecked(app.isAutoRotateEnabled());
        setShowNormalsChecked(app.isShowNormalsEnabled());
        setWireframeChecked(app.isWireframeEnabled());
        setCeilingChecked(app.isCeilingVisible());
    }

    boolean isTyping() {
        Element focus = screen.getFocusHandler().getKeyboardFocusElement();
        return focus != null && focus.getNiftyControl(TextField.class) != null;
    }

    @Override
    public void onEndScreen() {
    }

    @NiftyEventSubscriber(id = ID_ITEMS)
    public void onListBoxSelectionChanged(final String id, final ListBoxSelectionChangedEvent<Object> event) {

        List<Object> selection = event.getSelection();

        if (selection.size() == 1) {
            app.onSelectionChanged(selection.get(0));
        } else if (selection.isEmpty()) {
            selectionChanged(null);
        }
    }

    @NiftyEventSubscriber(id = ID_TYPES)
    public void onTypeChanged(final String id, final DropDownSelectionChangedEvent<ModelViewer.Types> event) {
        app.fillList(event.getSelection());
    }

    @NiftyEventSubscriber(id = ID_FILTER)
    public void onFilterChanged(final String id, final TextFieldChangedEvent event) {
        app.filterList(event.getText());
    }

    @NiftyEventSubscriber(id = ID_AUTO_ROTATE)
    public void onAutoRotateChanged(final String id, final CheckBoxStateChangedEvent event) {
        app.setAutoRotateEnabled(event.isChecked());
    }

    void setAutoRotateChecked(boolean enabled) {
        CheckBox checkBox = screen.findNiftyControl(ID_AUTO_ROTATE, CheckBox.class);
        if (checkBox != null && checkBox.isChecked() != enabled) {
            checkBox.setChecked(enabled);
        }
    }

    @NiftyEventSubscriber(id = ID_SHOW_NORMALS)
    public void onShowNormalsChanged(final String id, final CheckBoxStateChangedEvent event) {
        app.setShowNormalsEnabled(event.isChecked());
    }

    void setShowNormalsChecked(boolean enabled) {
        CheckBox checkBox = screen.findNiftyControl(ID_SHOW_NORMALS, CheckBox.class);
        if (checkBox != null && checkBox.isChecked() != enabled) {
            checkBox.setChecked(enabled);
        }
    }

    @NiftyEventSubscriber(id = ID_WIREFRAME)
    public void onWireframeChanged(final String id, final CheckBoxStateChangedEvent event) {
        app.setWireframeEnabled(event.isChecked());
    }

    void setWireframeChecked(boolean enabled) {
        CheckBox checkBox = screen.findNiftyControl(ID_WIREFRAME, CheckBox.class);
        if (checkBox != null && checkBox.isChecked() != enabled) {
            checkBox.setChecked(enabled);
        }
    }

    @NiftyEventSubscriber(id = ID_SHOW_CEILING)
    public void onCeilingChanged(final String id, final CheckBoxStateChangedEvent event) {
        app.setCeilingVisible(event.isChecked());
    }

    void setCeilingChecked(boolean enabled) {
        CheckBox checkBox = screen.findNiftyControl(ID_SHOW_CEILING, CheckBox.class);
        if (checkBox != null && checkBox.isChecked() != enabled) {
            checkBox.setChecked(enabled);
        }
    }

    @NiftyEventSubscriber(id = "overlayModel")
    public void onOverlayModelChanged(final String id, final DropDownSelectionChangedEvent<String> event) {
        app.setOverlayModel(event.getSelection());
    }

    @NiftyEventSubscriber(id = ID_OVERLAY)
    public void onOverlayChanged(final String id, final CheckBoxStateChangedEvent event) {
        app.setOverlayEnabled(event.isChecked());
    }

    @NiftyEventSubscriber(id = ID_EFFECT_DETAILS)
    public void onEffectDetailsChanged(final String id, final CheckBoxStateChangedEvent event) {
        showEffectDetails = event.isChecked();
        rebuildItem();
    }

    public void inspectEffect(String id) {
        Effect effect = app.getKwdFile().getEffect(Integer.parseInt(id));
        if (effect != null) {
            inspectedItem = effect;
            rebuildItem();
        }
    }

    public void inspectParticle(String id) {
        EffectElement element = app.getKwdFile().getEffectElement(Integer.parseInt(id));
        if (element != null) {
            inspectedItem = element;
            rebuildItem();
        }
    }

    public void resetEffectInspection() {
        inspectedItem = selectedItem;
        rebuildItem();
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

    void selectionChanged(Object item) {
        selectedItem = item;
        screen.findNiftyControl(ID_OVERLAY, CheckBox.class).setEnabled(item instanceof Effect);
        screen.findNiftyControl("overlayModel", DropDown.class).setEnabled(item instanceof Effect);
        inspectedItem = item;
        selectedAudio = null;
        if (showEffectDetails) {
            rebuildItem();
        }
    }

    protected void setupItem(Object item, List<SoundFile> audio) {
        boolean sameSelection = selectedItem == item;
        selectedItem = item;
        if (!sameSelection) {
            inspectedItem = item;
        }
        selectedAudio = audio;
        if (!showEffectDetails || !sameSelection) {
            rebuildItem();
        }
    }

    private void rebuildItem() {
        Object item = selectedItem;
        List<SoundFile> audio = selectedAudio;
        Element panel = screen.findElementById(ID_ITEM);
        for (Element element : panel.getChildren()) {
            element.markForRemoval();
        }

        Element el = showEffectDetails ? createEffectInspector(panel) : createItemDetails(panel, item);
        if (el != null) {
            DropDown<SoundFile> dropDown = (DropDown<SoundFile>) el.findNiftyControl(ID_SOUNDS, DropDown.class);
            if (dropDown != null) {
                dropDown.clear();
                if (audio != null) {
                    dropDown.addAllItems(audio);
                }
            }
        }
        //panel.addChild(el);
        panel.layoutElements();
        panel.reactivate();
    }

    /** Creates the ordinary property controls for a selected asset definition. */
    private Element createItemDetails(Element panel, Object item) {
        return switch (item) {
            case Creature creature -> createCreatureControl(creature).build(panel);
            case Room room -> createRoomControl(room).build(panel);
            case Terrain terrain -> createTerrainControl(terrain).build(panel);
            case Shot shot -> createShotControl(shot).build(panel);
            case GameLevel level -> createGameLevelControl(level).build(panel);
            case GameObject object -> createObjectControl(object).build(panel);
            case Trap trap -> createTrapControl(trap).build(panel);
            case Door door -> createDoorControl(door).build(panel);
            case Effect effect -> createEffectControl(effect).build(panel);
            case null, default -> null;
        };
    }

    private Element createEffectInspector(Element panel) {
        EffectInspector.Details details = new EffectInspector(app.getKwdFile()).describe(inspectedItem);
        PanelBuilder content = new PanelBuilder("effectInspectorContent");
        content.childLayoutVertical();
        content.width("100%");
        content.padding("6px");
        int width = Math.max(120, panel.getWidth() - 40);
        addInspectorText(content, details.title(), width, null);
        addInspectorText(content, "Inspecting definitions; preview playback is unchanged.", width, null);
        if (selectedItem instanceof Effect) {
            addInspectorText(content, "Overlay models share the effect origin. Changing the overlay restarts the preview; the assembled Portal uses its pit base.", width, null);
        }
        if (inspectedItem != selectedItem) {
            addInspectorText(content, "Back to selected item", width, "resetEffectInspection()");
        }
        for (String field : details.fields()) {
            addInspectorText(content, field, width, null);
        }
        for (EffectInspector.Link link : details.links()) {
            String method = link.kind() == EffectInspector.Kind.EFFECT ? "inspectEffect" : "inspectParticle";
            addInspectorText(content, link.label(), width, method + "(" + link.id() + ")");
        }
        ScrollPanelBuilder scroll = new ScrollPanelBuilder("effectInspector");
        scroll.width("100%");
        scroll.height("100%");
        scroll.parameter("horizontal", "false");
        scroll.panel(content);
        return scroll.build(panel);
    }

    private void addInspectorText(PanelBuilder parent, String text, int width, String action) {
        List<String> lines = new ArrayList<>();
        String line = "";
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && detailsFont.getWidth(candidate) > width) {
                lines.add(line);
                line = word;
            } else {
                line = candidate;
            }
        }
        lines.add(line);
        TextBuilder label = new TextBuilder();
        label.style("text");
        label.font("Interface/Fonts/Frontend14.fnt");
        label.text(String.join("\n", lines));
        label.width("100%");
        label.height((detailsFont.getHeight() * lines.size() + 10) + "px");
        label.textHAlignLeft();
        label.textVAlignCenter();
        label.visibleToMouse(false);
        PanelBuilder row = new PanelBuilder();
        row.childLayoutVertical();
        row.width("100%");
        row.height((detailsFont.getHeight() * lines.size() + 10) + "px");
        row.backgroundColor(action == null ? "#111e" : "#234f");
        if (action != null) {
            row.visibleToMouse(true);
            row.interactOnClick(action);
        }
        row.text(label);
        parent.panel(row);
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
                parameter("title", name + " [" + item.getEffectId() + "]");
                parameter("relatedTo", app.getEffectReferences(item));

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

        String textureName = AssetUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                + File.separator + result);

        TextureKey textureKey = new TextureKey(textureName, false);
        AssetInfo assetInfo = app.getAssetManager().locateAsset(textureKey);

        return (assetInfo != null ? textureName : AssetUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
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
