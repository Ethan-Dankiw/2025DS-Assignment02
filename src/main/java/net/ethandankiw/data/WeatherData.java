package net.ethandankiw.data;

import net.ethandankiw.data.http.JSON;

public class WeatherData {

	private final JSON json;
	private final long lastUpdated;


	public WeatherData(JSON json) {
		this.json = json;
		this.lastUpdated = System.currentTimeMillis();
	}


	public JSON getJson() {
		return json;
	}


	public long getLastUpdated() {
		return lastUpdated;
	}
}
