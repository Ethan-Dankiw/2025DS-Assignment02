package net.ethandankiw.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

import net.ethandankiw.GlobalConstants;

public class ClientSocket {

	private static final Logger logger = Logger.getLogger(ClientSocket.class.getName());


	public static void main(String[] args) {
		// Connect to the server using its IP Address and Port number
		try (Socket client = new Socket(GlobalConstants.SERVER_IP, GlobalConstants.SERVER_PORT)) {
			logger.info("Connected to server at " + GlobalConstants.SERVER_IP + ":" + GlobalConstants.SERVER_PORT);

			// Get the communication stream being sent from the client
			BufferedReader fromServer = new BufferedReader(new InputStreamReader(client.getInputStream()));

			// Get the communication stream for sending stuff to the client
			PrintWriter toServer = new PrintWriter(client.getOutputStream(), true);

			// Send info to the server and read its response
			logger.info("Sending stuff to the server...");
			toServer.println("line 1");
			toServer.println("line 2");
			toServer.println("line 3");
			toServer.println("line 4");

			// Receive info from the server
			fromServer.lines().forEach(line -> logger.info("Server says: " + line));
		} catch (IOException ioe) {
			// Log a severe error since the server cannot be created
			logger.severe("[SERVER]: " + ioe.getMessage());
		}
	}
}
