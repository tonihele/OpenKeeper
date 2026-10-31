/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.gui.nifty.flowlayout;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyIdCreator;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.builder.ElementBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.controls.AbstractController;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.SizeValue;
import de.lessvoid.nifty.tools.SizeValueType;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.gui.nifty.PagingButtons;

/**
 * Creates a scrollable area with multiple rows. Has its own scroll controls and
 * streches the contents to fill maximum area. Spans to multiple rows before
 * resorting to scrolling. Works only horizontally.<br>
 * Differs a bit from the original DK 2 component, the scrolling should be more
 * intuitive
 *
 * @author ArchDemon
 */
public final class CustomScroll extends AbstractController {

    private static final int ICON_MARGIN = 4;

    private Nifty nifty;
    private Screen screen;
    private Parameters parameters;

    private Element element;
    private Element content;
    private Element back;
    private Element forward;
    private PagingButtons pagingButtons;

    private final List<Element> elements = new ArrayList<>();
    private final List<Element> rows = new ArrayList<>();
    private int currentRowIndex = 1;
    private int maxRows = 2;

    private boolean visible = true;

    @Override
    public void bind(Nifty nifty, Screen screen, Element element, Parameters parameter) {
        this.nifty = nifty;
        this.screen = screen;
        this.parameters = parameter;
        this.element = element;

        if (this.element.getId() == null) {
            this.element.setId(getClass().getSimpleName() + "-" + NiftyIdCreator.generate());
        }

        maxRows = parameters.getAsInteger("maxRows", maxRows);

        // Get the elements
        content = this.element.findElementById("#content");
        back = this.element.findElementById("#back");
        forward = this.element.findElementById("#forward");
        pagingButtons = new PagingButtons(forward, back);
    }

    @Override
    public void init(Parameters parameter) {
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onFocus(boolean getFocus) {
    }

    @Override
    public boolean inputEvent(NiftyInputEvent inputEvent) {
        return false;
    }

    public void back() {
        scroll(1);
    }

    public void forward() {
        scroll(-1);
    }

    private void scroll(int direction) {
        if (elements.isEmpty() || rows.isEmpty()) {
            return;
        }

        // Scroll both rows by a whole icon column, including its right margin.
        int stepSize = elements.get(0).getWidth() + ICON_MARGIN;
        int widestRow = rows.stream().mapToInt(Element::getWidth).max().orElse(0);
        int overflow = Math.max(0, widestRow - content.getWidth());
        int maxScroll = ((overflow + stepSize - 1) / stepSize) * stepSize;
        int currentX = rows.get(0).getConstraintX().getValueAsInt(1.f);
        int nextX = Math.max(-maxScroll, Math.min(0, currentX + direction * stepSize));
        for (Element row : rows) {
            row.setConstraintX(SizeValue.px(nextX));
        }
        content.layoutElements();
        updatePagingControls();
    }

    public Element addElement(ControlBuilder controlBuilder) {
        Element currentRow = getCurrentRow();
        currentRowIndex++;
        if (rows.size() < currentRowIndex) {
            currentRowIndex = 1;
        }

        Element el = controlBuilder.build(currentRow);
        elements.add(el);

        resizeRowElement(el);
        el.setMarginRight(SizeValue.px(ICON_MARGIN));

        content.layoutElements();

        // Check overflow
        if (currentRow.getWidth() > content.getWidth() && rows.size() < maxRows) {

            // New row and rearrange all items
            createRow();
            rearrangeElements();
        }

        updatePagingControls();

        return el;
    }

    private void resizeRowElement(Element el) {

        // Stretch the element, heightwise and preserve aspect ratio
        int originalHeight = el.getHeight();
        int newHeight = content.getHeight() / rows.size();
        el.setConstraintHeight(new SizeValue(newHeight, SizeValueType.Pixel));
        el.setConstraintWidth(new SizeValue(el.getWidth() * newHeight / originalHeight, SizeValueType.Pixel));
    }

    private void rearrangeElements() {

        // Set the row Y constraints
        int i = 0;
        for (Element row : rows) {
            row.setConstraintY(SizeValue.percent(50 / rows.size() + i * 50));
            if (i != 0) {
                row.setPaddingTop(SizeValue.px(4));
            } else {
                row.setPaddingTop(SizeValue.px(0));
            }
            if (i != rows.size() - 1) {
                row.setPaddingBottom(SizeValue.px(4));
            } else {
                row.setPaddingBottom(SizeValue.px(0));
            }
            i++;
        }

        // Resize all the elements and set to correct rows
        currentRowIndex = 1;
        for (Element element : elements) {
            resizeRowElement(element);

            Element currentRow = getCurrentRow();
            element.markForMove(currentRow, () -> {
                element.setIndex(elements.indexOf(element) / rows.size());
                // Nifty moves elements at the end of the frame. Recalculate
                // widths and positions after the move, not just before it.
                content.layoutElements();
                updatePagingControls();
            });
            currentRowIndex++;
            if (rows.size() < currentRowIndex) {
                currentRowIndex = 1;
            }
        }

        content.layoutElements();
    }

    private Element getCurrentRow() {
        Element currentRow;
        if (rows.size() < currentRowIndex) {
            currentRow = createRow();
            content.layoutElements();
        } else {
            currentRow = rows.get(currentRowIndex - 1);
        }

        return currentRow;
    }

    private Element createRow() {
        Element currentRow = new PanelBuilder("#row-" + currentRowIndex + "-" + NiftyIdCreator.generate())
                .x(new SizeValue(0, SizeValueType.Pixel))
                .y(new SizeValue(50, SizeValueType.Percent))
                .childLayoutHorizontal()
                .valign(ElementBuilder.VAlign.Center)
                .height(new SizeValue(SizeValueType.Wildcard))
                .build(content);
        rows.add(currentRow);

        return currentRow;
    }

    public void removeAll() {
        for (Element child : content.getChildren()) {
            child.markForRemoval();
        }
        elements.clear();
        rows.clear();
        currentRowIndex = 1;
        setEnable(false);
    }

    public void setEnable(boolean enable) {
        pagingButtons.setEnabled(enable);
    }

    private void updatePagingControls() {
        // Native DK II enables both arrows whenever the panel needs paging,
        // including at either limit. Their click handlers clamp the movement.
        boolean canScroll = rows.stream().anyMatch(row -> row.getWidth() > content.getWidth());
        setEnable(canScroll);
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {

            if (visible) {
                back.show();
                forward.show();
            } else {
                back.hide();
                forward.hide();
            }

            this.visible = visible;
        }
    }

    @Override
    public void onEndScreen() {

    }

}
