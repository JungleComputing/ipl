package ibis.satin;

final class IRVector {
	InvocationRecord[] l = new InvocationRecord[500];
	int count=0;

	void add(InvocationRecord r) {
		if(count >= l.length) {
			InvocationRecord[] nl = new InvocationRecord[l.length*2];
			System.arraycopy(l, 0, nl, 0, l.length);
			l = nl;
		}

		l[count] = r;
		count++;
	}

	InvocationRecord remove(int stamp, ibis.ipl.IbisIdentifier owner) {
		InvocationRecord res = null;

		for(int i=0; i<count; i++) {
			if(l[i].stamp == stamp && l[i].owner.equals(owner)) {
				res = l[i];
				count--;
				l[i] = l[count];
				return res;
			}
		}

		return null;
	}

	void remove(InvocationRecord r) {
		for(int i=count-1; i>=0; i--) {
			if(l[i].equals(r)) {
				count--;
				l[i] = l[count];
				return;
			}
		}

		System.err.println("EEK, IRVector: removeing non-existant elt: " + r);
	}

	InvocationRecord removeIndex(int i) {
		if(i >= count) return null;

		InvocationRecord res = l[i];
		count--;
		l[i] = l[count];
		return res;
	}

	void print(java.io.PrintStream out) {
		out.println("==============IRVector:=============");
		for(int i=0; i<count; i++) {
			out.println("elt [" + i + "] = " + l[i]);
		}
		out.println("=========end of IRVector:===========");
	}
}
