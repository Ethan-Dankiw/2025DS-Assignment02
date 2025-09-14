package net.ethandankiw.data;

import net.ethandankiw.data.http.JSON;

public class WeatherData {

	private final JSON json;
	private final long lastUpdated;
	private final long lamportClock;


	public WeatherData(JSON json, long lamportClock) {
		this.json = json;
		this.lastUpdated = System.currentTimeMillis();
		this.lamportClock = lamportClock;
	}


	public JSON getJson() {
		return json;
	}


	public long getLastUpdated() {
		return lastUpdated;
	}


	public long getLamportClock() {
		return lamportClock;
	}
}
