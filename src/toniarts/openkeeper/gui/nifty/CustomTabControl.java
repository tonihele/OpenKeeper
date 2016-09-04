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

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.controls.Tab;
import de.lessvoid.nifty.controls.TabGroup;
import de.lessvoid.nifty.controls.tabs.TabGroupMember;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import java.util.logging.Logger;

/**
 *
 * @author ArchDemon
 */
public class CustomTabControl extends AbstractController implements Tab, TabGroupMember {

    private static final Logger log = Logger.getLogger(CustomTabControl.class.getName());
    /**
     * The tab group that is the parent of this tab. This might be {@code null}
     * for the time this tab is not a part of tab group.
     */

    private CustomTabGroupControl parentGroup;

    /**
     * The caption of this tab.
     */
    private String tabImage;
    private String tabImageActive;
    private String hint;
    private String tooltip;

    @Override
    public void bind(Nifty nifty, Screen screen, Element elmnt, Parameters prmtrs) {
        bind(elmnt);

        if (elmnt.getId() == null) {
            log.warning("Button element has no ID and can't publish any events properly.");
        }

        String image = prmtrs.get("image");
        if (image != null) {
            setImage(image);
        }

        String imageActive = prmtrs.get("active");
        if (imageActive != null) {
            setImageActive(imageActive);
        }
        
        String hintText = prmtrs.get("hintText");
        if (hintText != null) {
            setHint(hintText);
        }
        
        String tooltipText = prmtrs.get("tooltip");
        if (tooltipText != null) {
            setTooltip(tooltipText);
        }
    }

    @Override
    public void setCaption(final String caption) {
    }

    public void setImage(final String imageUrl) {
        if (!imageUrl.equals(tabImage)) {
            tabImage = imageUrl;
        }
    }

    public void setImageActive(final String imageUrl) {
        if (!imageUrl.equals(tabImage)) {
            tabImageActive = imageUrl;
        }
    }

    @Override
    public boolean hasParent() {
        return parentGroup != null;
    }

    @Override
    public String getCaption() {
        return "";
    }

    public String getImage() {
        if (tabImage == null) {
            log.warning("Tab image is not set yet.");
            return "";
        }
        return tabImage;
    }

    public String getImageActive() {
        if (tabImage == null) {
            log.warning("Tab image is not set yet.");
            return "";
        }
        return tabImageActive;
    }

    @Override
    public TabGroup getParentGroup() {
        return parentGroup;
    }

    @Override
    public boolean inputEvent(final NiftyInputEvent inputEvent) {
        return true;
    }

    @Override
    public boolean isVisible() {
        if (parentGroup != null) {
            Tab selectedTab = parentGroup.getSelectedTab();
            return selectedTab != null && selectedTab.equals(this);
        }
        return false;
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void setParentTabGroup(final TabGroup tabGroup) {
        parentGroup = (CustomTabGroupControl) tabGroup;
    }

    public void setHint(final String hintText) {
        this.hint = hintText;
    }

    public String getHint() {
        return this.hint;
    }
    
    public void setTooltip(final String tooltip) {
        this.tooltip = tooltip;
    }
    
    public String getTooltip() {
        return this.tooltip;
    }
}
