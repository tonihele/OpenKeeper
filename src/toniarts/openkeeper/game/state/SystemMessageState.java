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
package toniarts.openkeeper.game.state;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyIdCreator;
import de.lessvoid.nifty.builder.EffectBuilder;
import de.lessvoid.nifty.builder.HoverEffectBuilder;
import de.lessvoid.nifty.builder.ImageBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.gui.nifty.message.SystemMessageControl;
import toniarts.openkeeper.tools.convert.ConversionUtils;
/**
 *
 * @author ufdada
 */
public class SystemMessageState extends AbstractPauseAwareState {
    private final float lifeTime = 60000f;
    private final Nifty nifty;
    private final Main app;
    private final Screen hud;
    private final Element systemMessagesQueue;

    public enum MessageType {
        INFO,
        FIGHT,
        ALLY,
        ALLYMSG,
        PLAYEREXIT,
        CREATURE
    };

    SystemMessageState(Main app, boolean enabled) {
        this.nifty = app.getNifty().getNifty();
        this.app = app;
        this.hud = nifty.getScreen("hud");
        this.systemMessagesQueue = this.hud.findElementById("systemMessages");

        this.setEnabled(enabled);
    }
    
    /**
     * Adds a message to the message queue
     * 
     * @param type
     * @param text
     */
    public void addMessage(MessageType type, String text) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.app != null) {
            this.app.enqueue(()-> {
                this.addMessageIcon(type, text);
                return null;
            });
        } else {
            this.addMessageIcon(type, text);
        }
    }
    
    public void addMessageIcon(MessageType type, String text) {
        final String icon = String.format("Textures/GUI/Tabs/Messages/mt-%s-$index.png", type.toString().toLowerCase());
        final String normalIcon = ConversionUtils.getCanonicalAssetKey(icon.replace("$index", "00"));
        final String hoverIcon = ConversionUtils.getCanonicalAssetKey(icon.replace("$index", "01"));
        final String activeIcon = ConversionUtils.getCanonicalAssetKey(icon.replace("$index", "02"));
        
        Element image = new ImageBuilder("sysmessage-" + NiftyIdCreator.generate()){{
            filename(normalIcon);
            //marginLeft("4px");
            visibleToMouse(true);
            visible(false);
            onHoverEffect(new HoverEffectBuilder("imageOverlay"){{
                // using filename leads to an NPE, no idea why...
                getAttributes().setAttribute("filename", hoverIcon);
                post(true);
            }});
            onActiveEffect(new EffectBuilder("imageOverlayPulsate"){{
                getAttributes().setAttribute("filename", activeIcon);
                post(true);
            }});
            onShowEffect(new EffectBuilder("move"){{
                getAttributes().setAttribute("mode", "in");
                getAttributes().setAttribute("direction", "right");
                length(1000);
                startDelay(0);
            }});
            set("text", text);
            controller(SystemMessageControl.class.getName());
            interactOnClick("showMessage()");
        }}.build(nifty, this.hud, systemMessagesQueue);
        image.show();
    }
    
    @Override
    public void update(float tpf) {
        if (systemMessagesQueue != null) {
            for(Element child : systemMessagesQueue.getChildren()) {
                SystemMessageControl control = child.getControl(SystemMessageControl.class);
                if (control != null && System.currentTimeMillis() - control.getCreatedAt() > lifeTime) {
                    child.markForRemoval();
                }
            }
        }
    }

    @Override
    public boolean isPauseable() {
        return true;
    }
}
