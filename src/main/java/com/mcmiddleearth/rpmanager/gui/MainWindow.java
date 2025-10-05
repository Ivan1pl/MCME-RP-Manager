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

package com.mcmiddleearth.rpmanager.gui;

import com.mcmiddleearth.rpmanager.gui.actions.Action;
import com.mcmiddleearth.rpmanager.gui.actions.Actions;
import com.mcmiddleearth.rpmanager.gui.actions.OpenProjectAction;
import com.mcmiddleearth.rpmanager.gui.panes.ProjectsPane;
import com.mcmiddleearth.rpmanager.model.internal.Settings;
import com.mcmiddleearth.rpmanager.model.project.Layer;
import com.mcmiddleearth.rpmanager.model.project.Project;
import com.mcmiddleearth.rpmanager.model.project.Session;
import com.mcmiddleearth.rpmanager.utils.ActionManager;
import com.mcmiddleearth.rpmanager.utils.GsonProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class MainWindow extends JFrame {
    private static MainWindow INSTANCE;
    private static final File RECENT_PROJECTS_FILE = new File(System.getProperty("user.home"), "mcme-rp-manager-recent.json");
    private final Session session = new Session();
    private final ProjectsPane projectsPane;
    private final Settings settings;
    private final Map<File, String> recentProjects = new LinkedHashMap<>();
    private final JMenu openRecentMenu;

    public MainWindow(Settings settings) {
        loadRecentProjects();
        this.settings = settings;
        this.openRecentMenu = new JMenu("Open recent");
        updateRecentProjects();
        INSTANCE = this;
        createMenu();
        setTitle("MCME Resource Pack Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(this.projectsPane = new ProjectsPane(session), BorderLayout.CENTER);
        setVisible(true);
        updateSettings();
        this.addWindowStateListener(this::onWindowStateChanged);
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        SwingUtilities.invokeLater(this::updateRecentProjects);
    }

    public JMenu getOpenRecentMenu() {
        return openRecentMenu;
    }

    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        fileMenu.add(Actions.NEW_PROJECT);
        fileMenu.add(Actions.OPEN_PROJECT);
        fileMenu.add(openRecentMenu);
        fileMenu.add(Actions.SAVE_PROJECT);
        fileMenu.addSeparator();
        fileMenu.add(Actions.SETTINGS);
        menuBar.add(fileMenu);
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        editMenu.add(Actions.UNDO);
        editMenu.add(Actions.REDO);
        editMenu.addSeparator();
        editMenu.add(Actions.FIND_BLOCK_STATE);
        menuBar.add(editMenu);
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic('T');
        toolsMenu.add(Actions.COMPILE);
        menuBar.add(toolsMenu);

        setJMenuBar(menuBar);
    }

    public Session getSession() {
        return session;
    }

    public Project getCurrentProject() {
        return projectsPane.getCurrentProject();
    }

    public Layer getCurrentLayer() {
        return projectsPane.getCurrentLayer();
    }

    public Settings getSettings() {
        return settings;
    }

    public ActionManager getActionManager() {
        return projectsPane.getActionManager();
    }

    public void updateSettings() {
        try {
            UIManager.setLookAndFeel(Arrays.stream(UIManager.getInstalledLookAndFeels())
                    .filter(lf -> lf.getName().equals(settings.getLookAndFeel()))
                    .findFirst().orElseThrow().getClassName());
            SwingUtilities.updateComponentTreeUI(this);
            pack();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            //TODO show error dialog
            throw new RuntimeException(e);
        }
    }

    public static MainWindow getInstance() {
        return INSTANCE;
    }

    public void updateRecentProjects(Project project) {
        if (project.getProjectFile().exists()) {
            recentProjects.remove(project.getProjectFile());
            recentProjects.put(project.getProjectFile(), project.getName());
            RecentProjects projects = new RecentProjects();
            projects.setProjects(recentProjects.entrySet().stream()
                    .map(e -> new RecentProjects.RecentProject(e.getValue(), e.getKey())).toList());
            try (FileOutputStream outputStream = new FileOutputStream(RECENT_PROJECTS_FILE)) {
                outputStream.write(GsonProvider.getGson().toJson(projects).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                //nop
            }
            updateRecentProjects();
        }
    }

    public void selectFoundBlockState(String searchString, Object[] path) {
        projectsPane.selectFoundBlockState(searchString, path);
    }

    private void loadRecentProjects() {
        try {
            RecentProjects projects =
                    GsonProvider.getGson().fromJson(new FileReader(RECENT_PROJECTS_FILE), RecentProjects.class);
            projects.getProjects().forEach(p -> recentProjects.put(new File(p.getPath()), p.getName()));
        } catch (FileNotFoundException e) {
            //nop
        }
    }

    private void updateRecentProjects() {
        openRecentMenu.removeAll();
        List<JMenuItem> items = new LinkedList<>();
        for (Map.Entry<File, String> recentProject : recentProjects.entrySet()) {
            items.add(0, new JMenuItem(new Action(recentProject.getValue(), recentProject.getValue()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    OpenProjectAction.doOpenProject(recentProject.getKey());
                }
            }));
        }
        items.forEach(openRecentMenu::add);
    }

    public void reload() {
        projectsPane.reload();
    }

    private void onWindowStateChanged(WindowEvent event) {
        if ((event.getNewState() & JFrame.MAXIMIZED_BOTH) != 0) {
            Rectangle oldMaximizedBounds = getMaximizedBounds();
            updateMaximizedBounds();
            Rectangle newMaximizedBounds = getMaximizedBounds();
            if( newMaximizedBounds != null && !newMaximizedBounds.equals( oldMaximizedBounds ) ) {
                int oldExtendedState = getExtendedState();
                setExtendedState(oldExtendedState & ~JFrame.MAXIMIZED_BOTH);
                SwingUtilities.invokeLater(() -> setExtendedState(oldExtendedState));
            }
        }
    }

    private void updateMaximizedBounds() {
        Rectangle oldMaximizedBounds = getMaximizedBounds();
        if (oldMaximizedBounds == null) {
            GraphicsConfiguration gc = getGraphicsConfiguration();
            Rectangle screenBounds = gc.getBounds();

            int maximizedX = screenBounds.x;
            int maximizedY = screenBounds.y;
            int maximizedWidth = screenBounds.width;
            int maximizedHeight = screenBounds.height;

            Insets screenInsets = getToolkit().getScreenInsets(gc);

            Rectangle newMaximizedBounds = new Rectangle(
                    maximizedX + screenInsets.left,
                    maximizedY + screenInsets.top,
                    maximizedWidth - screenInsets.left - screenInsets.right,
                    maximizedHeight - screenInsets.top - screenInsets.bottom);

            setMaximizedBounds(newMaximizedBounds);
        }
    }

    private static class RecentProjects {
        private List<RecentProject> projects;

        public List<RecentProject> getProjects() {
            return projects;
        }

        public void setProjects(List<RecentProject> projects) {
            this.projects = projects;
        }

        private static class RecentProject {
            private String name;
            private String path;

            public RecentProject() {}

            public RecentProject(String name, File file) {
                this.name = name;
                this.path = file.getAbsolutePath();
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }
        }
    }
}
