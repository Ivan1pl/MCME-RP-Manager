/*
 * Copyright (C) 2022 MCME
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mcmiddleearth.rpmanager.gui.panes;

import com.mcmiddleearth.rpmanager.gui.actions.Action;
import com.mcmiddleearth.rpmanager.gui.components.CollapsibleSection;
import com.mcmiddleearth.rpmanager.gui.components.VerticalBox;
import com.mcmiddleearth.rpmanager.gui.components.renderers.EnumListCellRenderer;
import com.mcmiddleearth.rpmanager.model.GuiLight;
import com.mcmiddleearth.rpmanager.model.ItemModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ItemModelFileEditPane extends BaseModelFileEditPane {
    private final String fileName;

    public ItemModelFileEditPane(String fileName, ItemModel itemModel) {
        super(itemModel);
        this.fileName = fileName;

        this.add(parentPanel);

        JPanel guiLightPanel = new JPanel();
        guiLightPanel.setLayout(new BoxLayout(guiLightPanel, BoxLayout.X_AXIS));
        guiLightPanel.add(new JLabel("GUI light: "));
        JComboBox<GuiLight> guiLightInput = new JComboBox<>(GuiLight.values());
        guiLightInput.setRenderer(new EnumListCellRenderer<GuiLight>());
        guiLightInput.setSelectedItem(itemModel.getGuiLight());
        guiLightInput.addItemListener(e -> {
            itemModel.setGuiLight(
                    guiLightInput.getSelectedItem() == null || guiLightInput.getSelectedItem() == GuiLight.SIDE ?
                            null : (GuiLight) guiLightInput.getSelectedItem());
            onChange();
        });
        guiLightPanel.add(guiLightInput);
        this.add(guiLightPanel);

        ItemModelOverridesEditPane overridesEditPane = new ItemModelOverridesEditPane(itemModel);
        overridesEditPane.addChangeListener(e -> onChange());
        CollapsibleSection overridesEditSection =
                new CollapsibleSection("overrides: {", overridesEditPane, false,
                        new JButton(new Action("+", "Add override entry") {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                overridesEditPane.addOverrideEntry();
                            }
                        }));
        VerticalBox overridesBox = new VerticalBox();
        overridesBox.add(overridesEditSection);
        overridesBox.add(new JLabel("}"));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Display", displayPanel);
        tabbedPane.addTab("Textures", texturesPanel);
        tabbedPane.addTab("Elements", elementsPanel);
        tabbedPane.addTab("Overrides", overridesBox);
        tabbedPane.setSelectedIndex(1);
        this.add(tabbedPane);
    }
}
