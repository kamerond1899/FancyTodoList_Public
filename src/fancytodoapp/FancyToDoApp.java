package fancytodoapp;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import static fancytodoapp.FancyToDoApp.DPIUtils.getScalingFactor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
/**
 *
 *  Kameron Dangleben 12/3/24
 * 
 *  FancyTodoApp
 * 
 *  The main class for FancyToDoApp. Handles the main UI of the application
 *  as well as themes and file saving. Makes use of every other class within
 *  the fancytodoapp package.
 * 
 */
public class FancyToDoApp extends JFrame implements NativeKeyListener {

    private JTabbedPane tabbedPane;
    private JMenuBar menuBar;

    private TrayIcon trayIcon;

    private int cursorType = Cursor.DEFAULT_CURSOR;

    private boolean isDarkTheme = true;
    private Color themeColor = null; // Default theme color
    private ColorPersistenceHandler colorPersistenceHandler;
    private Point initialClick;
    private Point startPos;
    private Rectangle startBounds;
    private boolean resizing = false;
    private static final int RESIZE_BORDER = 10;
    private JPanel titleBar;
    private JButton settingsButton;
    private JButton fileButton;
    private JButton closeButton;
    
    
    
    public FancyToDoApp() {
        setTitle("FancyToDo");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(400, 300);
        setAlwaysOnTop(true);
        setUndecorated(true);

        getContentPane().setLayout(new BorderLayout());

        positionWindowNearSystemTray();

        tabbedPane = new JTabbedPane();

        // Title bar panel
        titleBar = new JPanel(new BorderLayout());
        titleBar.setPreferredSize(new Dimension(getWidth(), 25));
        titleBar.setBackground(Color.GRAY);

        // Close button
        closeButton = new JButton("X");
        closeButton.setPreferredSize(new Dimension(40, 25));
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setOpaque(true);
        closeButton.setBackground(Color.GRAY);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        closeButton.addActionListener(e -> System.exit(0));

        // Close button UX responsiveness
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setBackground(Color.RED);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setBackground(titleBar.getBackground());
            }
        });

        // Pseudo menu bar buttons
        fileButton = new JButton("File");
        styleTitleBarButton(fileButton);

        settingsButton = new JButton("Settings");
        styleTitleBarButton(settingsButton);

        // Pseudo menu bar popups
        JPopupMenu fileMenuPopup = createFileMenuPopup();
        JPopupMenu settingsMenuPopup = createSettingsMenuPopup();

        // Add mouse listeners to show popups
        addMenuButtonListener(fileButton, fileMenuPopup);
        addMenuButtonListener(settingsButton, settingsMenuPopup);

        // Create a panel for menu buttons. Pseudo menu bar
        JPanel menuPanel = new JPanel();
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        menuPanel.add(fileButton);
        menuPanel.add(settingsButton);

        // Add components to the title bar
        titleBar.add(menuPanel, BorderLayout.WEST);
        titleBar.add(closeButton, BorderLayout.EAST);
        
        // Apply initial theme colors to prevent default theme
        applyThemeToTitleBar();
        // Add the title bar to the frame
        getContentPane().add(titleBar, BorderLayout.NORTH);

        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        colorPersistenceHandler = new ColorPersistenceHandler();
        themeColor = colorPersistenceHandler.loadColor();

        if (themeColor == null) {
            themeColor = UIManager.getColor("Panel.background");
            if (themeColor == null) {
                themeColor = Color.DARK_GRAY;
            }
        }

        applyThemeColor(themeColor);

        addPlusTab();

        loadTabs();
        
        // Default tab in case zero exist upon launch
        if (tabbedPane.getTabCount() <= 1) {
            addTodoTab("Default");
        }

        addWindowDragListeners(titleBar);

        setMinimumSize(new Dimension(300, 200));

        registerGlobalHotkey();

        initializeSystemTray();

        addTabContextMenu();

        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
                if (tabIndex == tabbedPane.indexOfTab("+")) {
                    addNewTab();
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (NativeHookException ex) {
                    ex.printStackTrace();
                }
                trayIcon.displayMessage("FancyToDo", "Application exiting.", TrayIcon.MessageType.INFO);
                System.exit(0);
            }

            @Override
            public void windowIconified(WindowEvent e) {
                setVisible(false);
                trayIcon.displayMessage("FancyToDo", "Application minimized to tray.", TrayIcon.MessageType.INFO);
            }
        });
    }
    
    private boolean useCustomCheckboxes = true; // Default to using custom checkboxes
    
    private void toggleCheckboxStyle() {
        useCustomCheckboxes = !useCustomCheckboxes;
        // Update all TodoPanels
        for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof TodoPanel) {
                ((TodoPanel) comp).setUseCustomCheckboxes(useCustomCheckboxes);
            }
        }
    }
    
    private void switchTheme() {
        try {
            if (isDarkTheme) {
                UIManager.setLookAndFeel(new FlatLightLaf());
                isDarkTheme = false;
            } else {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                isDarkTheme = true;
            }

            // Apply theme to title bar and components
            applyThemeToTitleBar();
            SwingUtilities.updateComponentTreeUI(this);
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
    }
    private void applyThemeToTitleBar() {
        Color backgroundColor = UIManager.getColor("Panel.background");
        Color foregroundColor = UIManager.getColor("Panel.foreground");

        // Apply colors to title bar
        titleBar.setBackground(backgroundColor);

        // Update menu buttons
        for (JButton button : new JButton[]{fileButton, settingsButton, closeButton}) {
            button.setBackground(backgroundColor);
            button.setForeground(foregroundColor);
        }
    }

    private void chooseThemeColor() {
    Color selectedColor = JColorChooser.showDialog(this, "Select Theme Color", themeColor);
    if (selectedColor != null) {
        themeColor = selectedColor;
        applyThemeColor(themeColor);
        colorPersistenceHandler.saveColor(themeColor);
        saveTabs();
    }
}
    private void applyThemeColor(Color color) {
        // Update the background color of the main frame
        getContentPane().setBackground(color);
        // Update the background color of tabbedPane
        tabbedPane.setBackground(color.darker());
        tabbedPane.setForeground(getContrastingColor(color));

        // Update colors in all TodoPanels
        for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) { // Exclude "+" tab
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof TodoPanel) {
                ((TodoPanel) comp).applyThemeColor(color);
            }
        }
        // Update the UI
        SwingUtilities.updateComponentTreeUI(this);
    }
    private Color getContrastingColor(Color color) {
        int d = 0;
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        if (luminance > 0.5) {
            d = 0; // Dark text
        } else {
            d = 255; // Light text
        }
        return new Color(d, d, d);
    }

    private void saveTabAs() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (isPlusTab(selectedIndex)) {
            JOptionPane.showMessageDialog(this, "Cannot save the '+' tab.", "Save As", JOptionPane.WARNING_MESSAGE);
            return;
        }
        TodoPanel currentPanel = (TodoPanel) tabbedPane.getComponentAt(selectedIndex);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Todo List As");
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            currentPanel.saveTasksToFile(fileToSave);
        }
    }

    private void openTabFromFile() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Open Todo List");
    int userSelection = fileChooser.showOpenDialog(this);

    if (userSelection == JFileChooser.APPROVE_OPTION) {
        File fileToOpen = fileChooser.getSelectedFile();

        String tabName = JOptionPane.showInputDialog(
            this,
            "Enter name for the new tab:",
            "Open",
            JOptionPane.PLAIN_MESSAGE
        );

        if (tabName != null && !tabName.trim().isEmpty()) {
            // Check for duplicate tab names
            boolean exists = false;
            for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) { // Exclude "+" tab
                if (tabbedPane.getTitleAt(i).equalsIgnoreCase(tabName.trim())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                addTodoTab(tabName.trim());
                // Get the newly added tab
                TodoPanel newPanel = (TodoPanel) tabbedPane.getComponentAt(tabbedPane.getTabCount() - 2);
                try {
                    newPanel.loadTasksFromFile(fileToOpen);
                } catch (IOException ex) {
                    Logger.getLogger(FancyToDoApp.class.getName()).log(Level.SEVERE, null, ex);
                }
                saveTabs(); // Update the saved tabs
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "A tab with this name already exists.",
                    "Duplicate Tab",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Tab name cannot be empty.",
                "Invalid Name",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }
}

    private void saveTabs() {
       java.util.List<TabInfo> tabs = new ArrayList<>();
       for (int i = 0; i < tabbedPane.getTabCount(); i++) {
           if (!isPlusTab(i)) {
               String name = tabbedPane.getTitleAt(i);
               Color color = tabbedPane.getBackgroundAt(i);
               tabs.add(new TabInfo(name, color));
           }
       }
       Gson gson = new Gson();
       String json = gson.toJson(tabs);
       try (BufferedWriter writer = new BufferedWriter(new FileWriter("fancytodo_data/tabs.json"))) {
           writer.write(json);
       } catch (IOException e) {
           e.printStackTrace();
       }
   }
    private void loadTabs() {
        File tabsFile = new File("fancytodo_data/tabs.json");
        if (tabsFile.exists()) {
            boolean loadedSuccessfully = false;
            try (BufferedReader reader = new BufferedReader(new FileReader(tabsFile))) {
                Gson gson = new Gson();
                java.lang.reflect.Type listType = new TypeToken<List<TabInfo>>() {}.getType();
                List<TabInfo> tabs = gson.fromJson(reader, listType);
                if (tabs != null && !tabs.isEmpty()) {
                    for (TabInfo tabInfo : tabs) {
                        addTodoTab(tabInfo.getName(), tabInfo.getColor());
                    }
                    loadedSuccessfully = true;
                }
            } catch (JsonSyntaxException e) {
                System.err.println("Failed to parse tabs.json in new format. Attempting old format.");
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!loadedSuccessfully) {
                // Try to load in old format
                try (BufferedReader reader = new BufferedReader(new FileReader(tabsFile))) {
                    Gson gson = new Gson();
                    java.lang.reflect.Type listType = new TypeToken<List<String>>() {}.getType();
                    List<String> tabNames = gson.fromJson(reader, listType);
                    if (tabNames != null && !tabNames.isEmpty()) {
                        for (String tabName : tabNames) {
                            addTodoTab(tabName, null); // No color information in old format
                        }
                        // Save tabs in new format
                        saveTabs();
                        loadedSuccessfully = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!loadedSuccessfully) {
                // If all attempts fail, delete the corrupted file and start fresh
                System.err.println("Failed to load tabs. Deleting corrupted tabs.json file.");
                tabsFile.delete();
                addTodoTab("Main");
            }
        } else {
            addTodoTab("Main");
        }
    }

    private void handleOldTabFormat(File tabsFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(tabsFile))) {
            Gson gson = new Gson();
            java.lang.reflect.Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> tabNames = gson.fromJson(reader, listType);
            if (tabNames != null && !tabNames.isEmpty()) {
                for (String tabName : tabNames) {
                    addTodoTab(tabName, null); // No color information in old format
                }
                // After successfully migrating, save the tabs in new format
                saveTabs();
            } else {
                // If no tabs found, add a default tab
                addTodoTab("Main");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // If parsing fails, add a default tab
            addTodoTab("Main");
        }
    }

    private void positionWindowNearSystemTray() {
        // Get screen size and insets
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle screenRect = ge.getMaximumWindowBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());

        // Assume taskbar is at the bottom
        int x = screenRect.x + screenRect.width - getWidth() - 10; // 10 pixels padding from the right edge
        int y = screenRect.y + screenRect.height - getHeight() - 10; // 10 pixels padding from the bottom edge

        setLocation(x, y);
    }

    private void addPlusTab() {
        // Check if the "+" tab already exists
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getTitleAt(i).equals("+")) {
                return; // "+" tab already exists
            }
        }
        JPanel plusPanel = new JPanel(); // Empty panel for the "+" tab
        tabbedPane.addTab("+", null, plusPanel, "Add New Tab");
        tabbedPane.setEnabledAt(tabbedPane.getTabCount() - 1, false); // Disable interaction with the panel
    }

    private void addNewTab() {
        String newTabName = JOptionPane.showInputDialog(this, "Enter new tab name:", "Add New Tab", JOptionPane.PLAIN_MESSAGE);
        if (newTabName != null) {
            newTabName = newTabName.trim();
            if (!newTabName.isEmpty()) {
                // Check for duplicate tab names
                boolean exists = false;
                for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) { // Exclude "+" tab
                    if (tabbedPane.getTitleAt(i).equalsIgnoreCase(newTabName)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    addTodoTab(newTabName);
                } else {
                    JOptionPane.showMessageDialog(this, "A tab with this name already exists.", "Duplicate Tab", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Tab name cannot be empty.", "Invalid Name", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void addTodoTab(String title) {
        addTodoTab(title, null);
    }

    private void addTodoTab(String title, Color tabColor) {
        if (themeColor == null){
            themeColor = Color.WHITE;
        }
        TodoPanel todoPanel = new TodoPanel(title, themeColor); // Pass themeColor

        // Insert the new tab before the "+" tab
        int plusTabIndex = tabbedPane.indexOfTab("+");
        if (plusTabIndex == -1) {
            plusTabIndex = tabbedPane.getTabCount();
        }
        tabbedPane.insertTab(title, null, todoPanel, null, plusTabIndex);
        if (tabColor != null) {
            tabbedPane.setBackgroundAt(tabbedPane.indexOfComponent(todoPanel), tabColor);
        }
        tabbedPane.setSelectedComponent(todoPanel);
        saveTabs();
    }

    private void registerGlobalHotkey() {
        try {
            // Disable JNativeHook logging
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);

            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException ex) {
            ex.printStackTrace();
        }
    }

    // Implement NativeKeyListener methods
    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
        if (nativeKeyEvent.getKeyCode() == NativeKeyEvent.VC_F9) {
            // Toggle visibility
            SwingUtilities.invokeLater(() -> {
                setVisible(!isVisible());
                if (isVisible()) {
                    setExtendedState(JFrame.NORMAL);
                }
            });
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {
        // Not needed
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {
        // Not needed
    }



    private void addTabContextMenu() {
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
                    if (tabIndex != -1 && !isPlusTab(tabIndex)) { // Exclude "+" tab
                        showTabContextMenu(e.getComponent(), e.getX(), e.getY(), tabIndex);
                    }
                }
            }
        });
    }
    
    private boolean isPlusTab(int tabIndex) {
        return tabbedPane.getTitleAt(tabIndex).equals("+");
    }

    private void showTabContextMenu(Component invoker, int x, int y, int tabIndex) {
    JPopupMenu tabMenu = new JPopupMenu();
    JMenuItem renameItem = new JMenuItem("Rename Tab");
    JMenuItem deleteItem = new JMenuItem("Delete Tab");
    JMenuItem changeColorItem = new JMenuItem("Change Tab Color");
    JMenuItem changeCheckboxColorItem = new JMenuItem("Change Checkbox Color"); // New item

    renameItem.addActionListener(e -> renameTab(tabIndex));
    deleteItem.addActionListener(e -> deleteTab(tabIndex));
    changeColorItem.addActionListener(e -> changeTabColor(tabIndex));
    changeCheckboxColorItem.addActionListener(e -> changeTabCheckboxColor(tabIndex)); // New action

    tabMenu.add(renameItem);
    tabMenu.add(deleteItem);
    tabMenu.add(changeColorItem);
    tabMenu.add(changeCheckboxColorItem); // Add new item to menu

    tabMenu.show(invoker, x, y);
}

    private void changeTabCheckboxColor(int tabIndex) {
    Component comp = tabbedPane.getComponentAt(tabIndex);
    if (comp instanceof TodoPanel) {
        ((TodoPanel) comp).changeCheckboxColor();
    }
}

    private void changeTabColor(int tabIndex) {
        Color currentColor = tabbedPane.getBackgroundAt(tabIndex);
        Color selectedColor = JColorChooser.showDialog(this, "Select Tab Color", currentColor);
        if (selectedColor != null) {
            tabbedPane.setBackgroundAt(tabIndex, selectedColor);
            saveTabs(); // Save tab colors
        }
    }

    private void renameTab(int tabIndex) {
        String currentName = tabbedPane.getTitleAt(tabIndex);
        String newName = JOptionPane.showInputDialog(this, "Enter new name for the tab:", currentName);
        if (newName != null) {
            newName = newName.trim();
            if (!newName.isEmpty()) {
                // Check for duplicate tab names
                boolean exists = false;
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    if (i != tabIndex && tabbedPane.getTitleAt(i).equalsIgnoreCase(newName)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    tabbedPane.setTitleAt(tabIndex, newName);
                    saveTabs();
                    // Optionally, rename the persistence file
                    // Implement renaming logic in GsonPersistenceHandler if needed
                } else {
                    JOptionPane.showMessageDialog(this, "A tab with this name already exists.", "Duplicate Tab", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Tab name cannot be empty.", "Invalid Name", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void deleteTab(int tabIndex) {
        String tabName = tabbedPane.getTitleAt(tabIndex);
        if (tabbedPane.getTabCount() <= 2) { // At least one todo tab and the "+" tab
            JOptionPane.showMessageDialog(this, "Cannot delete the last remaining todo tab.", "Delete Tab", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the tab '" + tabName + "'?", "Delete Tab", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            tabbedPane.removeTabAt(tabIndex);
            saveTabs();
            // Optionally, delete the persistence file
            // Implement deletion logic in GsonPersistenceHandler if needed
        }
    }
    
    // Helper methods
    
    private void initializeSystemTray() {
        if (!SystemTray.isSupported()) {
            System.err.println("System tray not supported!");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        URL trayIconURL = getClass().getResource("/icons/trayIcon.png");
        if (trayIconURL == null) {
            System.err.println("trayIcon.png not found!");
            return;
        }
        Image image = Toolkit.getDefaultToolkit().getImage(trayIconURL);

        PopupMenu popup = new PopupMenu();

        MenuItem openItem = new MenuItem("Open");
        openItem.addActionListener(e -> {
            setVisible(true);
            setExtendedState(JFrame.NORMAL);
            trayIcon.displayMessage("FancyToDo", "Application opened.", TrayIcon.MessageType.INFO);
        });
        popup.add(openItem);

        MenuItem addTabItem = new MenuItem("Add New Tab");
        addTabItem.addActionListener(e -> {
            addNewTab();
            trayIcon.displayMessage("FancyToDo", "New tab added.", TrayIcon.MessageType.INFO);
        });
        popup.add(addTabItem);

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException ex) {
                ex.printStackTrace();
            }
            trayIcon.displayMessage("FancyToDo", "Application exiting.", TrayIcon.MessageType.INFO);
            System.exit(0);
        });
        popup.add(exitItem);

        trayIcon = new TrayIcon(image, "FancyToDo", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("FancyToDo - Your Persistent Todo List");
        trayIcon.addActionListener(e -> {
            setVisible(true);
            setExtendedState(JFrame.NORMAL);
            trayIcon.displayMessage("FancyToDo", "Application opened.", TrayIcon.MessageType.INFO);
        });

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }

        // Hide window instead of exiting when the close button is clicked
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }
    
    private void addWindowDragListeners(JPanel titleBar) {
        titleBar.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            initialClick = e.getPoint();
            startPos = e.getLocationOnScreen();
            startBounds = getBounds();
            resizing = (e.getX() <= RESIZE_BORDER && e.getY() <= RESIZE_BORDER);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            initialClick = null;
            resizing = false;
        }
    });

        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
            if (e.getX() <= RESIZE_BORDER && e.getY() <= RESIZE_BORDER) {
                titleBar.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
            } else {
                titleBar.setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (resizing) {
                Point currentPos = e.getLocationOnScreen();
                int dx = currentPos.x - startPos.x;
                int dy = currentPos.y - startPos.y;

                int newWidth = startBounds.width - dx;
                int newHeight = startBounds.height - dy;

                int newX = startBounds.x + dx;
                int newY = startBounds.y + dy;

                // Ensure minimum size
                if (newWidth < getMinimumSize().width) {
                    newWidth = getMinimumSize().width;
                    newX = startBounds.x + startBounds.width - newWidth;
                }
                if (newHeight < getMinimumSize().height) {
                    newHeight = getMinimumSize().height;
                    newY = startBounds.y + startBounds.height - newHeight;
                }

                setBounds(newX, newY, newWidth, newHeight);
            } else {
                // Move the window
                Point currentPos = e.getLocationOnScreen();
                int xMoved = currentPos.x - startPos.x;
                int yMoved = currentPos.y - startPos.y;

                int X = startBounds.x + xMoved;
                int Y = startBounds.y + yMoved;
                setLocation(X, Y);
            }
        }
    });
    }

    private void styleTitleBarButton(JButton button) {
        button.setFocusPainted(false);
        button.setContentAreaFilled(true); // Set to true to allow background color
        button.setBorderPainted(false);
        button.setOpaque(true); // Allows background to be colored
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(titleBar.getBackground());
        button.setForeground(UIManager.getColor("Button.foreground"));
        button.setFont(UIManager.getFont("Button.font"));
    }


    private void addMenuButtonListener(JButton button, JPopupMenu menuPopup) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                menuPopup.show(button, e.getX(), e.getY());
            }
        });
    }

    private JPopupMenu createFileMenuPopup() {
        JPopupMenu fileMenuPopup = new JPopupMenu();
        JMenuItem saveAsItem = new JMenuItem("Save Tab As");
        JMenuItem openItem = new JMenuItem("Open");

        saveAsItem.addActionListener(e -> saveTabAs());
        openItem.addActionListener(e -> openTabFromFile());

        fileMenuPopup.add(saveAsItem);
        fileMenuPopup.add(openItem);

        return fileMenuPopup;
    }

    private JPopupMenu createSettingsMenuPopup() {
        JPopupMenu settingsMenuPopup = new JPopupMenu();
        JMenuItem toggleAlwaysOnTopItem = new JMenuItem("Toggle Always on Top");
        JMenuItem switchThemeItem = new JMenuItem("Switch Theme");
        JMenuItem toggleCheckboxStyleItem = new JMenuItem("Toggle Checkbox Style");
        JMenuItem chooseColorItem = new JMenuItem("Choose Theme Color");

        toggleAlwaysOnTopItem.addActionListener(e -> {
            boolean isAlwaysOnTop = isAlwaysOnTop();
            setAlwaysOnTop(!isAlwaysOnTop);
            JOptionPane.showMessageDialog(this, "Always on Top is now " + (!isAlwaysOnTop));
        });

        switchThemeItem.addActionListener(e -> switchTheme());
        toggleCheckboxStyleItem.addActionListener(e -> toggleCheckboxStyle());
        chooseColorItem.addActionListener(e -> chooseThemeColor());

        settingsMenuPopup.add(toggleAlwaysOnTopItem);
        settingsMenuPopup.add(switchThemeItem);
        settingsMenuPopup.add(toggleCheckboxStyleItem);
        settingsMenuPopup.add(chooseColorItem);

        return settingsMenuPopup;
    }

    public class TabInfo {
        private String name;
        private int colorRGB;

        public TabInfo(String name, Color color) {
            this.name = name;
            this.colorRGB = color != null ? color.getRGB() : -1; // Use -1 for default color
        }

        public String getName() {
            return name;
        }

        public Color getColor() {
            return colorRGB != -1 ? new Color(colorRGB, true) : null;
        }
    }

    public class DPIUtils {
        /**
         * Estimates the scaling factor based on the screen DPI.
         * Assumes 96 DPI as 100% scaling.
         * 
         * @return scaling factor (e.g., 1.25 for 125%)
         */
        public static double getScalingFactor() {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            AffineTransform at = gc.getDefaultTransform();
            return at.getScaleX(); // Assuming uniform scaling for X and Y
        }
    }

    
    // Main method (run this class)
    public static void main(String[] args) {
        try {
            // Apply FlatLaf theme
            UIManager.setLookAndFeel(new FlatDarkLaf());
            getScalingFactor();
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            FancyToDoApp app = new FancyToDoApp();
            app.setVisible(true);
        });
    }
}
