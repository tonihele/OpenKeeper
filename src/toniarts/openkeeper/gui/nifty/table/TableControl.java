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
package toniarts.openkeeper.gui.nifty.table;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.EffectBuilder;
import de.lessvoid.nifty.builder.HoverEffectBuilder;
import de.lessvoid.nifty.builder.PanelBuilder;
import de.lessvoid.nifty.builder.TextBuilder;
import de.lessvoid.nifty.controls.label.builder.LabelBuilder;
import de.lessvoid.nifty.controls.listbox.ListBoxControl;
import de.lessvoid.nifty.controls.listbox.ListBoxItemController;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.mapping.MenuInputMapping;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.Color;
import de.lessvoid.xml.xpp3.Attributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Table control, build on top of the Nifty ListBox<br/>
 * With help from:
 * https://github.com/void256/nifty-gui/tree/1.3/nifty-examples/src/main/java/de/lessvoid/nifty/examples/table
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TableControl<T> extends ListBoxControl<T> {

    private List<TableColumn> tableColumns;
    private Element headers;
    private Element childRootElement;

    @Override
    public void bind(Nifty niftyParam, Screen screenParam, Element elementParam, Properties parameterParam, Attributes controlDefinitionAttributes) {

        // Set some things
        parameterParam.setProperty("horizontal", "off");
        parameterParam.setProperty("viewConverterClass", TableRowViewConverter.class.getName());

        // The cols/rows
        int cols = Integer.valueOf(parameterParam.getProperty("colCount"));
        tableColumns = new ArrayList<>(cols);
        for (int i = 0; i < cols; i++) {
            try {
                tableColumns.add(TableColumn.parse(parameterParam.getProperty("col" + i)));
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(TableControl.class.getName()).log(Level.SEVERE, "Failed to init a column!", ex);
            }
        }

        // Create the headers
        headers = elementParam.findElementByName("#headers");
        new PanelBuilder() {
            {
                height("30px"); // TODO: We should calculate by the font
                childLayoutHorizontal();
                width("100%");
                alignCenter();
                marginBottom("2px");

                // Set the columns
                int i = 0;
                for (final TableColumn col : tableColumns) {
                    createColumn(i, col);
                    i++;
                }
            }

            private void createColumn(final int i, final TableColumn col) {
                panel(new PanelBuilder("#headerColPanel-" + i) {
                    {
                        width(col.getWidth() + "%");
                        height("100%");
                        textVAlignCenter();
                        childLayoutCenter();
                        backgroundColor(new Color("#80808030"));
                        marginRight("2px");

                        // The contents
                        if (col.getType() == String.class) {
                            control(new LabelBuilder("#headerCol-" + i, col.getHeader()) {
                                {
                                    style("menuTextSmall");
                                }
                            });
                        }
                    }
                });
            }
        }.build(niftyParam, screenParam, headers);

        // Create the row control
        childRootElement = elementParam.findElementByName("#child-root");
        new PanelBuilder("#row") {
            {
                height("32px"); // TODO: We should calculate by the font, +2 from the padding
                childLayoutHorizontal();
                width("100%");
                alignCenter();
                paddingBottom("2px");

                // Set the columns
                int i = 0;
                for (final TableColumn col : tableColumns) {
                    createColumn(i, col);
                    i++;
                }
                visibleToMouse();
                controller(ListBoxItemController.class.getName());
                inputMapping(MenuInputMapping.class.getName());
                onHoverEffect(new HoverEffectBuilder("colorBar") {
                    {
                        effectParameter("color", "#fff2");
                        post(true);
                        inset("1px");
                        neverStopRendering(true);
                        effectParameter("timeType", "infinite");
                    }
                });
                onCustomEffect(new EffectBuilder("colorBar") {
                    {
                        customKey("focus");
                        post(false);
                        effectParameter("color", "#fff3");
                        neverStopRendering(true);
                        effectParameter("timeType", "infinite");
                    }
                });
                onCustomEffect(new EffectBuilder("colorBar") {
                    {
                        customKey("select");
                        post(false);
                        effectParameter("color", "#fff3");
                        neverStopRendering(true);
                        effectParameter("timeType", "infinite");
                    }
                });
                onClickEffect(new EffectBuilder("focus") {
                    {
                        effectParameter("targetElement", "#parent#parent");
                    }
                });
                interactOnClick("listBoxItemClicked()");
            }

            private void createColumn(final int i, final TableColumn col) {
                panel(new PanelBuilder("#colPanel-" + i) {
                    {
                        width(col.getWidth() + "%");
                        height("100%");
                        textVAlignCenter();
                        childLayoutCenter();
                        backgroundColor(col.getColor());
                        marginRight("2px");

                        // The contents
                        if (col.getType() == String.class) {
                            text(new TextBuilder("#col-" + i) {
                                {
                                    style("menuTextSmall");
                                }
                            });
                        }
                    }
                });
            }
        }.build(niftyParam, screenParam, childRootElement);

        // Call the super
        super.bind(niftyParam, screenParam, elementParam, parameterParam, controlDefinitionAttributes);
    }

    @Override
    public void ensureWidthConstraints() {
        super.ensureWidthConstraints();

        // Adjust our headers
        headers.setConstraintWidth(childRootElement.getConstraintWidth());
    }
}
