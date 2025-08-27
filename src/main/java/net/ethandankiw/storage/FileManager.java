package net.ethandankiw.storage;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.utils.JsonUtils;

public class FileManager {

	public static final Logger logger = LoggerFactory.getLogger(FileManager.class);

	// Path to where files are stored
	private static final String STORAGE_DIR = "src/main/java/net/ethandankiw/storage/files";


	private FileManager() {
	}


	public static List<File> getListOfStoredFiles() {
		// Get the file directory for the stored files
		File dir = new File(STORAGE_DIR);

		// If the directory does not exist
		if (!dir.exists()) {
			logger.warn("Storage directory does not exist");
			return List.of();
		}

		// If the directory is not a directory
		if ( !dir.isDirectory()) {
			logger.warn("Storage directory exists, but is not a directory");
			return List.of();
		}

		// Get all the files in the dir
		File[] files = dir.listFiles();

		// If there are no files
		if (files == null || files.length == 0) {
			return List.of();
		}

		// Return the list of files
		return Arrays.asList(files);
	}


	public static Map<String, String> readJSONFromFile(String path) {
		// Open the file at the path
		File file = new File(STORAGE_DIR, path);

		// If the file does not exist
		if (!file.exists()) {
			logger.warn("JSON file does not exist");
			return Map.of();
		}

		// If the file is not a file
		if (!file.isFile()) {
			logger.warn("JSON file exists, but is not a file");
			return Map.of();
		}

		// Parse the file into JSON
		return JsonUtils.parseToJSON(file);
	}
}
