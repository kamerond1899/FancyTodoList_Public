package fancytodoapp;

import java.awt.Color;
import java.io.*;
import java.nio.file.*;
/**
 *
 *  Kameron Dangleben 12/3/24
 * 
 *  ColorPersistenceHandler
 * 
 *  Ensures theme colors load and save properly and consistenty.
 * 
 */
public class ColorPersistenceHandler {
    private static final String COLOR_FILE = "fancytodo_data/theme_color.dat";

    public void saveColor(Color color) {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(COLOR_FILE))) {
            dos.writeInt(color.getRGB());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Color loadColor() {
        Path path = Paths.get(COLOR_FILE);
        if (Files.exists(path)) {
            try (DataInputStream dis = new DataInputStream(new FileInputStream(COLOR_FILE))) {
                int rgb = dis.readInt();
                return new Color(rgb);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null; // Default color
    }
}
