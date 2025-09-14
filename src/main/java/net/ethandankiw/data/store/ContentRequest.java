package net.ethandankiw.data.store;

import java.util.Objects;

import net.ethandankiw.data.http.JSON;

public class ContentRequest implements Comparable<ContentRequest> {
	private final String id;
	private final JSON json;
	private final long lamportClock;

	public ContentRequest(String id, JSON json, long lamportClock) {
		this.id = id;
		this.json = json;
		this.lamportClock = lamportClock;
	}

	public String getId() {
		return id;
	}

	public JSON getJson() {
		return json;
	}

	public long getLamportClock() {
		return lamportClock;
	}

	@Override
	public int compareTo(ContentRequest other) {
		return Long.compare(this.lamportClock, other.lamportClock);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ContentRequest other = (ContentRequest) obj;
		return Objects.equals(this.id, other.id) &&
				Objects.equals(this.json, other.json);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, json, lamportClock);
	}
}
