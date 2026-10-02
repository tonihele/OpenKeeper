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
package toniarts.openkeeper.game.state;

import com.jme3.input.KeyNames;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyIdCreator;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.controls.DropDownSelectionChangedEvent;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.controls.checkbox.builder.CheckboxBuilder;
import de.lessvoid.nifty.controls.dropdown.builder.DropDownBuilder;
import de.lessvoid.nifty.controls.label.builder.LabelBuilder;
import de.lessvoid.nifty.controls.slider.builder.SliderBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import org.bushe.swing.event.EventTopicSubscriber;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.gui.nifty.table.TableRow;
import toniarts.openkeeper.utils.DisplayMode;
import toniarts.openkeeper.utils.DisplayModeUtils;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collections;
import java.util.List;

/**
 * The graphics, sound and control option pages of the in-game pause menu. They
 * offer the same options as the main menu.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
final class PauseMenuSettings {

    private static final Logger logger = System.getLogger(PauseMenuSettings.class.getName());

    private static final String LABEL_STYLE = "textSmall";

    private final Nifty nifty;
    private final Main app;
    private final Screen screen;

    private final EventTopicSubscriber<DropDownSelectionChangedEvent> resolutionSubscriber = this::onResolutionChanged;
    private final EventTopicSubscriber<CheckBoxStateChangedEvent> fullscreenSubscriber = this::onFullscreenChanged;

    PauseMenuSettings(Nifty nifty, Main app, Screen screen) {
        this.nifty = nifty;
        this.app = app;
        this.screen = screen;
    }

    // ------------------------------------------------------------ graphics

    void buildGraphics(Element left, Element right) {
        addDropDown(left, "${menu.134}", "resolution", "220px");
        addCheckBox(left, " Fullscreen", "fullscreen");
        addDropDown(left, "Antialiasing", "antialiasing", "100px");
        addDropDown(left, "OpenGL", "openGl", "220px");
        addCheckBox(left, " SSAO", "ssao");

        addDropDown(right, "Bit depth", "bitDepth", "100px");
        addDropDown(right, "Refresh rate", "refreshRate", "100px");
        addCheckBox(right, " Vertical sync", "verticalSync");
        addDropDown(right, "Anisotropic filtering", "anisotropicFiltering", "100px");

        addLabel(right, "Display mode changes take effect after restarting the game");

        loadGraphics();

        nifty.unsubscribe("resolution", resolutionSubscriber);
        nifty.unsubscribe("fullscreen", fullscreenSubscriber);
        nifty.subscribe(screen, "resolution", DropDownSelectionChangedEvent.class, resolutionSubscriber);
        nifty.subscribe(screen, "fullscreen", CheckBoxStateChangedEvent.class, fullscreenSubscriber);
    }

    private void loadGraphics() {
        AppSettings settings = Main.getUserSettings().getAppSettings();

        DisplayMode mdm = new DisplayMode(settings);
        List<DisplayMode> resolutions = DisplayModeUtils.getInstance().getDisplayModes();
        int resolutionSelectedIndex = Collections.binarySearch(resolutions, mdm);

        DropDown res = find("resolution", DropDown.class);
        res.addAllItems(resolutions);
        if (resolutionSelectedIndex >= 0) {
            res.selectItemByIndex(resolutionSelectedIndex);
        }

        DropDown refresh = find("refreshRate", DropDown.class);
        DropDown bitDepths = find("bitDepth", DropDown.class);
        if (resolutionSelectedIndex >= 0) {
            refresh.addAllItems(resolutions.get(resolutionSelectedIndex).getRefreshRates());
            refresh.selectItem(settings.getFrequency());
            bitDepths.addAllItems(resolutions.get(resolutionSelectedIndex).getBitDepths());
            bitDepths.selectItem(settings.getDepthBits());
        } else {
            refresh.addAllItems(mdm.getRefreshRates());
            bitDepths.addAllItems(mdm.getBitDepths());
        }
        if (!settings.isFullscreen()) {
            refresh.disable();
        }

        CheckBox fullscreen = find("fullscreen", CheckBox.class);
        fullscreen.setChecked(settings.isFullscreen());
        fullscreen.setEnabled(DisplayModeUtils.getInstance().isFullScreenSupported());

        find("verticalSync", CheckBox.class).setChecked(settings.isVSync());

        DropDown aa = find("antialiasing", DropDown.class);
        aa.addAllItems(Settings.SAMPLES);
        if (!Settings.SAMPLES.contains(settings.getSamples())) {
            aa.addItem(settings.getSamples());
        }
        aa.selectItem(settings.getSamples());

        DropDown af = find("anisotropicFiltering", DropDown.class);
        af.addAllItems(Settings.ANISOTROPHIES);
        int selectedAF = Main.getUserSettings().getInteger(Settings.Setting.ANISOTROPY);
        if (!Settings.ANISOTROPHIES.contains(selectedAF)) {
            af.addItem(selectedAF);
        }
        af.selectItem(selectedAF);

        DropDown ogl = find("openGl", DropDown.class);
        ogl.addAllItems(Settings.OPENGL);
        ogl.selectItem(settings.getRenderer());

        find("ssao", CheckBox.class).setChecked(Main.getUserSettings().getBoolean(Settings.Setting.SSAO));
    }

    /**
     * Stores the graphics settings. Display mode changes need a restart to take
     * effect, restarting would end the running game so it is left to the user.
     */
    void applyGraphics() {
        Settings settings = Main.getUserSettings();
        DisplayMode mdm = (DisplayMode) find("resolution", DropDown.class).getSelection();

        if (mdm != null) {
            settings.getAppSettings().setResolution(mdm.getWidth(), mdm.getHeight());
        }
        settings.getAppSettings().setDepthBits((Integer) find("bitDepth", DropDown.class).getSelection());
        settings.getAppSettings().setFrequency((Integer) find("refreshRate", DropDown.class).getSelection());
        settings.getAppSettings().setFullscreen(find("fullscreen", CheckBox.class).isChecked());
        settings.getAppSettings().setVSync(find("verticalSync", CheckBox.class).isChecked());
        settings.getAppSettings().setRenderer((String) find("openGl", DropDown.class).getSelection());
        settings.getAppSettings().setSamples((Integer) find("antialiasing", DropDown.class).getSelection());
        settings.setSetting(Settings.Setting.ANISOTROPY, find("anisotropicFiltering", DropDown.class).getSelection());
        settings.setSetting(Settings.Setting.SSAO, find("ssao", CheckBox.class).isChecked());

        save();
        app.setViewProcessors();
    }

    private void onResolutionChanged(String id, DropDownSelectionChangedEvent event) {
        DisplayMode selection = (DisplayMode) event.getSelection();

        DropDown bitDepth = find("bitDepth", DropDown.class);
        bitDepth.clear();
        bitDepth.addAllItems(selection.getBitDepths());
        bitDepth.selectItemByIndex(bitDepth.itemCount() - 1);

        DropDown refresh = find("refreshRate", DropDown.class);
        refresh.clear();
        refresh.addAllItems(selection.getRefreshRates());
        refresh.selectItemByIndex(refresh.itemCount() - 1);
        refresh.setEnabled(find("fullscreen", CheckBox.class).isChecked());
    }

    private void onFullscreenChanged(String id, CheckBoxStateChangedEvent event) {
        find("refreshRate", DropDown.class).setEnabled(event.isChecked());
    }

    // --------------------------------------------------------------- sound

    void buildSound(Element parent) {
        addSlider(parent, "${menu.1460}", "masterVolume", null, 0f, 1f, 0.01f);
        addSlider(parent, "${menu.1457}", "voiceVolume", "voiceEnabled", 0f, 1f, 0.01f);
        addSlider(parent, "${menu.1459}", "musicVolume", "musicEnabled", 0f, 1f, 0.01f);
        addSlider(parent, "${menu.1458}", "sfxVolume", "sfxEnabled", 0f, 1f, 0.01f);

        Settings settings = Main.getUserSettings();
        find("masterVolume", Slider.class).setValue(settings.getFloat(Settings.Setting.MASTER_VOLUME));
        find("voiceVolume", Slider.class).setValue(settings.getFloat(Settings.Setting.VOICE_VOLUME));
        find("musicVolume", Slider.class).setValue(settings.getFloat(Settings.Setting.MUSIC_VOLUME));
        find("sfxVolume", Slider.class).setValue(settings.getFloat(Settings.Setting.SFX_VOLUME));

        find("voiceEnabled", CheckBox.class).setChecked(settings.getBoolean(Settings.Setting.VOICE_ENABLED));
        find("musicEnabled", CheckBox.class).setChecked(settings.getBoolean(Settings.Setting.MUSIC_ENABLED));
        find("sfxEnabled", CheckBox.class).setChecked(settings.getBoolean(Settings.Setting.SFX_ENABLED));
    }

    void applySound() {
        Settings settings = Main.getUserSettings();

        settings.setSetting(Settings.Setting.MASTER_VOLUME, find("masterVolume", Slider.class).getValue());
        settings.setSetting(Settings.Setting.VOICE_VOLUME, find("voiceVolume", Slider.class).getValue());
        settings.setSetting(Settings.Setting.MUSIC_VOLUME, find("musicVolume", Slider.class).getValue());
        settings.setSetting(Settings.Setting.SFX_VOLUME, find("sfxVolume", Slider.class).getValue());

        settings.setSetting(Settings.Setting.VOICE_ENABLED, find("voiceEnabled", CheckBox.class).isChecked());
        settings.setSetting(Settings.Setting.MUSIC_ENABLED, find("musicEnabled", CheckBox.class).isChecked());
        settings.setSetting(Settings.Setting.SFX_ENABLED, find("sfxEnabled", CheckBox.class).isChecked());

        Main.setupNiftySound(nifty);
        save();
    }

    // ------------------------------------------------------------ controls

    void buildControls(Element left, Element right) {
        new ControlBuilder("keyboardSetup", "table") {
            {
                parameter("vertical", "on");
                parameter("displayItems", "4");
                parameter("selection", "Single");
                parameter("colCount", "2");
                parameter("col0", "${menu.2845};55;java.lang.String;#32050c30");
                parameter("col1", "${menu.2846};45;java.lang.String;#32050c30");
                width("90%");
            }
        }.build(left);

        addSlider(right, "${menu.1466}", "mouseSensitivity", null, 0.5f, 2f, 0.1f);
        addSlider(right, "${menu.1469}", "gameSpeed", null, 0.25f, 4f, 0.25f);
        addSlider(right, "${menu.1468}", "scrollSpeed", null, 0.25f, 4f, 0.25f);
        addCheckBox(right, " ${menu.2840}", "invertMouse");

        // Same as in the main menu, shows the defaults
        ListBox<TableRow> listBox = find("keyboardSetup", ListBox.class);
        int i = 0;
        for (Settings.Setting setting : Settings.Setting.getSettings(Settings.SettingCategory.CONTROLS)) {
            String keys = "";
            if (setting.getSpecialKey() != null) {
                keys = (KeyNames.getName(setting.getSpecialKey()) + " + ").replace("Left ", "").replace("Right ", "");
            }
            keys += KeyNames.getName((int) setting.getDefaultValue()).replace("Left ", "").replace("Right ", "");
            listBox.addItem(new TableRow(i++, String.format("${menu.%s}", setting.getTranslationKey()), keys));
        }
        listBox.selectItemByIndex(0);

        find("mouseSensitivity", Slider.class).setValue((float) Settings.Setting.MOUSE_SENSITIVITY.getDefaultValue());
        find("gameSpeed", Slider.class).setValue((float) Settings.Setting.GAME_SPEED.getDefaultValue());
        find("scrollSpeed", Slider.class).setValue((float) Settings.Setting.SCROLL_SPEED.getDefaultValue());
        find("invertMouse", CheckBox.class).setChecked((boolean) Settings.Setting.MOUSE_INVERT.getDefaultValue());
    }

    // ------------------------------------------------------------- helpers

    void cleanup() {
        nifty.unsubscribe("resolution", resolutionSubscriber);
        nifty.unsubscribe("fullscreen", fullscreenSubscriber);
    }

    private void save() {
        try {
            Settings.getInstance().save();
        } catch (IOException ex) {
            logger.log(Level.ERROR, "Failed to save the settings!", ex);
        }
    }

    private <T extends de.lessvoid.nifty.controls.NiftyControl> T find(String id, Class<T> type) {
        return screen.findNiftyControl(id, type);
    }

    private static Element addRow(Element parent) {
        return new PanelBuilder("row-" + NiftyIdCreator.generate()) {
            {
                childLayoutHorizontal();
                width("100%");
                paddingBottom("4px");
            }
        }.build(parent);
    }

    private static void addLabel(Element parent, String text) {
        new LabelBuilder("label-" + NiftyIdCreator.generate(), text) {
            {
                style(LABEL_STYLE);
                wrap(true);
                width("90%");
                alignLeft();
                textHAlignLeft();
            }
        }.build(parent);
    }

    private static void addDropDown(Element parent, String label, String id, String width) {
        Element row = addRow(parent);
        new LabelBuilder("label-" + NiftyIdCreator.generate(), label) {
            {
                style(LABEL_STYLE);
                width("45%");
                textHAlignLeft();
                valignCenter();
            }
        }.build(row);
        new DropDownBuilder(id) {
            {
                width(width);
                valignCenter();
            }
        }.build(row);
    }

    private static void addCheckBox(Element parent, String label, String id) {
        Element row = addRow(parent);
        new CheckboxBuilder(id) {
            {
                valignCenter();
            }
        }.build(row);
        new LabelBuilder("label-" + NiftyIdCreator.generate(), label) {
            {
                style(LABEL_STYLE);
                textHAlignLeft();
                valignCenter();
            }
        }.build(row);
    }

    private static void addSlider(Element parent, String label, String id, String checkBoxId, float min, float max, float step) {
        Element row = addRow(parent);
        new LabelBuilder("label-" + NiftyIdCreator.generate(), label) {
            {
                style(LABEL_STYLE);
                width("30%");
                textHAlignLeft();
                valignCenter();
            }
        }.build(row);
        new SliderBuilder(id, false) {
            {
                min(min);
                max(max);
                initial(max);
                stepSize(step);
                buttonStepSize(step);
                width("45%");
                valignCenter();
            }
        }.build(row);
        if (checkBoxId != null) {
            new CheckboxBuilder(checkBoxId) {
                {
                    valignCenter();
                    marginLeft("10px");
                }
            }.build(row);
        }
    }
}
