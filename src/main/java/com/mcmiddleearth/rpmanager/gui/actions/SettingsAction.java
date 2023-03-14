/*
 * Copyright (C) 2023 MCME
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

package com.mcmiddleearth.rpmanager.gui.actions;

import com.mcmiddleearth.rpmanager.gui.MainWindow;
import com.mcmiddleearth.rpmanager.gui.modals.SettingsModal;

import java.awt.event.ActionEvent;

public class SettingsAction extends Action {
    public SettingsAction() {
        super("Settings...", "Editor settings");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MainWindow mainWindow = MainWindow.getInstance();
        new SettingsModal(mainWindow, mainWindow.getSettings());
    }
}