import ibis.gmi.GroupMember;

public class JobQueue extends GroupMember implements i_JobQueue {

	DistanceTable table;
	Job first = null;
	int count = 0;

	public JobQueue(DistanceTable dist) {
		count = 0;
		table = dist;
	}

	public synchronized void addJob(Job j) {
		if (first == null) { 
			first = j;
			count++;
		} else { 
			if (j.length < first.length) { 
				j.next = first;
				first = j;
				count++;
			} else { 
				Job temp = first;
			
				while (temp.next != null) { 
					if (j.length < temp.next.length) {
						j.next = temp.next;
						temp.next = j;
						count++;
						return;
					} else { 
						temp = temp.next;
					}					
				}

				temp.next = j;
			} 
		}
	}

	public DistanceTable getTable() {
		return table;
	}

	public void setTable(DistanceTable table) {
		this.table = table;
	}

	public synchronized Job getJob() {

		if(first == null) {
//			System.out.println("getJob returns null");
			return null;
		} else { 
			Job temp = first;
			first = first.next;
			
//			System.out.println("Returning Job of length " + temp.length);
			
			count--;

			if (count % 100 == 0) { 
				System.out.println(count + " Jobs left");
			}
			return temp;
		}
	}

	public synchronized int jobsLeft() {
		return count;
	}

	public void barrier() {}
}
