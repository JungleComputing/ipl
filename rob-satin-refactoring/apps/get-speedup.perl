#! /usr/bin/perl

# @serializations = ( "data", "ibis", "sun" );
# @ibises = ( "tcp", "net.bytes.gen.tcp_blk", "panda","net.gm" );
# @nodes = ( 1, 2, 4, 8, 16, 32 );

while ( <> ) {
    chomp;
    # printf "current $_\n";

    if ($_ =~ /ser=(\S+) nodes?=(\S+) ibis=(\S+)/) {
	$ser = $1;
	$node = $2;
	$ibis = $3;

	$found = 0;
	foreach ( @serializations ) {
	    if ( $_ eq $ser ) {
		$found = 1;
		break;
	    }
	}
	if ( ! $found ) {
	    @serializations = sort( @serializations, $ser );
	}

	$found = 0;
	foreach ( @nodes ) {
	    if ( $_ eq $node ) {
		$found = 1;
		break;
	    }
	}
	if ( ! $found ) {
	    @nodes = sort { $a <=> $b } ( @nodes, $node );
	}

	$found = 0;
	foreach ( @ibises ) {
	    if ( $_ eq $ibis ) {
		$found = 1;
		break;
	    }
	}
	if ( ! $found ) {
	    @ibises = sort( @ibises, $ibis );
	}

	# printf "ibis $ibis; ser $ser; node $node\n";
    }

    if (($_ =~ /application .* took [0-9.]+ s/) ||
	    ($_ =~ /Application: .* time: [0-9.]+ s/)) {

	if (/application .* took ([0-9.]+) s/) {
	    # This is a satin app
	    $lat = $1;
	    # printf "Satin: $_";
	} elsif (/Application: .* time: ([0-9.]+) s/) {
	    # This is an RMI app
	    $lat = $1;
	    # printf "RMI: $_";
	}
	# printf "ibis $ibis; ser $ser; lat $lat\n";

	$ix = $ibis . "/" . $ser . "@" . $node;

	$average{ $ix } += $lat;
	$n { $ix } ++;
	if ($min_lat { $ix } == 0 || $lat < $min_lat { $ix } ) {
	    $min_lat { $ix } = $lat;
	}
    }

}

$print_sequential = 1;

if ($print_sequential) {
    printf("*** SEQUENTIAL ***\n");
    printf("%-4s %-24s %-8s %8s %8s %8s\n",
	    "N", "Ibis", "Ser", "min(s)", "av.(s)");
}
$sequential = 0;
$min_sequential = 0;
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
	    if ($min_sequential == 0 || $min_lat { $ix } < $min_sequential ) {
		$min_sequential = $min_lat { $ix };
	    }
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
# $sequential /= $n;
$sequential = $min_sequential;

printf("*** PARALLEL ***\n");
printf("%-4s %-28s %-8s %-4s %7s %7s %7s %7s\n",
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
		    printf("%-4d %-28s %-8s %-4d %7.1f %7.1f %7.3f %7.3f\n",
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
