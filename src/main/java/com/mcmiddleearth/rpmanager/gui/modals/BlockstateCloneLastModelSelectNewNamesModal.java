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

import com.mcmiddleearth.rpmanager.events.BlockstateCloneNewNamesSelectedEvent;
import com.mcmiddleearth.rpmanager.events.EventDispatcher;
import com.mcmiddleearth.rpmanager.events.EventListener;
import com.mcmiddleearth.rpmanager.gui.actions.Action;
import com.mcmiddleearth.rpmanager.gui.components.FastScrollPane;
import com.mcmiddleearth.rpmanager.gui.components.Form;
import com.mcmiddleearth.rpmanager.gui.components.VerticalBox;
import com.mcmiddleearth.rpmanager.gui.utils.FormButtonEnabledListener;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class BlockstateCloneLastModelSelectNewNamesModal extends JDialog {
    private final String oldName;
    private final List<String> oldTextureNames;
    private final EventDispatcher eventDispatcher = new EventDispatcher();

    public BlockstateCloneLastModelSelectNewNamesModal(Frame parent, String oldName, String newName,
                                                       List<String> oldTextureNames, List<String> newTextureNames,
                                                       Consumer<BlockstateCloneNewNamesSelectedEvent.NewNames> onNewNamesSelected) {
        super(parent, "Select new names", true);
        this.oldName = oldName;
        this.oldTextureNames = oldTextureNames;
        eventDispatcher.addEventListener(event -> onNewNamesSelected.accept(event.getNewNames()),
                BlockstateCloneNewNamesSelectedEvent.class);

        setLayout(new BorderLayout());
        VerticalBox verticalBox = new VerticalBox();
        verticalBox.add(new JLabel("Select new model name (old name: " + oldName + ")"));
        SelectNewNameForm form = new SelectNewNameForm(newName);
        verticalBox.add(form);
        VerticalBox scrollableTexturesBox = new VerticalBox();
        scrollableTexturesBox.setMinimumSize(new Dimension(300, 300));
        List<SelectNewNameForm> textureForms = new ArrayList<>(newTextureNames.size());
        for (int i = 0; i < oldTextureNames.size(); ++i) {
            scrollableTexturesBox.add(new JLabel("Select new texture name (old name: " + oldTextureNames.get(i) + ")"));
            SelectNewNameForm textureForm = new SelectNewNameForm(newTextureNames.get(i));
            scrollableTexturesBox.add(textureForm);
            textureForms.add(textureForm);
        }
        verticalBox.add(new FastScrollPane(scrollableTexturesBox));
        add(verticalBox, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        JButton next = new JButton(new Action("Apply", "Apply changes") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                List<String> textureFormsNewNames = textureForms.stream().map(SelectNewNameForm::getNewName).toList();
                if (validateInputs(form.getNewName(), textureFormsNewNames)) {
                    eventDispatcher.dispatchEvent(new BlockstateCloneNewNamesSelectedEvent(this,
                            new BlockstateCloneNewNamesSelectedEvent.NewNames(
                                    form.getNewName(), textureFormsNewNames)));
                    BlockstateCloneLastModelSelectNewNamesModal.this.close();
                }
            }
        });
        new FormButtonEnabledListener(next.getModel(),
                Stream.concat(form.getDocuments().stream(),
                        textureForms.stream().flatMap(f -> f.getDocuments().stream())).toList());
        buttonsPanel.add(next);
        JButton cancel = new JButton(new Action("Cancel", "Cancel") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                eventDispatcher.dispatchEvent(new BlockstateCloneNewNamesSelectedEvent(this, null));
                BlockstateCloneLastModelSelectNewNamesModal.this.close();
            }
        });
        buttonsPanel.add(cancel);
        add(buttonsPanel, BorderLayout.PAGE_END);

        pack();
        setVisible(true);
    }

    public void close() {
        setVisible(false);
        dispose();
    }

    public void addNewNamesSelectedListener(EventListener<BlockstateCloneNewNamesSelectedEvent> listener) {
        eventDispatcher.addEventListener(listener, BlockstateCloneNewNamesSelectedEvent.class);
    }

    private boolean validateInputs(String newName, List<String> newTextureNames) {
        if (oldName.equals(newName)) {
            JOptionPane.showMessageDialog(this,
                    "New model name has to be different than the old name", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        boolean anyTextureUnchanged = false;
        for (int i = 0; i < oldTextureNames.size(); ++i) {
            if (oldTextureNames.get(i).equals(newTextureNames.get(i))) {
                anyTextureUnchanged = true;
                break;
            }
        }
        if (anyTextureUnchanged) {
            return JOptionPane.showConfirmDialog(this,
                    "Some texture names have not been changed. For each such texture, the existing texture " +
                            "file will be used and no copy will be made. Do you want to continue?",
                    "Texture names not changed", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        }
        return true;
    }

    private static class SelectNewNameForm extends Form {
        private final JTextField newNameInput;

        private SelectNewNameForm(String value) {
            newNameInput = new JTextField(50);
            newNameInput.setText(value);

            addLabel(0, "New name");
            addInput(0, newNameInput);
        }

        public List<Document> getDocuments() {
            return Collections.singletonList(newNameInput.getDocument());
        }

        public String getNewName() {
            return newNameInput.getText();
        }
    }
}
