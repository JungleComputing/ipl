package ibis.satin;

final class RandomWorkStealing implements Algorithm {
	Satin s;

	RandomWorkStealing(Satin s) {
		this.s = s;
	}

	public void syncHandler() {
		Victim v = null;
		synchronized(s) {
			v = s.victims.getRandomVictim();
		}
		s.stealJob(v);
	}
}
