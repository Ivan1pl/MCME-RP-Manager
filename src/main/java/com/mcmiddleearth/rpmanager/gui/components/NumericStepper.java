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

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.util.function.Consumer;

public class NumericStepper extends JSpinner {
    public NumericStepper(int value, int minValue, int maxValue, Consumer<Integer> setter,
                          Consumer<NumericStepper> onChange) {
        super(new SpinnerNumberModel(value, minValue, maxValue, 1));

        ((JSpinner.DefaultEditor) getEditor()).getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                setter.accept(getValue() == null ? null : ((Number) getValue()).intValue());
                onChange.accept(NumericStepper.this);
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                setter.accept(getValue() == null ? null : ((Number) getValue()).intValue());
                onChange.accept(NumericStepper.this);
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                setter.accept(getValue() == null ? null : ((Number) getValue()).intValue());
                onChange.accept(NumericStepper.this);
            }
        });
    }
}
