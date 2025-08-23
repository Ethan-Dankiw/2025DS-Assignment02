package net.ethandankiw.storage;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.utils.JsonUtils;

public class FileManager {

	private FileManager() {
	}


	public static final Logger logger = LoggerFactory.getLogger(FileManager.class);

	public static List<File> getListOfStoredFiles() {
		// Get the file directory for the stored files
		File dir = new File(getStorageDirectory());

		// Get all the files in the dir
		File[] files = dir.listFiles();

		// If there are no files
		if (files == null) {
			return List.of();
		}

		// Return the list of files
		return Arrays.stream(files).toList();
	}


	public static Map<String, String> readJSONFromFile(String path) {
		// Open the file at the path
		File file = openFile(path);

		// Parse the file into JSON
		return JsonUtils.parseFile(file);
	}


	private static File openFile(String path) {
		// Get the file directory for stored files
		File dir = new File(getStorageDirectory());

		// Get the file at the specified path
		return new File(dir, path);
	}

	private static String getStorageDirectory() {
		// Get the current working directory
		File currentDir = new File(".");

		try {
			// Goto the storage directory folder
			return currentDir.getCanonicalPath() + File.separator + "src/main/java/net/ethandankiw/storage/files/";

		} catch (IOException ioe) {
			logger.error("Unable to get the current working directory");
		}

		return "";
	}
}
