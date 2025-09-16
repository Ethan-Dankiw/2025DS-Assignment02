package net.ethandankiw.data.store;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import net.ethandankiw.data.http.HttpRequestMethod;
import net.ethandankiw.data.http.JSON;

public class ContentRequest implements Comparable<ContentRequest> {

	private final String id;
	private final HttpRequestMethod method;
	private final JSON json;
	private final long lamportClock;

	// For GET requests, a CompletableFuture is needed to return a value
	private final CompletableFuture<JSON> future;


	public ContentRequest(String id, JSON json, long lamportClock) {
		this.id = id;
		this.method = HttpRequestMethod.PUT;
		this.json = json;
		this.lamportClock = lamportClock;
		this.future = null;
	}


	public ContentRequest(String id, CompletableFuture<JSON> future, long lamportClock) {
		this.id = id;
		this.method = HttpRequestMethod.GET;
		this.json = null;
		this.lamportClock = lamportClock;
		this.future = future;
	}


	public String getId() {
		return id;
	}


	public HttpRequestMethod getMethod() {
		return method;
	}


	public JSON getJson() {
		return json;
	}


	public long getLamportClock() {
		return lamportClock;
	}


	public CompletableFuture<JSON> getFuture() { return future; }


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
		return Objects.equals(this.id, other.id)
				&& Objects.equals(this.json, other.json)
				&& this.lamportClock == other.lamportClock
				&& this.method == other.method;
	}


	@Override
	public int hashCode() {
		return Objects.hash(id, json, lamportClock);
	}
}
