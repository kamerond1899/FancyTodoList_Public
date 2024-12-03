package fancytodoapp;

import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Kam
 * 
 *  TaskTableModel
 * 
 *  The to-do list table model. Contains the status(checked or unchecked) and the task string
 * 
 */
public class TaskTableModel extends AbstractTableModel {
    private List<Task> tasks;
    private String[] columnNames = {"Status", "Task"};

    public TaskTableModel(List<Task> tasks) {
        this.tasks = tasks;
    }

    public void addTask(Task task) {
        getTasks().add(task);
        fireTableRowsInserted(getTasks().size() - 1, getTasks().size() - 1);
    }

    public void removeTask(int index) {
        getTasks().remove(index);
        fireTableRowsDeleted(index, index);
    }

    public Task getTask(int index) {
        return getTasks().get(index);
    }

    @Override
    public int getRowCount() {
        return getTasks().size();
    }

    @Override
    public int getColumnCount() {
        return getColumnNames().length; // 2 columns: Status and Task
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Task task = getTasks().get(rowIndex);
        if (columnIndex == 0) {
            return task.status;
        } else if (columnIndex == 1) {
            return task.task;
        }
        return null;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Task task = getTasks().get(rowIndex);
        if (columnIndex == 0) {
            task.status = (Boolean) value;
        } else if (columnIndex == 1) {
            task.task = (String) value;
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    @Override
    public String getColumnName(int column) {
        return getColumnNames()[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true; // Both columns are editable
    }

    public List<Task> getTasks() {
        return tasks;
    }

    /**
     * @param tasks the tasks to set
     */
    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    /**
     * @return the columnNames
     */
    public String[] getColumnNames() {
        return columnNames;
    }

    /**
     * @param columnNames the columnNames to set
     */
    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }
}
