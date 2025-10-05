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

import com.mcmiddleearth.rpmanager.events.ChangeEvent;
import com.mcmiddleearth.rpmanager.events.EventDispatcher;
import com.mcmiddleearth.rpmanager.events.EventListener;
import com.mcmiddleearth.rpmanager.gui.MainWindow;
import com.mcmiddleearth.rpmanager.gui.actions.Action;
import com.mcmiddleearth.rpmanager.gui.components.CollapsibleSection;
import com.mcmiddleearth.rpmanager.gui.components.IconButton;
import com.mcmiddleearth.rpmanager.gui.components.VerticalBox;
import com.mcmiddleearth.rpmanager.gui.constants.Icons;
import com.mcmiddleearth.rpmanager.model.Model;
import com.mcmiddleearth.rpmanager.process.BlockstateCloneLastModelProcess;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class VariantEditPane extends VerticalBox {
    private final String variant;
    private final List<Model> models;
    private final EventDispatcher eventDispatcher = new EventDispatcher();

    public VariantEditPane(String variant, List<Model> models) {
        this.variant = variant;
        this.models = models;

        setBorder(new EmptyBorder(0, 10, 0, 0));

        this.add(new JButton(new Action("Add model", "Add new model to this variant") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                addModel();
            }
        }));

        this.add(new JButton(new Action("Clone last model", "Clone last model and add it to this variant") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                cloneModel();
            }
        }));

        boolean collapsed = models.size() != 1;
        int label = 1;
        for (Model model : models) {
            VariantModelEditPane variantModelEditPane = new VariantModelEditPane(model);
            variantModelEditPane.addChangeListener(this::onChange);
            this.add(new CollapsibleSection(
                    Integer.toString(label++), variantModelEditPane, collapsed, removeModelButton(model)));
            this.add(new JSeparator());
        }
    }

    public String getVariant() {
        return variant;
    }

    private void onChange(ChangeEvent changeEvent) {
        eventDispatcher.dispatchEvent(changeEvent);
    }

    public void addChangeListener(EventListener<ChangeEvent> listener) {
        eventDispatcher.addEventListener(listener, ChangeEvent.class);
    }

    private JButton removeModelButton(Model model) {
        return new IconButton(new Action("-", Icons.DELETE_ICON, "Remove model") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                removeModel(model);
            }
        });
    }

    private void addModel() {
        Model model = new Model();
        models.add(model);

        VariantModelEditPane variantModelEditPane = new VariantModelEditPane(model);
        variantModelEditPane.addChangeListener(this::onChange);
        this.add(new CollapsibleSection(
                Integer.toString(models.size()), variantModelEditPane, false, removeModelButton(model)));
        this.add(new JSeparator());

        revalidate();
        repaint();

        eventDispatcher.dispatchEvent(new ChangeEvent(this, model));
    }

    private void cloneModel() {
        Model lastModel = models.isEmpty() ? null : models.get(models.size() - 1);
        String oldName = lastModel == null ? null : lastModel.getModel();
        if (oldName == null) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(),
                    "Last defined model is incomplete and cannot be cloned",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            try {
                new BlockstateCloneLastModelProcess(result -> cloneModel(result), models, lastModel,
                        MainWindow.getInstance().getCurrentLayer().getName()).start();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(MainWindow.getInstance(),
                        "Unknown error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cloneModel(BlockstateCloneLastModelProcess.Result cloneProcessResult) {
        if (cloneProcessResult == null) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(),
                    "Clone operation failed", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            MainWindow.getInstance().getActionManager().submit(
                    cloneProcessResult.getUndoAction(), cloneProcessResult.getRedoAction());

            Model model = models.get(models.size() - 1);
            VariantModelEditPane variantModelEditPane = new VariantModelEditPane(model);
            variantModelEditPane.addChangeListener(this::onChange);
            this.add(new CollapsibleSection(
                    Integer.toString(models.size()), variantModelEditPane, false, removeModelButton(model)));
            this.add(new JSeparator());

            revalidate();
            repaint();

            eventDispatcher.dispatchEvent(new ChangeEvent(this, model));

            MainWindow.getInstance().getActionManager().refresh();
        }
    }

    private void removeModel(Model model) {
        for (int i = getComponentCount() - 1; i >= 0; --i) {
            if (getComponent(i) instanceof CollapsibleSection section &&
                    section.getContent() instanceof VariantModelEditPane variantModelEditPane &&
                    variantModelEditPane.getModel() == model) {
                remove(i+1);
                remove(i);
            }
        }

        models.remove(model);
        updateLabels();

        revalidate();
        repaint();

        eventDispatcher.dispatchEvent(new ChangeEvent(this, model));
    }

    private void updateLabels() {
        int label = 1;
        for (int i = 0; i < getComponentCount(); ++i) {
            if (getComponent(i) instanceof CollapsibleSection section) {
                section.setTitle(Integer.toString(label++));
            }
        }
    }
}
