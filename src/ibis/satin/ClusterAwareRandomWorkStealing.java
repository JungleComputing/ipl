package ibis.satin;

final class ClusterAwareRandomWorkStealing implements Algorithm {
	Satin s;

	ClusterAwareRandomWorkStealing(Satin s) {
		this.s = s;
	}

	public void syncHandler() {
		Victim lv = null;
		Victim rv = null;

		synchronized(s) {
			lv = s.victims.getLocalRandomVictim();

			if(!s.asyncStealInProgress) {
				rv = s.victims.getRemoteRandomVictim();
			}
		}
		
		if(rv != null) s.sendAsynchronousStealRequest(rv);
		s.stealJob(lv);
	}
}
