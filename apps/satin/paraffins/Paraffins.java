// Salishan Paraffins Problem:
// Given an integer n, output the chemical structure of all
// paraffin molecules for i<=n, without repetition and in 
// order of increasing size. Include all isomers, but no 
// dupicates. The chemical formula for paraffin molecules is
// C(i)H(2i+2). Any representation for the molecules could 
// be chosen, as long as it clearly distinguishes among
// isomers.
 
// Solution is based on theory of free and oriented trees
// (see Knuth, D. The Art of Computer Programming, Vol. 1)

// The correct numbers are:
// 1, 1, 1, 2, 3, 5, 9, 18, 35, 75, 159, 335, 802, 1858, 4347, ...

import java.util.Vector;

interface IntArrayEnumeration {
	int[] nextElement();
}


interface Constants {
	int RadKind = 1;
	int CCPKind = 2;
	int BCPKind = 3;
}


interface Printable {
	void print();
}


final class RecursivePartition implements IntArrayEnumeration {
	int number_of_elements;
	int min_element;
	int max_element;
	Vector result;
	int position;
  
	RecursivePartition(int sum_of_elements, int number_of_elements,
			   int min_element, int max_element) {
		this.number_of_elements = number_of_elements;
		this.min_element        = min_element;
		this.max_element        = max_element;
    
		result = new Vector(number_of_elements * (max_element - min_element) / 2);
		position = 0;

		int[] p = new int[number_of_elements];
		recursively_partition(0, sum_of_elements, min_element, p);
	}

	public int[] nextElement() {
		if ((result == null) || (position >= result.size())) {
			return null;
		} else {
			return (int[])(result.elementAt(position++));
		}
	}

	void recursively_partition(int level, int remain, int prev_element, int[] p) {
		int remaining_levels = number_of_elements - level - 1;
		if (remaining_levels > 0) {
			int smallest = Math.max(prev_element,
						remain - max_element*remaining_levels);
			int largest  = Math.min(remain / (remaining_levels + 1),
						remain - min_element*remaining_levels);
			// forall (int ele = smallest; ele <= largest; ele++) {
			for (int ele = smallest; ele <= largest; ele++) {
				// for ele == smallest no new int[] is actually needed
				// we do it for potential parallelism.
				int[] q = new int[number_of_elements];
				System.arraycopy(p,0,q,0,level); //copy only the first entries
				q[level] = ele; 
				recursively_partition(level + 1, remain - ele, ele, q);
			}
		} else {
			p[level] = remain;
			result.addElement(p);
		}
	}
}

// ----------------------------------------------------------------------
// Classes for various paraffin radicals
   
abstract class Radical implements Printable {
	abstract public void print();
}

final class HydrogenRadical extends Radical { // hydrogen radical has no neighbors
	public void print() {
		System.out.print("H");
	}
} 

final class CarbonRadical extends Radical { // carboniferous radical has 3 neighbors
	Radical[] carbon_neighbors; // radicals attached to C root

	CarbonRadical() {
		carbon_neighbors = new Radical[3]; // carboniferous radical has 3 neighbors
	}
  
	CarbonRadical(Radical[] subradicals) {
		// create a carboniferous radical from 3 other radicals
		carbon_neighbors = subradicals;
	}
    
	public void print() {
		System.out.print("C(");
		for (int i = 0; i < carbon_neighbors.length; i++) {
			carbon_neighbors[i].print();
		}
		System.out.print(")");
	}
}
  
// ----------------------------------------------------------------------------
// various paraffin molecules

abstract class Molecule implements Printable {
	abstract public void print();
}
    
final class BCP_Molecule extends Molecule {
	// ``Bond centered paraffins (BCPs)'': even-sized double centroid 
	// paraffin molecules. The root corresponds to a carbon-
	// carbon bond connecting two radicals. The two radicals
	// of a BCP of size i each have exactly i/2 carbon atoms.

	final int num_neighbors = 2;  // has 2 radical neighbors
	Radical[] bond_neighbors;

	BCP_Molecule(Radical[] radicals) {
		// make a BCP paraffin molecule from 2 radicals
		bond_neighbors = radicals;
	}
  
	public void print() {
		System.out.print("BCP molecule:      (");
		for (int i = 0; i < bond_neighbors.length; i++) {
			Radical elt = bond_neighbors[i];
			if (elt != null) {
				System.out.print("(");
				elt.print();
				System.out.print(")");
			}
		}
		System.out.println();
	}
}

final class CCP_Molecule extends Molecule {
	// ``Carbon centered paraffins (CCPs)'': odd-sized paraffin molecules
	// or even-sized single-centroid paraffin molecules
	// The root is a carbon atom that has 4 subtrees representing
	// paraffin radicals. Each radical of a CCP of size i is
	// <= (i-1)/2

	final int num_neighbors = 4;  // has 4 radical neighbors
	Radical[] carbon_neighbors;

	CCP_Molecule(Radical[] radicals) {
		// make a CCP paraffin molecule from 4 radicals
		carbon_neighbors = radicals;
	}

	public void print() {
		System.out.print("CCP molecule:      (C");
		for (int i = 0; i < carbon_neighbors.length; i++) {
			Radical elt = carbon_neighbors[i];
			if (elt != null) {
				System.out.print("(");
				elt.print();
				System.out.print(")");
			}
		}
		System.out.println(")");
	}
} 
   
// ----------------------------------------------------------------------------

final class Paraffins implements Constants {

	static final boolean PRINT = false;

	Vector[] radicals;
	int size;
	int counts[];
	
	Paraffins(int size) {
		this.size = size;
		counts = new int[size+1];
	}
  
	void work() {
		long startTime = System.currentTimeMillis();

		getRadicals();

		startTime = (System.currentTimeMillis() - startTime);
		System.out.println("Elapsed Time for radicals " + startTime/60000 +":"+
				   (startTime%60000)/1000+":"+(startTime%1000));

		generate(size);

		for (int i=0; i<size+1; i++) {
			System.out.println("size " + i + " has " + counts[i] + " molecules");
		}
	}

	void getRadicals() {
		radicals = new Vector[size/2+1];
		for (int i = 0; i < (size/2+1); i++)
			radicals[i] = new Vector(1000);
    
		// initialize a list of radicals 
    
		// hydrogen is a radical of size 0 (no C atoms)
		radicals[0].addElement(new HydrogenRadical());

		for (int i = 1; i <= size/2; i++) {
			gen_radicals_of_size(i);
			radicals[i].trimToSize();
			// for (int j = 0; j<radicals[i].size(); j++) {
			//   ((Radical)(radicals[i].elementAt(j))).print();
			// }
		}
	}

	void generate(int n) {
		// used for sequential Version olny
		// generate paraffin molecules
		for (int i = 1; i <= n; i++) {
			generate_molecules_of_size(i);
		}
	}
  
	void generate_molecules_of_size(int size) {
		// generate paraffin isomer molecules
		// of size "size"

		// generate carbon centered molecules for both odd
		// and even size paraffin molecules.
		gen_ccps_of_size(size);
		if (size%2 == 0) {
			// even size molecule: need bond centered paraffins as well
			gen_bcps_of_size(size);
		}
	}

	void gen_radicals_of_size(int size) {
		// generate all radicals of a specified size
		// generates partitions representing subradicals
		// and then actually creates new radicals based
		// on these partitions
		IntArrayEnumeration e = new RecursivePartition(size-1, 3, 0, size);
		int[] partition;
		while ( (partition = e.nextElement()) != null ) {
			// given a partition representing subradical 
			// sizes for a new radical, generate all such
			// new radicals and append them to the radical list.
			enum_rad_tuples(partition, RadKind, size);
		}
	}

	void gen_ccps_of_size(int size) {
		// generate all CCPs of a specified size:
		// generates partitions representing radicals of a CCP
		// and then actually creates new CCP molecules  based
		// on these partitions
		IntArrayEnumeration e = new RecursivePartition(size-1, 4, 0, (size-1)/2);
		int[] partition;
		while ( (partition = e.nextElement()) != null ) {
			// given a partition representing CCP radical
			// sizes for new CCP molecules, generate all such
			// new molecules and append them to the list of molecules.
			enum_rad_tuples(partition, CCPKind, size);
		}
	}
  
	void gen_bcps_of_size(int size) {
		// generate all BCPs of a specified size:
		// generates partitions representing radicals of a BCP
		// and then actually creates new CCP molecules  based
		// on these partitions
		IntArrayEnumeration e = new RecursivePartition(size, 2, size/2, size/2);
		int[] partition;
		while ( (partition = e.nextElement()) != null ) {
			// given a partition representing BCP radical
			// sizes for new BCP molecules, generate all such
			// new molecules and append them to the list of molecules.
			enum_rad_tuples(partition, BCPKind, size);
		}
	}

	void enum_rad_tuples(int[] p, int kind, int size) {
		// enumarates all possible radical tuples with size
		// represented by a partition p.
		Radical[] radical_tuple = new Radical[p.length];
		recursively_enumerate(p,kind,size, radical_tuple, 0,
				      radicals[p[0]],0, p[0]);
	}


        /* p = read only, kind = read-only, size = read-only,
	   radical_tuple = read only, level = read only (but is increased), 
	   remainder = read only */
	/* modifies class variables radicals and counts */
	void recursively_enumerate(int[] p, int kind, int size,
				   Radical[] radical_tuple, int level,
				   Vector remainder, int remainderpos,
				   int prev_element) {
		// the inner recursive procedure for radical tuple enumaration.
		Vector levels_radical_list;

		if (p[level] == prev_element) {
			levels_radical_list = remainder;
		} else {
			levels_radical_list = radicals[p[level]];
			remainderpos = 0;
		}
		// forall (int i = remainderpos; i < levels_radical_list.size(); i++) {
		for (int i = remainderpos; i < levels_radical_list.size(); i++) {
			// for i == remainderpos no new Radical[] is actually needed
			// we do it for potential parallelism.
			Radical[] new_tuple = new Radical[p.length];
			System.arraycopy(radical_tuple,0,new_tuple,0,level); // copy only the first
			new_tuple[level] = (Radical)(levels_radical_list.elementAt(i));
			if (level < p.length-1) {
				recursively_enumerate(p, kind, size, new_tuple, level+1,
						      levels_radical_list, i, p[level]);
			} else {
				apply_to_each(kind, size, new_tuple);
			}
		}
	}

	void apply_to_each(int kind, int size, Radical[] rad) {
		switch(kind) {
		case RadKind:
			radicals[size].addElement(new CarbonRadical(rad));
			return;
		case CCPKind:
			if(PRINT) {
				CCP_Molecule ccpmol = new CCP_Molecule(rad);
				ccpmol.print();
			}
			counts[size]++;
			return;
		case BCPKind:
			if(PRINT) {
				BCP_Molecule bcpmol = new BCP_Molecule(rad);
				bcpmol.print();
			}
			counts[size]++;
			return;
		default:
			throw new RuntimeException("programming error");
		}
	}

	private static void usage() {
		System.out.println("usage: java Main "
				   +"<number of carbon atoms>");
	}

	public static void main(String[] argv) {
		int size = 10;

		if (argv.length == 1) {
			try {
				size = Integer.parseInt(argv[0]);
			} catch (NumberFormatException e) {
				System.out.println("invalid number of carbon atoms");
				usage();
				System.exit(-1);
			}
		} else if (argv.length > 1) {
			usage();
			System.exit(-1);
		}

		long startTime = System.currentTimeMillis();

		new Paraffins(size).work();

		startTime = (System.currentTimeMillis() - startTime);
		System.out.println("Elapsed Time " + startTime/60000 +":"+
				   (startTime%60000)/1000+":"+(startTime%1000));

		System.exit(0);
	}
}
