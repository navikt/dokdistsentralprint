package no.nav.dokdistsentralprint.sdist009;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistsentralprint.consumer.leaderelection.LeaderElectionConsumer;
import org.apache.camel.cluster.CamelClusterMember;
import org.apache.camel.support.cluster.AbstractCamelClusterService;
import org.apache.camel.support.cluster.AbstractCamelClusterView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Camel cluster service basert på leaderelection-endepunktet på NAIS. Brukes av ClusteredRoutePolicy
 * for å starte/stoppe sdist009 når poden blir eller slutter å være leader.
 */
@Slf4j
@Component
public class LeaderElectionClusterService extends AbstractCamelClusterService<LeaderElectionClusterService.LeaderElectionClusterView> {

	private static final long TID_MELLOM_HVER_LEADEROPPDATERING = 60;
	private static final String LEADER_ELECTION_CLUSTER_SERVICE_ID = "sdist009-leader-election";

	private final LeaderElectionConsumer leaderElectionConsumer;

	public LeaderElectionClusterService(LeaderElectionConsumer leaderElectionConsumer) {
		super(LEADER_ELECTION_CLUSTER_SERVICE_ID);
		this.leaderElectionConsumer = leaderElectionConsumer;
	}

	@Override
	protected LeaderElectionClusterView createView(String namespace) {
		return new LeaderElectionClusterView(namespace);
	}

	class LeaderElectionClusterView extends AbstractCamelClusterView implements CamelClusterMember {

		private volatile boolean leader;
		private ScheduledExecutorService executorService;

		LeaderElectionClusterView(String namespace) {
			super(LeaderElectionClusterService.this, namespace);
		}

		@Override
		protected void doStart() {
			executorService = getCamelContext()
					.getExecutorServiceManager()
					.newSingleThreadScheduledExecutor(this, "LeaderElectionThreadPool-" + getNamespace());
			executorService.scheduleWithFixedDelay(this::oppdaterLeaderstatus, 0, TID_MELLOM_HVER_LEADEROPPDATERING, SECONDS);
		}

		@Override
		protected void doStop() {
			if (executorService != null) {
				getCamelContext().getExecutorServiceManager().shutdownNow(executorService);
			}
			leader = false;
		}

		private void oppdaterLeaderstatus() {
			boolean nyLeaderstatus;
			try {
				nyLeaderstatus = leaderElectionConsumer.isLeader();
			} catch (Exception e) {
				log.warn("Kunne ikke hente leader fra elector. Setter leader=false. Feilmelding={}", e.getMessage());
				nyLeaderstatus = false;
			}

			if (leader == nyLeaderstatus) {
				return;
			}

			log.info("Poden sin leader-status har endret seg fra leaderstatus={} til leaderstatus={}", leader, nyLeaderstatus);
			leader = nyLeaderstatus;

			fireLeadershipChangedEvent(nyLeaderstatus ? this : null);
		}

		@Override
		public Optional<CamelClusterMember> getLeader() {
			return leader ? Optional.of(this) : Optional.empty();
		}

		@Override
		public CamelClusterMember getLocalMember() {
			return this;
		}

		@Override
		public List<CamelClusterMember> getMembers() {
			return List.of(this);
		}

		@Override
		public String getId() {
			return "local";
		}

		@Override
		public boolean isLeader() {
			return leader;
		}

		@Override
		public boolean isLocal() {
			return true;
		}
	}

}