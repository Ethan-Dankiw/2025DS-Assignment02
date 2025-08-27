package net.ethandankiw.data;

import java.util.Comparator;

import net.ethandankiw.server.AggregationServer;

public class ServerLoadComparator implements Comparator<AggregationServer> {
	@Override
	public int compare(AggregationServer server1, AggregationServer server2) {
		return Double.compare(server1.getLoad(), server2.getLoad());
	}
}
