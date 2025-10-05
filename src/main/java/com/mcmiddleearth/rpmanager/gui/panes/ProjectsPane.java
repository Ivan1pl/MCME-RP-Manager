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

import com.mcmiddleearth.rpmanager.events.ListItemAddedEvent;
import com.mcmiddleearth.rpmanager.events.ListItemRemovedEvent;
import com.mcmiddleearth.rpmanager.model.project.Layer;
import com.mcmiddleearth.rpmanager.model.project.Project;
import com.mcmiddleearth.rpmanager.model.project.Session;
import com.mcmiddleearth.rpmanager.utils.ActionManager;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

public class ProjectsPane extends JTabbedPane {
    private final Session session;

    public ProjectsPane(Session session) {
        this.session = session;

        session.addProjectAddedListener(this::onProjectAdded);
        session.addProjectRemovedListener(this::onProjectClosed);

        insertTab("+", null, new EmptyProjectPane(), "New/open project", 0);
    }

    @SuppressWarnings("unchecked")
    private void onProjectAdded(ListItemAddedEvent event) {
        Project project = (Project) event.getItem();
        try {
            insertTab(project.getName(), null, new ProjectPane(project), project.getName(), event.getIndex());
            setSelectedIndex(event.getIndex());
        } catch (IOException | GitAPIException e) {
            ((List<Project>) event.getSource()).remove(event.getIndex());
            //TODO error dialog
        }
    }

    private void onProjectClosed(ListItemRemovedEvent event) {
        remove(event.getIndex());
    }

    public Project getCurrentProject() {
        int index = getSelectedIndex();
        return index >= session.getProjects().size() ? null : session.getProjects().get(index);
    }

    public Layer getCurrentLayer() {
        if (getSelectedComponent() instanceof ProjectPane projectPane) {
            return projectPane.getCurrentLayer();
        } else {
            return null;
        }
    }

    public ActionManager getActionManager() {
        if (getSelectedComponent() instanceof ProjectPane p) {
            return p.getActionManager();
        }
        return new ActionManager(() -> {});
    }

    public void reload() {
        if (getSelectedComponent() instanceof ProjectPane p) {
            p.reload();
        }
    }

    public void selectFoundBlockState(String searchString, Object[] path) {
        if (getSelectedComponent() instanceof ProjectPane p) {
            p.selectFoundBlockState(searchString, path);
        }
    }
}
