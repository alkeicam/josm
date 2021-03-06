// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.gui.widgets.JosmComboBox;

/**
 * This is a table cell editor for selecting a possible tag value from a list of
 * proposed tag values. The editor also allows to select all proposed valued or
 * to remove the tag.
 *
 * The editor responds intercepts some keys and interprets them as navigation keys. It
 * forwards navigation events to {@link NavigationListener}s registred with this editor.
 * You should register the parent table using this editor as {@link NavigationListener}.
 *
 * {@link KeyEvent#VK_ENTER} and {@link KeyEvent#VK_TAB} trigger a {@link NavigationListener#gotoNextDecision()}.
 */
public class MultiValueCellEditor extends AbstractCellEditor implements TableCellEditor {

    /**
     * Defines the interface for an object implementing navigation between rows
     */
    public static interface NavigationListener {
        /** Call when need to go to next row */
        void gotoNextDecision();
        /** Call when need to go to previous row */
        void gotoPreviousDecision();
    }

    /** the combo box used as editor */
    private JosmComboBox<Object> editor;
    private DefaultComboBoxModel<Object> editorModel;
    private CopyOnWriteArrayList<NavigationListener> listeners;

    public void addNavigationListeners(NavigationListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public void removeNavigationListeners(NavigationListener listener) {
        listeners.remove(listener);
    }

    protected void fireGotoNextDecision() {
        for (NavigationListener l: listeners) {
            l.gotoNextDecision();
        }
    }

    protected void fireGotoPreviousDecision() {
        for (NavigationListener l: listeners) {
            l.gotoPreviousDecision();
        }
    }

    /**
     * Construct a new {@link MultiValueCellEditor}
     */
    public MultiValueCellEditor() {
        editorModel = new DefaultComboBoxModel<>();
        editor = new JosmComboBox<Object>(editorModel) {
            @Override
            public void processKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    fireGotoNextDecision();
                } else if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (e.isShiftDown()) {
                        fireGotoPreviousDecision();
                    } else {
                        fireGotoNextDecision();
                    }
                } else if ( e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_DELETE  || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    if (editorModel.getIndexOf(MultiValueDecisionType.KEEP_NONE) > 0) {
                        editorModel.setSelectedItem(MultiValueDecisionType.KEEP_NONE);
                        fireGotoNextDecision();
                    }
                } else if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelCellEditing();
                }
                super.processKeyEvent(e);
            }
        };
        editor.addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        editor.showPopup();
                    }
                }
        );
        editor.addItemListener(
                new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        if(e.getStateChange() == ItemEvent.SELECTED)
                            fireEditingStopped();
                    }
                }
        );
        editor.setRenderer(new EditorCellRenderer());
        listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Populate model with possible values for a decision, and select current choice.
     * @param decision The {@link MultiValueResolutionDecision} to proceed
     */
    protected void initEditor(MultiValueResolutionDecision decision) {
        editorModel.removeAllElements();
        if (!decision.isDecided()) {
            editorModel.addElement(MultiValueDecisionType.UNDECIDED);
        }
        for (String value: decision.getValues()) {
            editorModel.addElement(value);
        }
        if (decision.canKeepNone()) {
            editorModel.addElement(MultiValueDecisionType.KEEP_NONE);
        }
        if (decision.canKeepAll()) {
            editorModel.addElement(MultiValueDecisionType.KEEP_ALL);
        }
        switch(decision.getDecisionType()) {
        case UNDECIDED:
            editor.setSelectedItem(MultiValueDecisionType.UNDECIDED);
            break;
        case KEEP_ONE:
            editor.setSelectedItem(decision.getChosenValue());
            break;
        case KEEP_NONE:
            editor.setSelectedItem(MultiValueDecisionType.KEEP_NONE);
            break;
        case KEEP_ALL:
            editor.setSelectedItem(MultiValueDecisionType.KEEP_ALL);
        }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        MultiValueResolutionDecision decision = (MultiValueResolutionDecision)value;
        initEditor(decision);
        editor.requestFocus();
        return editor;
    }

    @Override
    public Object getCellEditorValue() {
        return editor.getSelectedItem();
    }

    /**
     * The cell renderer used in the edit combo box
     *
     */
    private static class EditorCellRenderer extends JLabel implements ListCellRenderer<Object> {

        /**
         * Construct a new {@link EditorCellRenderer}.
         */
        public EditorCellRenderer() {
            setOpaque(true);
        }

        /**
         * Set component color.
         * @param selected true if is selected
         */
        protected void renderColors(boolean selected) {
            if (selected) {
                setForeground(UIManager.getColor("ComboBox.selectionForeground"));
                setBackground(UIManager.getColor("ComboBox.selectionBackground"));
            } else {
                setForeground(UIManager.getColor("ComboBox.foreground"));
                setBackground(UIManager.getColor("ComboBox.background"));
            }
        }

        /**
         * Set text for a value
         * @param value {@link String} or {@link MultiValueDecisionType}
         */
        protected void renderValue(Object value) {
            setFont(UIManager.getFont("ComboBox.font"));
            if (String.class.isInstance(value)) {
                setText(String.class.cast(value));
            } else if (MultiValueDecisionType.class.isInstance(value)) {
                switch(MultiValueDecisionType.class.cast(value)) {
                case UNDECIDED:
                    setText(tr("Choose a value"));
                    setFont(UIManager.getFont("ComboBox.font").deriveFont(Font.ITALIC + Font.BOLD));
                    break;
                case KEEP_NONE:
                    setText(tr("none"));
                    setFont(UIManager.getFont("ComboBox.font").deriveFont(Font.ITALIC + Font.BOLD));
                    break;
                case KEEP_ALL:
                    setText(tr("all"));
                    setFont(UIManager.getFont("ComboBox.font").deriveFont(Font.ITALIC + Font.BOLD));
                    break;
                default:
                    // don't display other values
                }
            }
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            renderColors(isSelected);
            renderValue(value);
            return this;
        }
    }
}
