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
package toniarts.openkeeper.gui.nifty;

import de.lessvoid.nifty.EndNotify;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.ButtonClickedEvent;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.controls.Tab;
import de.lessvoid.nifty.controls.TabGroup;
import de.lessvoid.nifty.controls.TabSelectedEvent;
import de.lessvoid.nifty.controls.tabs.TabGroupMember;
import de.lessvoid.nifty.controls.tabs.builder.TabBuilder;
import de.lessvoid.nifty.effects.EffectEventId;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.events.ElementShowEvent;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.loaderv2.types.ElementType;
import de.lessvoid.nifty.screen.Screen;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bushe.swing.event.EventTopicSubscriber;

/**
 *
 * @author ArchDemon
 */
public class CustomTabGroupControl extends AbstractController implements TabGroup {

    /**
     * This class is used for the event handlers that are needed for the tab
     * group controls. It will force the tab group to update the visibility
     * settings of all tabs in case the element is shown.
     */
    private static final class TabGroupShowEventSubscriber implements EventTopicSubscriber<ElementShowEvent> {

        @Override
        public void onEvent(final String topic, final ElementShowEvent data) {
            final CustomTabGroupControl control = data.getElement().getControl(CustomTabGroupControl.class);
            if (control != null) {
                control.checkVisibility();
            }
        }
    }

    /**
     * This class is used for the event handlers that monitor the clicks on the
     * buttons that are used to select the active tab in a tab group.
     */
    private static final class ButtonClickEventSubscriber implements EventTopicSubscriber<ButtonClickedEvent> {

        /**
         * The tab group that is effected by this click.
         */
        private final CustomTabGroupControl parentControl;

        /**
         * Create a instance of this event subscriber and set the tab group
         * control that will be effected by this subscriber.
         *
         * @param parent the tab group control that is the parent of this class
         */
        private ButtonClickEventSubscriber(final CustomTabGroupControl parent) {
            parentControl = parent;
        }

        @Override
        public void onEvent(final String topic, final ButtonClickedEvent data) {
            Element element = data.getButton().getElement();
            if (element != null) {
                parentControl.processButtonClick(element);
            }
        }
    }

    /**
     * This end notification is supposed to used in case a check of the
     * visibility values of the tab control is required once a operation is
     * done.
     */
    private static final class CheckVisibilityEndNotify implements EndNotify {

        /**
         * The tab group control that is the target for the visibility check.
         */
        private final CustomTabGroupControl parentControl;

        /**
         * The next notification that is supposed to be called in chain.
         */
        private final EndNotify next;

        /**
         * Constructor for the check visibility end notification.
         *
         * @param control the tab group control that is the target
         * @param nextNotify the next notification to call or {@code null} in
         * case there is none
         */
        private CheckVisibilityEndNotify(final CustomTabGroupControl control, final EndNotify nextNotify) {
            parentControl = control;
            next = nextNotify;
        }

        @Override
        public void perform() {
            parentControl.checkVisibility();
            if (next != null) {
                next.perform();
            }
        }
    }

    /**
     * This class is used to call the adding operation for a tab again, once the
     * tab was moved to the proper location. Hopefully people manage to add the
     * tab at the right spot from the start, so using this class won't be needed
     * that often.
     */
    private static final class TabAddMoveEndNotify implements EndNotify {

        /**
         * The tab group control where the tab is supposed to be added to.
         */
        private final CustomTabGroupControl parentControl;

        /**
         * The tab that is supposed to be added.
         */
        private final Tab tabToAdd;

        /**
         * Create a instance of this notification handler.
         *
         * @param control the tab group control where the tab is added to
         * @param tab the tab that is added
         */
        private TabAddMoveEndNotify(final CustomTabGroupControl control, final Tab tab) {
            parentControl = control;
            tabToAdd = tab;
        }

        @Override
        public void perform() {
            parentControl.addTab(tabToAdd);
        }
    }

    /**
     * The logger that takes care for the output of log messages in this class.
     */
    private static final Logger log = Logger.getLogger(CustomTabGroupControl.class.getName());

    /**
     * The subscriber of the show events for this control. This is required to
     * fix the visibility values one the tab control is displayed.
     */
    private static final EventTopicSubscriber<ElementShowEvent> showEventSubscriber = new CustomTabGroupControl.TabGroupShowEventSubscriber();

    /**
     * This subscriber is used to monitor the click events on the buttons.
     */
    private final EventTopicSubscriber<ButtonClickedEvent> buttonClickedSubscriber;

    /**
     * This is the panel that is supposed to hold the buttons used to change the
     * currently visible tab.
     */
    private Element tabButtonPanel;

    /**
     * This is the content panel that stores all tabs.
     */
    private Element contentPanel;

    /**
     * The instance of the Nifty-GUI that is parent to this tab group control.
     */
    private Nifty nifty;

    /**
     * The screen that is parent to the element this control is assigned to.
     */
    private Screen screen;

    /**
     * The template of the button that is supposed to be used for each new tab.
     */
    private ElementType buttonTemplate;

    /**
     * The index of the tab that is currently selected or {@code -1} in case no
     * tab is visible.
     */
    private int selectedIndex;

    /**
     * This value is set true once the template is gone.
     */
    private boolean templateRemoved;

    public CustomTabGroupControl() {
        //noinspection ThisEscapedInObjectConstruction
        buttonClickedSubscriber = new CustomTabGroupControl.ButtonClickEventSubscriber(this);
        selectedIndex = -1;
    }

    @Override
    public void addTab(final Element tab) {
        final Tab tabControl = tab.getNiftyControl(Tab.class);
        if (tabControl == null) {
            throw new IllegalArgumentException("Element to add is not a tab.");
        }
        addTab(tabControl);
    }

    @Override
    public void addTab(final TabBuilder tabBuilder) {
        if (nifty == null || screen == null || contentPanel == null) {
            throw new IllegalStateException("Element is not bound yet. Can't add tabs.");
        }
        final Element tab = tabBuilder.build(nifty, screen, contentPanel);
        final Tab tabControl = tab.getNiftyControl(Tab.class);
        if (tabControl == null) {
            throw new IllegalStateException("Tab builder did not create a tab... WTF?!");
        }
        addTab(tabControl);
    }

    @Override
    public void bind(Nifty nifty, Screen screen, Element elmnt, Parameters prmtrs) {
        bind(elmnt);

        this.nifty = nifty;
        this.screen = screen;

        tabButtonPanel = elmnt.findElementById("#tab-button-panel");
        contentPanel = elmnt.findElementById("#tab-content-panel");

        if (tabButtonPanel == null) {
            log.severe("Panel for the tabs not found. Tab group will not work properly. Looked for: #tab-button-panel");
        } else {
            final Element buttonElement = tabButtonPanel.findElementById("#button-template");
            if (buttonElement == null) {
                log.severe("No template for tab button found. Tab group will be unable to display tabs. Looked for: "
                        + "#button-template");
            } else {
                buttonTemplate = buttonElement.getElementType().copy();
                buttonElement.markForRemoval(new EndNotify() {
                    @Override
                    public void perform() {
                        templateRemoved = true;
                    }
                });
            }
        }
        if (contentPanel == null) {
            log.severe("Content panel not found. Tab group will be unable to display tab content. Looked for: "
                    + "#tab-content-pane");
        }
    }

    @Override
    public Tab getSelectedTab() {
        if (selectedIndex == -1 || contentPanel == null) {
            return null;
        }
        return contentPanel.getChildren().get(selectedIndex).getNiftyControl(Tab.class);
    }

    @Override
    public int getSelectedTabIndex() {
        return selectedIndex;
    }

    @Override
    public void init(Parameters prmtrs) {
        super.init(prmtrs);

        if (contentPanel != null) {
            for (final Element element : contentPanel.getChildren()) {
                final Tab tabControl = element.getNiftyControl(Tab.class);
                if (tabControl == null) {
                    log.log(Level.WARNING, "Element without tab control detected. Removing: {0}", element.getId());
                    element.markForRemoval();
                } else {
                    initTab(tabControl);
                    selectedIndex = 0;
                }
            }
        }

        checkVisibility();
    }

    private void initTab(final Tab tab) {
        final int tabIndex = indexOf(tab);
        Element button = getButton(tabIndex);
        if (button == null) {
            if (buttonTemplate == null || nifty == null || screen == null || tabButtonPanel == null) {
                log.severe("Tab can't be initialized. Binding not done yet or binding failed.");
                return;
            }
            final ElementType newButtonTemplate = buttonTemplate.copy();
            newButtonTemplate.getAttributes().set("id", buildTabButtonName(tabIndex));
            newButtonTemplate.getAttributes().set("image", ((CustomTabControl) tab).getImage());
            newButtonTemplate.getAttributes().set("active", ((CustomTabControl) tab).getImageActive());
            newButtonTemplate.getAttributes().set("hintText", ((CustomTabControl) tab).getHint());
            newButtonTemplate.getAttributes().set("tooltip", ((CustomTabControl) tab).getTooltip());
            newButtonTemplate.getAttributes().set("sound", ((CustomTabControl) tab).getSound());
            button = nifty.createElementFromType(screen, tabButtonPanel, newButtonTemplate);
        }
        String buttonId = button.getId();
        if (buttonId != null) {
            nifty.subscribe(screen, buttonId, ButtonClickedEvent.class, buttonClickedSubscriber);
        }

        if (!button.isVisible()) {
            button.show();
        }

        if (tab instanceof TabGroupMember) {
            ((TabGroupMember) tab).setParentTabGroup(this);
        }
    }

    private String buildTabButtonName(final int index) {
        String tabButtonId = "#tabButton-" + index;
        if (tabButtonPanel != null) {
            tabButtonId = tabButtonPanel.getId() + tabButtonId;
        }
        return tabButtonId;
    }

    @Override
    public void addTab(final Tab tab) {
        Element tabElement = tab.getElement();
        if (tabElement != null) {
            Element tabParentElement = tabElement.getParent();
            if (contentPanel != null && !tabParentElement.equals(contentPanel)) {
                tabElement.markForMove(contentPanel, new CustomTabGroupControl.TabAddMoveEndNotify(this, tab));
                return;
            }
        }

        initTab(tab);
        checkVisibility();
    }

    @Override
    public int indexOf(final Tab tab) {
        if (contentPanel == null) {
            return -1;
        }
        final int length = getTabCount();
        final List<Element> elementList = contentPanel.getChildren();
        int result = -1;
        for (int i = 0; i < length; i++) {
            if (tab.equals(elementList.get(i).getNiftyControl(Tab.class))) {
                result = i;
                break;
            }
        }
        return result;
    }

    @Override
    public int getTabCount() {
        return contentPanel != null ? contentPanel.getChildren().size() : 0;
    }

    /**
     * Get the button element at a specified index.
     * <p/>
     * This function ignores the template-button as needed. This is required
     * because Nifty does not provide a blocking way to remove a element from
     * the GUI.
     *
     * @param index the index of the button
     * @return the element of the button or {@code null} in case there is no
     * button assigned to this index
     */
    private Element getButton(final int index) {
        if (tabButtonPanel == null) {
            return null;
        }
        int realIndex = index;

        final List<Element> buttonList = tabButtonPanel.getChildren();
        if (buttonList.isEmpty()) {
            return null;
        }

        if (!templateRemoved) {
            realIndex++;
        }

        if (realIndex >= buttonList.size()) {
            return null;
        }
        return tabButtonPanel.getChildren().get(realIndex);
    }

    /**
     * Get the tab control at a specified index.
     *
     * @param index the index of the tab control
     * @return the tab control
     */
    @Override
    public Tab getTabAtIndex(final int index) {
        if (contentPanel == null) {
            return null;
        }
        return contentPanel.getChildren().get(index).getNiftyControl(Tab.class);
    }

    /**
     * Check the visibility settings of all tabs and correct it as needed.
     */
    private void checkVisibility() {
        if (contentPanel == null) {
            return;
        }
        final int length = getTabCount();
        final List<Element> tabList = contentPanel.getChildren();

        for (int i = 0; i < length; i++) {
            final Element tab = tabList.get(i);
            final Element button = getButton(i);

            if (button == null) {
                log.warning("Something is wrong with the tabs. Tab button not there anymore.");
                continue;
            }

            if (i == selectedIndex) {
                if (!tab.isVisible()) {
                    tab.show();
                }
                if (!"nifty-tab-button-active".equals(button.getStyle())) {
                    button.setStyle("nifty-tab-button-active");
                    button.findElementById("#selector").setVisible(true);
                    button.startEffect(EffectEventId.onCustom, null, "select");
                    //addMargin(i, button);
                }
                button.setRenderOrder(100000);
            } else {
                if (tab.isVisible()) {
                    tab.hide();
                }
                if (!"nifty-tab-button".equals(button.getStyle())) {
                    button.setStyle("nifty-tab-button");
                    button.findElementById("#selector").setVisible(false);
                    button.stopEffect(EffectEventId.onCustom);
                    //addMargin(i, button);
                }
                button.setRenderOrder(0);
            }
        }

        Element element = getElement();
        if (element != null) {
            element.layoutElements();
        }
    }

    @Override
    public boolean inputEvent(final NiftyInputEvent inputEvent) {
        return true;
    }

    @Override
    public boolean isTabInGroup(final Tab tab) {
        return indexOf(tab) > -1;
    }

    @Override
    public void onStartScreen() {
        if (nifty == null || screen == null) {
            log.severe("Starting screen failed. Seems the binding is not done yet.");
        }
        String id = getId();
        if (id != null) {
            nifty.subscribe(screen, id, ElementShowEvent.class, showEventSubscriber);
        }
    }

    @Override
    public void removeTab(final int index) {
    }

    @Override
    public void removeTab(final Tab tab) {
    }

    @Override
    public void removeTab(final Element tab) {
    }

    @Override
    public void removeTab(final int index, final EndNotify notify) {
    }

    @Override
    public void removeTab(final Tab tab, final EndNotify notify) {
    }

    @Override
    public void removeTab(final Element tab, final EndNotify notify) {
    }

    @Override
    public void setSelectedTab(final Tab tab) {
        final int index = indexOf(tab);
        if (index == -1) {
            throw new IllegalArgumentException("The tab to remove is not part of this tab group.");
        }
        setSelectedTabIndex(index);
    }

    @Override
    public void setSelectedTabIndex(final int index) {
        if ((index < 0) || (index >= getTabCount())) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
        }

        selectedIndex = index;
        checkVisibility();
        if (nifty != null) {
            String id = getId();
            if (id != null) {
                Tab tab = getTabAtIndex(index);
                if (tab == null) {
                    log.severe("Tab with valid index returned null. This looks like a internal error.");
                } else {
                    nifty.publishEvent(id, new TabSelectedEvent(this, tab, index));
                }
            }
        }
    }

    @Override
    public void setTabCaption(final int index, final String caption) {
    }

    @Override
    public void setTabCaption(final Tab tab, final String caption) {
    }

    /**
     * Handle a click on a button and switch the tab.
     *
     * @param clickedButton the button that was clicked
     */
    private void processButtonClick(final Element clickedButton) {
        if (tabButtonPanel == null) {
            return;
        }
        final List<Element> buttons = tabButtonPanel.getChildren();
        if (buttons.isEmpty()) {
            return;
        }

        int indexOffset = 0;
        if (!templateRemoved) {
            indexOffset = -1;
        }

        setSelectedTabIndex(buttons.indexOf(clickedButton) + indexOffset);
    }
}
