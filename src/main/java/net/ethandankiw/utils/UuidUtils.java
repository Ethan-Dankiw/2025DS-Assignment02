package net.ethandankiw.utils;

import java.util.UUID;

public class UuidUtils {

	private UuidUtils() {
	}

	public static String generateUUID() {
		return UUID.randomUUID().toString();
	}
}
