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

import com.mcmiddleearth.rpmanager.gui.constants.Icons;

import javax.swing.*;
import java.awt.*;

public class CollapsibleSection extends JPanel {
    //TODO replace with nice icons
    private final JButton expandButton;
    private final JLabel title;
    private final JPanel content;
    private final JPanel titlePanel;
    private boolean collapsed;
    private boolean contentPresent = false;

    public CollapsibleSection(String title, JPanel content, boolean collapsed, Component... toolbarContent) {
        this.content = content;
        this.collapsed = collapsed;
        this.setLayout(new BorderLayout());
        this.expandButton = new IconButton(Icons.EXPAND_ICON);
        this.expandButton.addActionListener(actionEvent -> switchState());
        this.title = new JLabel(title);
        this.title.setFont(new Font(this.title.getFont().getName(), Font.BOLD, this.title.getFont().getSize()));

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        for (Component component : toolbarContent) {
            toolbar.add(component);
        }

        this.titlePanel = new JPanel();
        titlePanel.setLayout(new BorderLayout());
        titlePanel.add(expandButton, BorderLayout.LINE_START);
        titlePanel.add(this.title, BorderLayout.CENTER);
        titlePanel.add(toolbar, BorderLayout.LINE_END);
        this.add(titlePanel, BorderLayout.PAGE_START);
        updateState();
    }

    public JPanel getContent() {
        return content;
    }

    private void switchState() {
        collapsed = !collapsed;
        updateState();
    }

    private void updateState() {
        expandButton.setIcon(collapsed ? Icons.EXPAND_ICON : Icons.RETRACT_ICON);
        content.setVisible(!collapsed);
        if (collapsed && contentPresent) {
            this.remove(content);
            contentPresent = false;
        } else if (!collapsed && !contentPresent) {
            this.add(content, BorderLayout.CENTER);
            contentPresent = true;
        }
        revalidate();
        repaint();
        Component parent = getParent();
        if (parent != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    public void setTitle(String title) {
        this.title.setText(title);
        revalidate();
        repaint();
    }
}
