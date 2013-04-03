package au.com.jwatmuff.eventmanager.util.gui;

import java.awt.Component;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author James
 *
 * Copied from http://www.exampledepot.com/egs/javax.swing.table/ComboBox.html
 */
public class ComboBoxCellRenderer extends JComboBox<Object> implements TableCellRenderer {

    public ComboBoxCellRenderer(Object[] items) {
        super(items);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }

        // Select the current value
        setSelectedItem(value);
        return this;
    }
}
