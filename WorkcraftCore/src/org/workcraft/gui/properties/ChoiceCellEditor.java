package org.workcraft.gui.properties;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

@SuppressWarnings("serial")
public class ChoiceCellEditor extends AbstractCellEditor implements TableCellEditor, ItemListener {

    private final FlatComboBox comboBox;
    private final ChoiceWrapper[] wrappers;

    public ChoiceCellEditor(PropertyDescriptor descriptor) {
        comboBox = new FlatComboBox();
        comboBox.setFocusable(false);
        comboBox.addItemListener(this);

        Map<? extends Object, String> choice = descriptor.getChoice();
        int choiceCount = choice.size();
        wrappers = new ChoiceWrapper[choiceCount];
        int index = 0;
        for (Object object: choice.keySet()) {
            String text = choice.get(object);
            if (text != null) {
                wrappers[index] = new ChoiceWrapper(text, object);
                comboBox.addItem(wrappers[index]);
            }
            index++;
        }
    }

    @Override
    public Object getCellEditorValue() {
        Object selectedItem = comboBox.getSelectedItem();
        if (selectedItem instanceof ChoiceWrapper) {
            return ((ChoiceWrapper) selectedItem).value;
        }
        return null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        comboBox.setOpaque(value == null);
        comboBox.setFont(table.getFont());
        // First select non-existent item, then select a "correct" item.
        comboBox.setSelectedItem(null);
        for (ChoiceWrapper wrapper: wrappers) {
            if (wrapper.text.equals(value)) {
                comboBox.setSelectedItem(wrapper);
                break;
            }
        }
        return comboBox;
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        fireEditingStopped();
    }

}

