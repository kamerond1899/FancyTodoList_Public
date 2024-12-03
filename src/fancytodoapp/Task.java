
package fancytodoapp;

import java.awt.Color;

/**
 *
 * @author Kam
 * 
 *  Task
 * 
 *  Object which contains all info for each to-do list task. JSON friendly.
 * 
 */
public class Task {
    public boolean status; // Checked or unchecked
    public String task;  // To-do list text
    public int checkboxColorRGB; // Store the color as RGB integer (-1 if default)

    public Task(boolean status, String task) {
        this.status = status;
        this.task = task;
        this.checkboxColorRGB = -1; // Default color indicator (see setter and getter for handling)
    }

    public Color getCheckboxColor() {
        return checkboxColorRGB != -1 ? new Color(checkboxColorRGB, true) : null;
    }

    public void setCheckboxColor(Color color) {
        this.checkboxColorRGB = color != null ? color.getRGB() : -1;
    }
}
