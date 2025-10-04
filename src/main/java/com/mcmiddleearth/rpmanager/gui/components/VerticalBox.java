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

package com.mcmiddleearth.rpmanager.gui.components;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class VerticalBox extends JPanel {
    private final GridBagConstraints gridBagConstraints;

    public VerticalBox() {
        this(GridBagConstraints.HORIZONTAL);
    }

    public VerticalBox(Insets insets) {
        this(GridBagConstraints.HORIZONTAL, insets);
    }

    public VerticalBox(int fill) {
        this(fill, new Insets(0, 0, 0, 0));
    }

    public VerticalBox(int fill, Insets insets) {
        this.setLayout(new GridBagLayout());
        this.gridBagConstraints = new GridBagConstraints();
        this.gridBagConstraints.fill = fill;
        this.gridBagConstraints.gridx = 0;
        this.gridBagConstraints.gridy = 0;
        this.gridBagConstraints.weighty = 0.0;
        this.gridBagConstraints.weightx = 1.0;
        this.gridBagConstraints.insets = insets;

        addVerticalGlue();
    }

    @Override
    public Component add(Component comp) {
        super.remove(gridBagConstraints.gridy);
        add(comp, gridBagConstraints);
        gridBagConstraints.gridy++;
        addVerticalGlue();
        return comp;
    }

    @Override
    public void remove(int index) {
        List<Component> toMove = new LinkedList<>();
        for (int i = index + 1; i < gridBagConstraints.gridy; ++i) {
            toMove.add(getComponent(i));
        }
        for (int i = gridBagConstraints.gridy; i >= index; --i) {
            super.remove(i);
        }
        gridBagConstraints.gridy = index;
        addVerticalGlue();
        for (Component component : toMove) {
            add(component);
        }
    }

    @Override
    public void removeAll() {
        while (gridBagConstraints.gridy > 0) {
            remove(gridBagConstraints.gridy-1);
        }
    }

    private void addVerticalGlue() {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.gridx = 0;
        c.gridy = gridBagConstraints.gridy;
        c.weightx = 1.0;
        c.weighty = 1.0;
        add(Box.createVerticalGlue(), c);
    }

    public int getChildrenCount() {
        return gridBagConstraints.gridy;
    }
}
