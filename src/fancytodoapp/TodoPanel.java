package fancytodoapp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fancytodoapp.FancyToDoApp.DPIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.List;
import javax.swing.table.TableCellEditor;
import org.imgscalr.Scalr;

/**
 *
 * @author Kam
 * 
 *  TodoPanel
 * 
 *  Class which contains most of the functionality of the to-do list itself, the model, 
 *  custom checkbox functionality, and the Gson Persistence Handler.
 * 
 */

public class TodoPanel extends JPanel {

    private DefaultTableModel model;
    private JTable todoTable;
    private final List<Task> tasks; // List to store tasks
    private final GsonPersistenceHandler persistenceHandler;

    // Icons and colors
    private ImageIcon iconCheckBlack;
    private ImageIcon iconUncheckBlack;
    private ImageIcon iconCheckWhite;
    private ImageIcon iconUncheckWhite;
    private ImageIcon originalIconChecked;
    private ImageIcon originalIconUnchecked;
    private final Map<Color, ImageIcon[]> checkboxIconCache = new HashMap<>();

    private Color currentThemeColor;
    private Color checkboxColor = null; // User-selected checkbox color
    private boolean useCustomCheckboxes = true; // Default to using custom checkboxes
    // Icon cache
    private final Map<String, ImageIcon> iconCache = new HashMap<>();

    public TodoPanel(String tabName, Color themeColor) {
        
        this.tasks = new ArrayList<>();

        setLayout(new BorderLayout());

        // Initialize currentThemeColor
        if (themeColor != null) {
            this.currentThemeColor = themeColor;
        } else {
            this.currentThemeColor = UIManager.getColor("Panel.background");
            if (this.currentThemeColor == null) {
                this.currentThemeColor = Color.WHITE; // Default color
            }
        }

        // Initialize icons
        loadIcons();

        // Initialize persistence handler
        persistenceHandler = new GsonPersistenceHandler(tabName);

        // Load existing tasks
        loadTasks(); // Populates the 'tasks' list

        // Initialize table model
        initializeTableModel();

        // Initialize table
        initializeTable();

        // Apply theme color
        applyThemeColor(currentThemeColor);
    }

    public void setUseCustomCheckboxes(boolean useCustom) {
        this.useCustomCheckboxes = useCustom;
        updateCheckboxRendererAndEditor();
    }

    private void initializeTableModel() {
        // Initialize table model with column names
        model = new DefaultTableModel(new Object[]{"Status", "Task"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return !isPlusRow(row) && (column == 0 || column == 1);
            }
        };

        // Populate the model with tasks
        for (Task task : tasks) {
            model.addRow(new Object[]{task.status, task.task});
        }
    }

    private void initializeTable() {
        todoTable = new JTable(model) {
            @Override
            public int getRowCount() {
                return model.getRowCount() + 1; // Add one extra row for the "+"
            }

            @Override
            public Object getValueAt(int row, int column) {
                if (isPlusRow(row)) {
                    return null; // Return null for the "+" row
                }
                return model.getValueAt(row, column);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                if (isPlusRow(row)) {
                    return false;
                }
                return model.isCellEditable(row, column);
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                if (!isPlusRow(row)) {
                    model.setValueAt(aValue, row, column);
                    // Update the task in the tasks list
                    Task task = tasks.get(row);
                    if (column == 0) {
                        task.status = (Boolean) aValue;
                    } else if (column == 1) {
                        task.task = (String) aValue;
                    }
                    saveTasks();
                }
            }

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                if (isPlusRow(row)) {
                    JLabel plusLabel = new JLabel("+");
                    plusLabel.setHorizontalAlignment(JLabel.CENTER);
                    plusLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
                    plusLabel.setOpaque(true);
                    plusLabel.setBackground(getBackground());
                    plusLabel.setForeground(getForeground());
                    return plusLabel;
                } else {
                    return super.prepareRenderer(renderer, row, column);
                }
            }
        };

        todoTable.setFillsViewportHeight(true);
        todoTable.setRowHeight(32);
        todoTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Set column widths
        todoTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        todoTable.getColumnModel().getColumn(1).setPreferredWidth(300);

        // Set custom renderer and editor for checkbox
        updateCheckboxRendererAndEditor();



        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(todoTable);
        add(scrollPane, BorderLayout.CENTER);

        // Add mouse listener for clicks and context menu
        addTableMouseListener();
    }

    // Method to load icons
    private void loadIcons() {
        // Load black icons
        iconCheckBlack = loadAndScaleIcon(getClass().getResource("/icons/iconCheck_b.png"));
        iconUncheckBlack = loadAndScaleIcon(getClass().getResource("/icons/iconUncheck_b.png"));

        // Load white icons
        iconCheckWhite = loadAndScaleIcon(getClass().getResource("/icons/iconCheck_w.png"));
        iconUncheckWhite = loadAndScaleIcon(getClass().getResource("/icons/iconUncheck_w.png"));

        // Set original icons based on theme
        if (isColorDark(currentThemeColor)) {
            originalIconChecked = iconCheckWhite;
            originalIconUnchecked = iconUncheckWhite;
        } else {
            originalIconChecked = iconCheckBlack;
            originalIconUnchecked = iconUncheckBlack;
        }
    }

    // Method to save tasks to a file
    public void saveTasksToFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            Gson gson = new Gson();
            String json = gson.toJson(tasks);
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving tasks to file.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to load tasks from a file
    public void loadTasksFromFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Task>>() {}.getType();
            List<Task> loadedTasks = gson.fromJson(reader, listType);
            if (loadedTasks != null) {
                tasks.clear();
                tasks.addAll(loadedTasks);
                // Update the table model
                model.setRowCount(0);
                for (Task task : tasks) {
                    model.addRow(new Object[]{task.status, task.task});
                }
                saveTasks(); // Save to persistence
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e; // Rethrow to be handled by caller
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading tasks from file.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to add context menu to the table
    private void addTableContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        
        JMenuItem deleteItem = new JMenuItem("Delete Selected");

        deleteItem.addActionListener(e -> deleteSelectedRows());

        popupMenu.add(deleteItem);
        todoTable.setComponentPopupMenu(popupMenu);
    }

    // Method to delete selected rows
    private void deleteSelectedRows() {
        int[] selectedRows = todoTable.getSelectedRows();
        if (selectedRows.length > 0) {
            List<Integer> rowsToDelete = new ArrayList<>();
            for (int row : selectedRows) {
                if (!isPlusRow(row)) {
                    rowsToDelete.add(row);
                }
            }
            // Remove from highest index to avoid shifting
            Collections.sort(rowsToDelete, Collections.reverseOrder());
            for (int rowIndex : rowsToDelete) {
                tasks.remove(rowIndex);
                model.removeRow(rowIndex);
            }
            saveTasks();
        } else {
            JOptionPane.showMessageDialog(this, "No tasks selected for deletion.", "Delete Tasks", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Method to check if a row is the "+" row
    private boolean isPlusRow(int row) {
        return row == model.getRowCount();
    }

    // Method to load and scale icons

    // Updated loadAndScaleIcon using Scalr
    private ImageIcon loadAndScaleIcon(URL iconURL) {
        double scalingFactor = DPIUtils.getScalingFactor();
        int baseIconSize = 24; // Original icon size
        int desiredIconSize = (int) (baseIconSize * scalingFactor); // Scale based on DPI

        String cacheKey = iconURL.toString() + "@" + desiredIconSize;
        if (iconCache.containsKey(cacheKey)) {
            return iconCache.get(cacheKey);
        }

        try {
            BufferedImage originalImage = ImageIO.read(iconURL);

            // Use Scalr to perform more high-quality scaling than java alone
            BufferedImage scaledImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC,
                    desiredIconSize, desiredIconSize, Scalr.OP_ANTIALIAS);
            

            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            iconCache.put(cacheKey, scaledIcon);
            return scaledIcon;
        } catch (IOException e) {
            e.printStackTrace();
            ImageIcon defaultIcon = createDefaultIcon(desiredIconSize, desiredIconSize);
            iconCache.put(cacheKey, defaultIcon);
            return defaultIcon;
        }
    }


    // Method to create a default icon
    private ImageIcon createDefaultIcon(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        g2.setColor(Color.GRAY);
        g2.fillOval(0, 0, width, height); // Example: Draw a gray circle as a placeholder
        g2.dispose();
        return new ImageIcon(img);
    }


    // Method to add mouse listener to the table
    private void addTableMouseListener() {
        todoTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseEvent(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseEvent(e);
            }

            private void handleMouseEvent(MouseEvent e) {
                int row = todoTable.rowAtPoint(e.getPoint());
                int column = todoTable.columnAtPoint(e.getPoint());

                if (e.isPopupTrigger()) {
                    if (row >= 0 && !isPlusRow(row)) {
                        todoTable.setRowSelectionInterval(row, row);
                        showTaskContextMenu(e.getComponent(), e.getX(), e.getY(), row);
                    }
                } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    if (!isPlusRow(row) && column == 1) {
                        todoTable.editCellAt(row, column);
                    }
                } else if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                    if (isPlusRow(row)) {
                        // Prompt user to enter a new task
                        String taskText = JOptionPane.showInputDialog(TodoPanel.this, "Enter new task:", "Add Task", JOptionPane.PLAIN_MESSAGE);
                        if (taskText != null && !taskText.trim().isEmpty()) {
                            addNewTask(taskText.trim());
                        }
                    }
                }
            }
        });
    }

    // Method to show task context menu
    private void showTaskContextMenu(Component invoker, int x, int y, int row) {
        JPopupMenu taskMenu = new JPopupMenu();
        JMenuItem changeCheckboxColorItem = new JMenuItem("Change Checkbox Color");
        JMenuItem deleteTaskItem = new JMenuItem("Delete Task");

        changeCheckboxColorItem.addActionListener(e -> changeTaskCheckboxColor(row));
        deleteTaskItem.addActionListener(e -> deleteTask(row));

        taskMenu.add(changeCheckboxColorItem);
        taskMenu.add(deleteTaskItem);

        taskMenu.show(invoker, x, y);
    }

    // Method to change checkbox color for a specific task
    private void changeTaskCheckboxColor(int row) {
        Task task = tasks.get(row);
        Color currentColor = task.getCheckboxColor();
        Color selectedColor = JColorChooser.showDialog(this, "Select Checkbox Color", currentColor);
        if (selectedColor != null) {
            task.setCheckboxColor(selectedColor);
            todoTable.repaint();
            saveTasks();
        }
    }

    // Method to delete a specific task
    private void deleteTask(int row) {
        tasks.remove(row);
        model.removeRow(row);
        saveTasks();
    }

    // Method to add a new task
    private void addNewTask(String taskText) {
        Task newTask = new Task(false, taskText);
        tasks.add(newTask);
        model.insertRow(model.getRowCount(), new Object[]{newTask.status, newTask.task});
        saveTasks();
    }

    // Method to save tasks
    private void saveTasks() {
        persistenceHandler.saveTasks(tasks);
    }

    // Method to load tasks
    private void loadTasks() {
        List<Task> loadedTasks = persistenceHandler.loadTasks();
        if (loadedTasks != null && !loadedTasks.isEmpty()) {
            tasks.addAll(loadedTasks);
        } else {
            
            // Define the list of colors to cycle through
            Color[] colors = {
                Color.RED,
                Color.ORANGE,
                Color.YELLOW,
                Color.GREEN,
                Color.BLUE
            };
            
            // Create the default task texts
            Task[] defaultTasks = {
                new Task(true, "Welcome to FancyToDo!"),
                new Task(true, "Press F9 to hide and unhide the apllication"),
                new Task(true, "Right click tabs for tab customization options"),
                new Task(true, "Right click todo items to customize or delete an item"),
                new Task(true, "Go to settings for theme related options")
            };

            for (int i = 0; i <= 4; i++) {
                
                Task newTask = defaultTasks[i]; // Create a new Task instance

                // Cycle through the colors array using modulo to wrap around if i exceeds the array length
                Color c = colors[i % colors.length];

                // Set the checkbox color for the new task
                newTask.setCheckboxColor(c);

                // Add the new task to the tasks list
                tasks.add(newTask);
            }

        }
    }

    // Method to update checkbox renderer and editor
    private void updateCheckboxRendererAndEditor() {
        if (useCustomCheckboxes) {
            todoTable.getColumnModel().getColumn(0).setCellRenderer(new CustomCheckBoxRenderer());
            todoTable.getColumnModel().getColumn(0).setCellEditor(new CustomCheckBoxEditor());
        } else {
            todoTable.getColumnModel().getColumn(0).setCellRenderer(todoTable.getDefaultRenderer(Boolean.class));
            todoTable.getColumnModel().getColumn(0).setCellEditor(todoTable.getDefaultEditor(Boolean.class));
        }
        todoTable.repaint();
    }

    // Custom checkbox renderer
    private class CustomCheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        public CustomCheckBoxRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
            setOpaque(true);
            setBorderPainted(false);
            setText("");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            if (isPlusRow(row)) {
                return new JLabel(); // Empty label for the "+" row
            }

            setSelected(Boolean.TRUE.equals(value));
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

            Task task = tasks.get(row);
            Color taskCheckboxColor = task.getCheckboxColor();
            if (taskCheckboxColor == null) {
                taskCheckboxColor = TodoPanel.this.checkboxColor; // Use tab's default if none
            }

            ImageIcon[] icons = getTintedIcons(taskCheckboxColor);
            setIcon(icons[0]); // Unchecked icon
            setSelectedIcon(icons[1]); // Checked icon

            return this;
        }
    }

    // Custom checkbox editor
    private class CustomCheckBoxEditor extends AbstractCellEditor implements TableCellEditor {
        private JCheckBox checkBox;

        public CustomCheckBoxEditor() {
            checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(JLabel.CENTER);
            checkBox.setOpaque(true);
            checkBox.setBorderPainted(false);
            checkBox.setText("");

            checkBox.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Object getCellEditorValue() {
            return checkBox.isSelected();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            if (isPlusRow(row)) {
                return new JLabel(); // Empty label for the "+" row
            }

            checkBox.setSelected(Boolean.TRUE.equals(value));
            checkBox.setBackground(table.getBackground());

            Task task = tasks.get(row);
            Color taskCheckboxColor = task.getCheckboxColor();
            if (taskCheckboxColor == null) {
                taskCheckboxColor = TodoPanel.this.checkboxColor;
            }

            ImageIcon[] icons = getTintedIcons(taskCheckboxColor);
            checkBox.setIcon(icons[0]); // Unchecked icon
            checkBox.setSelectedIcon(icons[1]); // Checked icon

            return checkBox;
        }
    }

    // Method to get tinted icons
    private ImageIcon[] getTintedIcons(Color color) {
        if (color == null) {
            color = isColorDark(currentThemeColor) ? Color.WHITE : Color.BLACK;
        }

        ImageIcon[] icons = checkboxIconCache.get(color);
        if (icons == null) {
            ImageIcon tintedUnchecked = tintIcon(originalIconUnchecked, color);
            ImageIcon tintedChecked = tintIcon(originalIconChecked, color);
            icons = new ImageIcon[]{tintedUnchecked, tintedChecked};
            checkboxIconCache.put(color, icons);
        }
        return icons;
    }

    // Method to tint an icon
    private ImageIcon tintIcon(ImageIcon originalIcon, Color color) {
        BufferedImage img = new BufferedImage(
                originalIcon.getIconWidth(),
                originalIcon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        // Apply anti-aliasing to improve icon rendering quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2d.drawImage(originalIcon.getImage(), 0, 0, null);
        g2d.setComposite(AlphaComposite.SrcAtop);
        g2d.setColor(color);
        g2d.fillRect(0, 0, img.getWidth(), img.getHeight());
        g2d.dispose();

        return new ImageIcon(img);
    }

    // Method to check if a color is dark
    private boolean isColorDark(Color color) {
        double luminance = (0.299 * color.getRed() +
                0.587 * color.getGreen() +
                0.114 * color.getBlue()) / 255;
        return luminance < 0.5;
    }

    // Method to get contrasting color
    private Color getContrastingColor(Color color) {
        int d;
        double luminance = (0.299 * color.getRed() +
                0.587 * color.getGreen() +
                0.114 * color.getBlue()) / 255;
        if (luminance > 0.5) {
            d = 0; // Dark text
        } else {
            d = 255; // Light text
        }
        return new Color(d, d, d);
    }

    // Method to apply theme color
    public void applyThemeColor(Color themeColor) {
        if (themeColor == null) {
            themeColor = UIManager.getColor("Panel.background");
            if (themeColor == null) {
                themeColor = Color.WHITE; // Default color
            }
        }
        this.currentThemeColor = themeColor;

        // Reload icons based on theme
        loadIcons();
        // Clear icon cache
        checkboxIconCache.clear();

        // Update renderer and editor
        updateCheckboxRendererAndEditor();

        setBackground(themeColor);
        todoTable.setBackground(themeColor);
        todoTable.setForeground(getContrastingColor(themeColor));
        todoTable.setGridColor(themeColor.darker());
        todoTable.getTableHeader().setBackground(themeColor.darker());
        todoTable.getTableHeader().setForeground(getContrastingColor(themeColor));

        // Update the scroll pane's viewport background
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, todoTable);
        if (scrollPane != null) {
            scrollPane.getViewport().setBackground(themeColor);
        }

        // Repaint the table
        todoTable.repaint();
    }

    // Method to change checkbox color at tab level
    public void changeCheckboxColor() {
        Color selectedColor = JColorChooser.showDialog(this, "Select Checkbox Color", checkboxColor);
        if (selectedColor != null) {
            checkboxColor = selectedColor;
            // Clear icon cache
            checkboxIconCache.clear();
            // Update renderer and editor
            updateCheckboxRendererAndEditor();
            todoTable.repaint();
        }
    }

}
