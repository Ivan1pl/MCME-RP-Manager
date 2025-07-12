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

import com.mcmiddleearth.rpmanager.events.ComponentSizeUpdatedEvent;
import com.mcmiddleearth.rpmanager.events.ListItemAddedEvent;
import com.mcmiddleearth.rpmanager.events.ListItemRemovedEvent;
import com.mcmiddleearth.rpmanager.gui.actions.Action;
import com.mcmiddleearth.rpmanager.gui.components.IconButton;
import com.mcmiddleearth.rpmanager.gui.components.tree.StaticTreeNode;
import com.mcmiddleearth.rpmanager.gui.constants.Icons;
import com.mcmiddleearth.rpmanager.gui.listeners.LayerTreeSelectionListener;
import com.mcmiddleearth.rpmanager.model.project.Layer;
import com.mcmiddleearth.rpmanager.model.project.Project;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ProjectFilesPane extends JPanel {
    private final JFileChooser fileChooser = new JFileChooser();
    private final Project project;
    private final List<LayerFilesPane> layerFilesPanes = new LinkedList<>();
    private final List<LayerTreeSelectionListener> treeSelectionListeners = new LinkedList<>();

    public ProjectFilesPane(Project project) throws IOException, GitAPIException {
        this.project = project;

        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Resource pack metadata file", "mcmeta"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileHidingEnabled(false);

        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setAlignmentY(TOP_ALIGNMENT);

        for (Layer layer : project.getLayers()) {
            LayerFilesPane layerFilesPane = new LayerFilesPane(layer, project);
            layerFilesPane.setAlignmentY(TOP_ALIGNMENT);
            layerFilesPane.addComponentSizeUpdatedListener(this::onLayerCollapsedOrExpanded);
            layerFilesPanes.add(layerFilesPane);
            layerFilesPane.addTreeSelectionListener(this::onFileSelectionChanged);
        }

        JButton addLayerButton = new IconButton(new Action("+", Icons.ADD_ICON, "Add resource pack layer") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (fileChooser.showOpenDialog(ProjectFilesPane.this) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    project.addLayer(file.getParentFile().getName(), file);
                }
            }
        });
        addLayerButton.setAlignmentY(TOP_ALIGNMENT);
        JPanel addLayerPanel = new JPanel();
        addLayerPanel.setLayout(new BorderLayout());
        addLayerPanel.add(addLayerButton, BorderLayout.PAGE_START);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                layerFilesPanes.get(layerFilesPanes.size() - 1), addLayerPanel);
        splitPane.setDividerSize(10);
        splitPane.setOneTouchExpandable(false);
        splitPane.setResizeWeight(1.0);
        splitPane.setEnabled(false);
        for (int i = layerFilesPanes.size() - 2; i >= 0; --i) {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                    layerFilesPanes.get(i), splitPane);
            splitPane.setDividerSize(10);
            splitPane.setOneTouchExpandable(false);
            splitPane.setResizeWeight(1.0 / (double) (layerFilesPanes.size() - i));
        }

        add(splitPane);

        project.addLayerAddedListener(this::onLayerAdded);
        project.addLayerRemovedListener(this::onLayerRemoved);
    }

    @SuppressWarnings("unchecked")
    private void onLayerAdded(ListItemAddedEvent event) {
        try {
            LayerFilesPane layerFilesPane = new LayerFilesPane((Layer) event.getItem(), project);
            layerFilesPane.setAlignmentY(TOP_ALIGNMENT);
            layerFilesPane.addComponentSizeUpdatedListener(this::onLayerCollapsedOrExpanded);
            // event.getIndex() should always be greater than 0 - it is not possible to add before vanilla pack.
            JSplitPane mainSplitPane = (JSplitPane) layerFilesPanes.get(event.getIndex() - 1).getParent();
            JSplitPane newInnerPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                    layerFilesPane, mainSplitPane.getRightComponent());
            newInnerPane.setDividerSize(10);
            newInnerPane.setOneTouchExpandable(false);
            newInnerPane.setResizeWeight(mainSplitPane.getResizeWeight());
            newInnerPane.setEnabled(mainSplitPane.isEnabled());
            mainSplitPane.setRightComponent(newInnerPane);
            mainSplitPane.setEnabled(true);
            layerFilesPanes.add(event.getIndex(), layerFilesPane);
            layerFilesPane.addTreeSelectionListener(this::onFileSelectionChanged);
            revalidate();
            repaint();
        } catch (IOException | GitAPIException e) {
            //TODO error dialog
            ((List<Layer>) event.getSource()).remove(event.getIndex());
        }
    }

    private void onLayerRemoved(ListItemRemovedEvent event) {
        if (event.getIndex() == layerFilesPanes.size() - 1) {
            // event.getIndex() should always be greater than 0 - it is not possible to remove vanilla pack.
            JSplitPane mainSplitPane = (JSplitPane) layerFilesPanes.get(event.getIndex() - 1).getParent();
            JSplitPane childPane = (JSplitPane) mainSplitPane.getRightComponent();
            mainSplitPane.setRightComponent(childPane.getRightComponent());
            mainSplitPane.setEnabled(childPane.isEnabled());
            mainSplitPane.setResizeWeight(childPane.getResizeWeight());
        } else {
            // Removing from the middle.
            JSplitPane mainSplitPane = (JSplitPane) layerFilesPanes.get(event.getIndex()).getParent();
            JSplitPane childPane = (JSplitPane) mainSplitPane.getRightComponent();
            mainSplitPane.setLeftComponent(childPane.getLeftComponent());
            mainSplitPane.setRightComponent(childPane.getRightComponent());
            mainSplitPane.setEnabled(childPane.isEnabled());
            mainSplitPane.setResizeWeight(childPane.getResizeWeight());
        }
        layerFilesPanes.remove(event.getIndex());
        revalidate();
        repaint();
    }

    private void onLayerCollapsedOrExpanded(ComponentSizeUpdatedEvent event) {
        int index = layerFilesPanes.indexOf(event.getSource());
        JSplitPane mainSplitPane = (JSplitPane) layerFilesPanes.get(index).getParent();
        if (mainSplitPane.isEnabled()) {
            mainSplitPane.setEnabled(false);
            mainSplitPane.setDividerLocation(mainSplitPane.getMinimumDividerLocation());
        } else {
            mainSplitPane.setEnabled(index != layerFilesPanes.size() - 1);
            mainSplitPane.setDividerLocation(index == layerFilesPanes.size() - 1 ?
                    mainSplitPane.getMaximumDividerLocation() : mainSplitPane.getMinimumDividerLocation());
        }
        SwingUtilities.invokeLater(() -> updateDividerLocationForCollapsedTrees(0));
        revalidate();
        repaint();
    }

    private void updateDividerLocationForCollapsedTrees(int index) {
        if (index < layerFilesPanes.size() - 1) {
            if (!layerFilesPanes.get(index).isExpanded()) {
                JSplitPane splitPane = (JSplitPane) layerFilesPanes.get(index).getParent();
                splitPane.setDividerLocation(splitPane.getMinimumDividerLocation());
            }
            SwingUtilities.invokeLater(() -> updateDividerLocationForCollapsedTrees(index + 1));
        }
    }

    private void onFileSelectionChanged(Layer layer, JTree tree, TreeSelectionEvent event) {
        for (LayerFilesPane pane : layerFilesPanes) {
            if (pane.getLayer() != layer) {
                pane.suppressEvents(pane::clearSelection);
            }
        }
        for (LayerTreeSelectionListener listener : treeSelectionListeners) {
            listener.valueChanged(layer, tree, event);
        }
    }

    public void addTreeSelectionListener(LayerTreeSelectionListener listener) {
        this.treeSelectionListeners.add(listener);
    }

    public void reload() {
        layerFilesPanes.forEach(LayerFilesPane::reload);
    }

    public void setSelectedNode(Object[] path) {
        for (LayerFilesPane layerFilesPane : layerFilesPanes) {
            StaticTreeNode staticTreeNode = layerFilesPane.findNode(path);
            if (staticTreeNode != null) {
                layerFilesPane.setSelectedNode(staticTreeNode);
                break;
            }
        }
    }
}
