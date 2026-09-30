package no.nav.dokdistsentralprint.consumer.leaderelection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.UnknownHostException;

import static java.net.InetAddress.getLocalHost;

@Slf4j
@Component
public class LeaderElectionConsumer {

	private final RestClient restClient ;

	public LeaderElectionConsumer(RestClient restClient,
	                              @Value("${elector.get.url}") String electorUrl) {
		this.restClient = restClient.mutate()
				.baseUrl(electorUrl)
				.build();
	}

	public boolean isLeader() {
		Leader leader = restClient.get()
				.retrieve()
				.body(Leader.class);

		if (leader == null) {
			return false;
		}

		try {
			return getLocalHost().getHostName().equals(leader.name());
		} catch (UnknownHostException e) {
			return false;
		}
	}

}