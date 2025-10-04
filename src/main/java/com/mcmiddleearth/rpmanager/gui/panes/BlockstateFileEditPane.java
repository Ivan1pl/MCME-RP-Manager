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
import com.mcmiddleearth.rpmanager.gui.modals.AddBlockstateModal;
import com.mcmiddleearth.rpmanager.model.BlockState;
import com.mcmiddleearth.rpmanager.model.Case;
import com.mcmiddleearth.rpmanager.model.Model;
import com.mcmiddleearth.rpmanager.model.When;
import com.mcmiddleearth.rpmanager.utils.BlockStateUtils;
import com.mcmiddleearth.rpmanager.utils.GsonProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BlockstateFileEditPane extends VerticalBox {
    private final String fileName;
    private final BlockState blockState;
    private final EventDispatcher eventDispatcher = new EventDispatcher();

    public BlockstateFileEditPane(String fileName, BlockState blockState) {
        this.fileName = fileName;
        this.blockState = blockState;

        if (blockState.getVariants() != null && !blockState.getVariants().isEmpty()) {
            JButton addSection = new JButton(new Action("Add section", "Add new block state section") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    addSection();
                }
            });
            this.add(addSection);
            boolean collapsed = blockState.getVariants().size() != 1;
            for (Map.Entry<String, List<Model>> variant : blockState.getVariants().entrySet()) {
                VariantEditPane variantEditPane = new VariantEditPane(variant.getKey(), variant.getValue());
                variantEditPane.addChangeListener(event -> onChange());
                this.add(new CollapsibleSection(variant.getKey(), variantEditPane, collapsed,
                        removeSectionButton(variant.getKey())));
                this.add(new JSeparator());
            }
        } else if (blockState.getMultipart() != null) {
            JButton addCase = new JButton(new Action("Add case", "Add new case") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    addCase();
                }
            });
            this.add(addCase);
            boolean collapsed = blockState.getMultipart().size() != 1;
            for (Case c : blockState.getMultipart()) {
                CaseEditPane caseEditPane = new CaseEditPane(fileName, c);
                caseEditPane.addChangeListener(event -> onChange());
                this.add(new CollapsibleSection("Case", caseEditPane, collapsed, removeCaseButton(c)));
                this.add(new JSeparator());
            }
        }
    }

    public String scrollToMatchingNodeAndExpand(String searchString) {
        Map<String, String> matchValues = BlockStateUtils.getVariantValuesBySearchString(searchString);
        for (int i = 0; i < getComponentCount(); ++i) {
            if (getComponent(i) instanceof CollapsibleSection collapsibleSection) {
                if (collapsibleSection.getContent() instanceof VariantEditPane variantEditPane) {
                    if (BlockStateUtils.matches(variantEditPane.getVariant(), matchValues)) {
                        scrollToAndExpand(collapsibleSection);
                        return "\"" + variantEditPane.getVariant() + "\"";
                    }
                } else if (collapsibleSection.getContent() instanceof CaseEditPane caseEditPane) {
                    if (BlockStateUtils.matches(caseEditPane.getCase(), matchValues)) {
                        scrollToAndExpand(collapsibleSection);
                        return getPreviewString(caseEditPane.getCase());
                    }
                }
            }
        }
        return null;
    }

    private String getPreviewString(Object o) {
        return GsonProvider.getGson().toJson(o).replaceAll("\\s+", "");
    }

    private void addSection() {
        Map<String, List<String>> possibleStates = BlockStateUtils.getPossibleStates(fileName);
        if (possibleStates == null || possibleStates.isEmpty()) {
            doAddSection("");
        } else {
            new AddBlockstateModal(MainWindow.getInstance(), possibleStates, new Action("Accept", "Accept") {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    doAddSection(actionEvent.getActionCommand());
                }
            });
        }
    }

    private void addCase() {
        Case c = new Case();
        c.setWhen(new When());
        c.setApply(new LinkedList<>());
        blockState.getMultipart().add(c);
        CaseEditPane caseEditPane = new CaseEditPane(fileName, c);
        caseEditPane.addChangeListener(event -> onChange());
        add(new CollapsibleSection("Case", caseEditPane, false, removeCaseButton(c)));
        add(new JSeparator());
        revalidate();
        repaint();
        onChange();
    }

    private void doAddSection(String key) {
        List<Model> models = new LinkedList<>();
        models.add(new Model());
        doAddSection(key, models);
    }

    private void doAddSection(String key, List<Model> models) {
        blockState.getVariants().put(key, models);
        VariantEditPane variantEditPane = new VariantEditPane(key, models);
        variantEditPane.addChangeListener(event -> onChange());
        add(new CollapsibleSection(key, variantEditPane, false, removeSectionButton(key)));
        add(new JSeparator());
        revalidate();
        repaint();
        onChange();
    }

    private JButton removeSectionButton(String key) {
        return new IconButton(new Action("-", Icons.DELETE_ICON, "Remove section") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doRemoveSection(key);
            }
        });
    }

    private JButton removeCaseButton(Case c) {
        return new IconButton(new Action("-", Icons.DELETE_ICON, "Remove case") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doRemoveCase(c);
            }
        });
    }

    private void doRemoveSection(String key) {
        blockState.getVariants().remove(key);
        for (int i = getComponentCount() - 1; i >= 0; --i) {
            Component component = getComponent(i);
            if (component instanceof CollapsibleSection section &&
                    section.getContent() instanceof VariantEditPane variantEditPane &&
                    variantEditPane.getVariant().equals(key)) {
                remove(i+1);
                remove(i);
            }
        }
        revalidate();
        repaint();
        onChange();
    }

    private void doRemoveCase(Case c) {
        blockState.getMultipart().remove(c);
        for (int i = getComponentCount() - 1; i >= 0; --i) {
            Component component = getComponent(i);
            if (component instanceof CollapsibleSection section &&
                    section.getContent() instanceof CaseEditPane caseEditPane && caseEditPane.getCase() == c) {
                remove(i+1);
                remove(i);
            }
        }
        revalidate();
        repaint();
        onChange();
    }

    private void onChange() {
        eventDispatcher.dispatchEvent(new ChangeEvent(this, blockState));
    }

    public void addChangeListener(EventListener<ChangeEvent> listener) {
        eventDispatcher.addEventListener(listener, ChangeEvent.class);
    }

    private void scrollToAndExpand(CollapsibleSection collapsibleSection) {
        collapsibleSection.setCollapsed(false);
        SwingUtilities.invokeLater(() -> {
            scrollRectToVisible(collapsibleSection.getBounds());
        });
    }
}
