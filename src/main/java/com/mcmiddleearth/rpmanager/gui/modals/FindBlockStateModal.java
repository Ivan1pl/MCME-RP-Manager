/*
 * Copyright (C) 2025 MCME
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

package com.mcmiddleearth.rpmanager.gui.modals;

import com.mcmiddleearth.rpmanager.gui.MainWindow;
import com.mcmiddleearth.rpmanager.gui.actions.Action;
import com.mcmiddleearth.rpmanager.gui.components.FastScrollPane;
import com.mcmiddleearth.rpmanager.gui.components.IconButton;
import com.mcmiddleearth.rpmanager.gui.components.TextInput;
import com.mcmiddleearth.rpmanager.gui.components.VerticalBox;
import com.mcmiddleearth.rpmanager.gui.constants.Icons;
import com.mcmiddleearth.rpmanager.model.BlockState;
import com.mcmiddleearth.rpmanager.model.internal.LayerRelatedFiles;
import com.mcmiddleearth.rpmanager.model.internal.SelectedFileData;
import com.mcmiddleearth.rpmanager.utils.BlockStateUtils;
import com.mcmiddleearth.rpmanager.utils.GsonProvider;
import com.mcmiddleearth.rpmanager.utils.ResourcePackUtils;
import com.mcmiddleearth.rpmanager.utils.TextAreaUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FindBlockStateModal extends JDialog {
    private final JButton search;
    private final VerticalBox verticalBox;
    private final JTextArea preview;
    private JList<LayerRelatedFiles> layerList;
    private String searchString;

    public FindBlockStateModal(Frame parent) {
        super(parent, "Find block state", true);

        setLayout(new BorderLayout());
        setResizable(false);

        this.verticalBox = new VerticalBox();
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BorderLayout());
        TextInput searchInput = new TextInput("", this::setSearchString, input -> {});
        searchInput.setColumns(30);
        searchInput.requestFocusInWindow();
        searchPanel.add(searchInput, BorderLayout.CENTER);

        Action searchAction = new Action("Search", Icons.SEARCH_ICON, "Search block states") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (searchString != null) {
                        updateLayerList(ResourcePackUtils.getMatchingBlockStates(
                                MainWindow.getInstance().getCurrentProject(), searchString.trim()));
                        revalidate();
                        repaint();
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(MainWindow.getInstance(), "Unknown error", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        searchInput.addActionListener(searchAction);

        this.search = new IconButton(searchAction);
        searchPanel.add(this.search, BorderLayout.LINE_END);
        verticalBox.add(searchPanel);
        add(verticalBox, BorderLayout.CENTER);

        JPanel previewPanel = new JPanel();
        previewPanel.setLayout(new BorderLayout());
        previewPanel.setPreferredSize(new Dimension(300, 0));
        JLabel previewLabel = new JLabel("Preview");
        previewLabel.setHorizontalAlignment(JLabel.CENTER);
        previewPanel.add(previewLabel, BorderLayout.PAGE_START);
        this.preview = new JTextArea();
        preview.setEditable(false);
        previewPanel.add(new FastScrollPane(preview), BorderLayout.CENTER);
        add(previewPanel, BorderLayout.LINE_END);

        JButton cancel = new JButton(new Action("Cancel", "Cancel search") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                FindBlockStateModal.this.close();
            }
        });
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        buttonsPanel.add(cancel);
        add(buttonsPanel, BorderLayout.PAGE_END);

        updateLayerList(null);

        pack();

        SwingUtilities.invokeLater(() -> {
            setLocation(
                    MainWindow.getInstance().getX() + MainWindow.getInstance().getWidth()/2 - getWidth()/2,
                    MainWindow.getInstance().getY() + MainWindow.getInstance().getHeight()/2 - getHeight()/2);
            setVisible(true);
        });
    }

    public void close() {
        setVisible(false);
        dispose();
    }

    private void setSearchString(String searchString) {
        this.searchString = searchString;
        this.search.setEnabled(searchString != null && !searchString.isEmpty());
        invalidate();
        repaint();
    }

    private void updateLayerList(List<LayerRelatedFiles> layers) {
        if (layerList != null) {
            verticalBox.remove(1);
        }
        layerList = new JList<>(layers == null ? new LayerRelatedFiles[0] : layers.toArray(LayerRelatedFiles[]::new));
        layerList.setCellRenderer(new LayerRelatedFilesListCellRenderer());
        layerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        layerList.addListSelectionListener(this::onListSelectionChanged);
        layerList.addMouseListener(new ListMouseAdapter());
        FastScrollPane fastScrollPane = new FastScrollPane(layerList);
        fastScrollPane.setPreferredSize(new Dimension(0, 200));
        verticalBox.add(fastScrollPane);
    }

    @SuppressWarnings("unchecked")
    private void onListSelectionChanged(ListSelectionEvent event) {
        setPreview(event == null ? null : ((JList<LayerRelatedFiles>) event.getSource()).getSelectedValue());
    }

    private void setPreview(LayerRelatedFiles layerRelatedFiles) {
        setPreview(layerRelatedFiles == null ? null : layerRelatedFiles.getRelatedFiles().get(0));
    }

    private void setPreview(SelectedFileData selectedFileData) {
        if (selectedFileData == null) {
            setPreview("", null);
        } else {
            setPreview(GsonProvider.getGson().toJson(selectedFileData.getData()),
                    (BlockState) selectedFileData.getData());
        }
    }

    private void setPreview(String text, BlockState blockState) {
        String previewSelectString = null;
        preview.setText(text);
        if (blockState != null && searchString != null && !searchString.trim().isEmpty()) {
            Map<String, String> matchValues = BlockStateUtils.getVariantValuesBySearchString(searchString.trim());
            if (blockState.getVariants() != null) {
                previewSelectString = blockState.getVariants().keySet().stream()
                        .filter(variant -> BlockStateUtils.matches(variant, matchValues))
                        .findFirst().map(s -> "\"" + s + "\"").orElse(null);
            } else if (blockState.getMultipart() != null) {
                previewSelectString = blockState.getMultipart().stream()
                        .filter(c -> BlockStateUtils.matches(c, matchValues))
                        .findFirst().map(c -> GsonProvider.getGson().toJson(c).replaceAll("\\s+", ""))
                        .orElse(null);
            }
        }
        invalidate();
        repaint();
        if (previewSelectString != null) {
            String selectString = previewSelectString;
            SwingUtilities.invokeLater(() -> TextAreaUtils.scrollTextAreaIgnoreWhitespaces(preview, selectString));
        }
    }

    private static class LayerRelatedFilesListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null) {
                LayerRelatedFiles layerRelatedFiles = (LayerRelatedFiles) value;
                setText(layerRelatedFiles.getLayerName());
                setToolTipText(layerRelatedFiles.getLayerName());
            }
            return this;
        }
    }

    private class ListMouseAdapter extends MouseAdapter {
        @SuppressWarnings("unchecked")
        @Override
        public void mouseClicked(MouseEvent e) {
            JList<LayerRelatedFiles> list = (JList<LayerRelatedFiles>) e.getSource();
            if (e.getClickCount() >= 2 && e.getButton() == MouseEvent.BUTTON1) {
                Rectangle r = list.getCellBounds(0, list.getLastVisibleIndex());
                if (r != null && r.contains(e.getPoint())) {
                    LayerRelatedFiles data = list.getSelectedValue();
                    MainWindow.getInstance().selectFoundBlockState(
                            searchString.trim(), data.getRelatedFiles().get(0).getPath());
                    close();
                }
            }
        }
    }
}
