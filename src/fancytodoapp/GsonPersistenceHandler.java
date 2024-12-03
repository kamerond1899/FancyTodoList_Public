package fancytodoapp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.List;
/**
 *
 *  Kameron Dangleben 12/3/24
 * 
 *  GsonPersistenceHandler
 * 
 *  Handler which manages GSON serialization of data. Tasks are broken into JSON
 *  friendly data which can be saved and loaded.
 * 
 */
public class GsonPersistenceHandler {

    private static final String DATA_DIR = "fancytodo_data";
    private String tabName;
    private Gson gson;

    public GsonPersistenceHandler(String tabName) {
        this.tabName = tabName;
        this.gson = new Gson();
        ensureDataDirExists();
    }

    private void ensureDataDirExists() {
        Path path = Paths.get(DATA_DIR);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveTasks(List<Task> tasks) {
        String json = gson.toJson(tasks);
        Path filePath = Paths.get(DATA_DIR, tabName + ".json");
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Task> loadTasks() {
        Path filePath = Paths.get(DATA_DIR, tabName + ".json");
        if (Files.exists(filePath)) {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                Type listType = new TypeToken<List<Task>>() {}.getType();
                return gson.fromJson(reader, listType);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null; // No saved tasks
    }
}
