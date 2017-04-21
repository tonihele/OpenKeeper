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
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.sound.SoundFile;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
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

        if (item instanceof Creature) {
            ControlBuilder cb = createCreatureControl((Creature) item);

            Element el = cb.build(nifty, screen, panel);
            DropDown<SoundFile> dropDown = (DropDown<SoundFile>) el.findNiftyControl(ID_SOUNDS, DropDown.class);
            dropDown.clear();
            dropDown.addAllItems(audio);

            //panel.addChild(el);
            panel.layoutElements();
            panel.reactivate();
        }
    }

    private ControlBuilder createCreatureControl(final Creature creature) {
        String name = getResourceString(creature.getNameStringId());

        ControlBuilder result = new ControlBuilder(name, "creature") {
            {
                parameter("title", name);

                parameter("iconOne", getResourceImageName(creature.getIcon1Resource()));
                parameter("iconTwo", getResourceImageName(creature.getIcon2Resource()));
                parameter("portrait", getResourceImageName(creature.getPortraitResource()));
                parameter("unique", getResourceImageName(creature.getUniqueResource()));
                parameter("fpMelee", getResourceImageName(creature.getFirstPersonMeleeResource()));
                parameter("fpFilter", getResourceImageName(creature.getFirstPersonFilterResource()));

                parameter("description", getResourceString(creature.getGeneralDescriptionStringId()));
                parameter("tooltip", getResourceString(creature.getTooltipStringId()));

                parameter("introduction", getResourceString(creature.getIntroductionStringId()));
                parameter("strength", getResourceString(creature.getStrengthStringId()));
                parameter("uniqueName", getResourceString(creature.getUniqueNameTextId()));
                parameter("weakness", getResourceString(creature.getWeaknessStringId()));

                parameter("angerFood", getResourceString(creature.getAngerStringIdFood()));
                parameter("angerGeneral", getResourceString(creature.getAngerStringIdGeneral()));
                parameter("angerHatred", getResourceString(creature.getAngerStringIdHatred()));
                parameter("angerHeld", getResourceString(creature.getAngerStringIdHeld()));
                parameter("angerLair", getResourceString(creature.getAngerStringIdLair()));
                parameter("angerLonely", getResourceString(creature.getAngerStringIdLonely()));
                parameter("angerPay", getResourceString(creature.getAngerStringIdPay()));
                parameter("angerSlap", getResourceString(creature.getAngerStringIdSlap()));
                parameter("angerTorture", getResourceString(creature.getAngerStringIdTorture()));
                parameter("angerWork", getResourceString(creature.getAngerStringIdWork()));
            }
        };

        return result;
    }

    private String getResourceImageName(ArtResource resource) {
        String result = (resource != null) ? resource.getName() + ".png" : "&mask&transparent.png";

        return ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                + File.separator + result);
    }

    private String getResourceString(int id) {
        String result = "";

        if (id != 0) {
            result = Utils.getMainTextResourceBundle().getString(Integer.toString(id));
        }

        return result;
    }
}
