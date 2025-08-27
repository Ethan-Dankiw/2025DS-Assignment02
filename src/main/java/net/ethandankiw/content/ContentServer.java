package net.ethandankiw.content;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ethandankiw.storage.FileManager;

public class ContentServer {

	public static final Logger logger = LoggerFactory.getLogger(ContentServer.class);


	public static void main(String[] args) {

		// Get a list of files in the storage directory
		List<File> files = FileManager.getListOfStoredFiles();

		// Print the file names of each file
		files.forEach(file -> logger.debug("File Name: {}", file.getName()));
	}
}
