#! /usr/bin/perl

@serializations = ( "data", "ibis", "sun" );
@ibises = ( "tcp", "net.bytes.gen.tcp_blk", "panda","net.gm" );
@nodes = ( 1, 2, 4, 8, 16, 32 );

while ( <> ) {
    chomp;
    # printf "current $_\n";
    if ($_ =~ /ser=(\S+) node=(\S+) ibis=(\S+)/) {
	$ser = $1;
	$node = $2;
	$ibis = $3;
	# printf "ibis $ibis; ser $ser; node $node\n";
    }
    if ($_ =~ /SOR ([0-9]+) x ([0-9]+) took ([0-9.]+) sec\./) {
	$rows = $1;
	$colls = $2;
	$lat = $3;
	# printf "ibis $ibis; ser $ser; rows $rows; colls $colls; lat $lat\n";

	$ix = $ibis . "/" . $ser . "@" . $node;

	$average{ $ix } += $lat;
	$n { $ix } ++;
	if ($min_lat { $ix } == 0 || $lat < $min_lat { $ix } ) {
	    $min_lat { $ix } = $lat;
	}
    }
}

if ($print_sequential) {
    printf("*** SEQUENTIAL ***\n");
    printf("%-4s %-24s %-8s %8s %8s %8s\n",
	    "N", "Ibis", "Ser", "min(s)", "av.(s)");
}
$sequential = 0;
$n = 0;
foreach ( @ibises ) {
    $ibis = $_;
    foreach ( @serializations ) {
	$ser = $_;
	$node = 1;
	$ix = $ibis . "/" . $ser . "@" . $node;
	if ( $n { $ix } > 0) {
	    $sequential += $min_lat { $ix };
	    $n++;
	    if ($print_sequential) {
		printf("%-4d %-24s %-8s %8.1f %8.1f\n",
			$n { $ix },
			"$ibis", "$ser", 
			$min_lat { $ix },
			$average{ $ix } / $n { $ix });
	    }
	}
    }
}
$sequential /= $n;

printf("*** PARALLEL ***\n");
printf("%-4s %-24s %-8s %-4s %8s %8s %8s %8s\n",
	"N", "Ibis", "Ser", "cpus", "min(us)", "av.(us)", "speedup", "eff.");
foreach ( @ibises ) {
    $ibis = $_;
    foreach ( @serializations ) {
	$ser = $_;
	foreach ( @nodes ) {
	    $node = $_;
	    $ix = $ibis . "/" . $ser . "@" . $node;
	    if ($node != 1) {
		if ( $n { $ix } > 0) {
		    $speedup = $sequential / $min_lat { $ix };
		    printf("%-4d %-24s %-8s %-4d %8.1f %8.1f %8.3f %8.3f\n",
			    $n { $ix },
			    "$ibis", "$ser", 
			    $node,
			    $min_lat { $ix },
			    $average{ $ix } / $n { $ix },
			    $speedup,
			    $speedup / $node);
		}
	    }
	}
    }
}

# foreach ( keys %average ) {
# 	print "$_ : ";
# 	printf "Average ($n{ $_ }) = ";
# 	printf $average{ $_ } / $n { $_ };
# 	printf " us\n";
# }
