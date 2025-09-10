package net.ethandankiw.data.http;

import java.util.HashMap;
import java.util.Map;

public class JSON {
	private final Map<String, String> data;

	public JSON() {
		this.data = new HashMap<>();
	}

	public Map<String, String> get() {
		return this.data;
	}

	public void add(String key, String value) {
		this.data.put(key, value);
	}

	public String getValue(String key) {
		return this.data.get(key);
	}

	public boolean containsKey(String key) {
		return this.data.containsKey(key);
	}
}
