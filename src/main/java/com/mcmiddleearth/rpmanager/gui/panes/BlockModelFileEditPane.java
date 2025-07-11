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

import com.mcmiddleearth.rpmanager.gui.components.FastScrollPane;
import com.mcmiddleearth.rpmanager.model.BlockModel;

import javax.swing.*;

public class BlockModelFileEditPane extends BaseModelFileEditPane {
    private final String fileName;

    public BlockModelFileEditPane(String fileName, BlockModel blockModel) {
        super(blockModel);
        this.fileName = fileName;

        this.add(parentPanel);

        JPanel ambientOcclusionPanel = new JPanel();
        ambientOcclusionPanel.setLayout(new BoxLayout(ambientOcclusionPanel, BoxLayout.X_AXIS));
        ambientOcclusionPanel.add(new JLabel("Ambient occlusion: "));
        JComboBox<Boolean> ambientOcclusionInput = new JComboBox<>(new Boolean[] { null, false, true });
        ambientOcclusionInput.setSelectedItem(blockModel.getAmbientocclusion());
        ambientOcclusionInput.addItemListener(event -> {
            blockModel.setAmbientocclusion((Boolean) ambientOcclusionInput.getSelectedItem());
            onChange();
        });
        ambientOcclusionPanel.add(ambientOcclusionInput);
        this.add(ambientOcclusionPanel);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Display", displayPanel);
        tabbedPane.addTab("Textures", texturesPanel);
        tabbedPane.addTab("Elements", elementsPanel);
        tabbedPane.setSelectedIndex(1);
        this.add(tabbedPane);
    }
}
