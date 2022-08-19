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

package com.mcmiddleearth.rpmanager.gui.modals;

import com.mcmiddleearth.rpmanager.gui.actions.Action;
import com.mcmiddleearth.rpmanager.gui.components.Form;
import com.mcmiddleearth.rpmanager.gui.components.tree.StaticTreeNode;
import com.mcmiddleearth.rpmanager.gui.utils.FormButtonEnabledListener;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

public class MassRenameReplaceModal extends BaseRenameModal {
    public MassRenameReplaceModal(Frame parent, JTree tree, List<StaticTreeNode> nodes) {
        super(parent, "Rename files", tree);

        setLayout(new BorderLayout());
        MassRenameReplaceForm form = new MassRenameReplaceForm();
        add(form, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        JButton rename = new JButton(new com.mcmiddleearth.rpmanager.gui.actions.Action("Rename", "Rename files") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                for (StaticTreeNode node : nodes) {
                    renameNode(node, form.getFrom(), form.getTo());
                }
                MassRenameReplaceModal.this.close();
            }
        });
        new FormButtonEnabledListener(rename.getModel(), form.getDocuments());
        buttonsPanel.add(rename);
        JButton cancel = new JButton(new Action("Cancel", "Cancel renaming files") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                MassRenameReplaceModal.this.close();
            }
        });
        buttonsPanel.add(cancel);
        add(buttonsPanel, BorderLayout.PAGE_END);

        pack();
        setVisible(true);
    }

    private void renameNode(StaticTreeNode node, String from, String to) {
        renameNode(node, node.getName().replace(from, to));
    }

    public void close() {
        setVisible(false);
        dispose();
    }

    private static class MassRenameReplaceForm extends Form {
        private final JTextField fromInput;
        private final JTextField toInput;

        private MassRenameReplaceForm() {
            fromInput = new JTextField(50);
            toInput = new JTextField(50);

            addLabel(0, "Replace");
            addInput(0, fromInput);
            addLabel(1, "With");
            addInput(1, toInput);
        }

        public List<Document> getDocuments() {
            return Arrays.asList(fromInput.getDocument(), toInput.getDocument());
        }

        public String getFrom() {
            return fromInput.getText();
        }

        public String getTo() {
            return toInput.getText();
        }
    }
}