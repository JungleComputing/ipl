package ibis.satin;
import ibis.ipl.IbisIdentifier;

final class StampVector implements Config {
	int[] stamps = new int[50];
	IbisIdentifier[] owners = new IbisIdentifier[50];
	int count=0;

	public void add(int stamp, IbisIdentifier owner) {
		if(count >= stamps.length) {
			int[] nstamps = new int[stamps.length*2];
			System.arraycopy(stamps, 0, nstamps, 0, stamps.length);
			stamps = nstamps;

			IbisIdentifier[] nowners = new IbisIdentifier[owners.length*2];
			System.arraycopy(owners, 0, nowners, 0, owners.length);
			owners = nowners;
		}

		stamps[count] = stamp;
		owners[count] = owner;
		count++;
	}

	public boolean containsParentOf(int stamp, ibis.ipl.IbisIdentifier owner) {
		for(int i=0; i<count; i++) {
			if(stamps[i] == stamp && owners[i].equals(owner)) return true;
		}

		return false;
	}

	int getIndex(int stamp, IbisIdentifier owner) {
		for(int i=0; i<count; i++) {
			if(stamps[i] == stamp && owners[i].equals(owner)) {
				return i;
			}
		}

		return -1;
	}

	void removeIndex(int i) {
		if(ASSERTS) {
			if(i >= count || i < 0) {
				System.err.println("removeIndex of strange index: " + i);
				new Exception().printStackTrace();
				System.exit(1);
			}
		}

		count--;
		stamps[i] = stamps[count];
		owners[i] = owners[count];
	}
}
