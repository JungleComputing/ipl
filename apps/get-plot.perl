#! /usr/bin/perl

# @serializations = ( "data", "ibis", "sun" );
# @ibises = ( "tcp", "net.bytes.gen.tcp_blk", "panda","net.gm" );
# @nodes = ( 1, 2, 4, 8, 16, 32 );

while ( <> ) {
    chomp;
    # printf "current $_\n";
    ( $runs, $ibis, $ser, $node, $min, $av, $speedup, $eff ) = split;
    # printf ( "Runs $runs Ibis $ibis Serialization $ser Nodes $node\n");

    if ( $node =~ /^[0-9]+$/) {
	$ix = $ibis . "/" . $ser . "@" . $node;

	$data{ $ix } += $speedup;
	$n { $ix } ++;
	$archx = $ibis . "/" . $ser;
	$archn { $archx } ++;

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

    }
}

mkdir "plots" or die "Cannot mkdir plots";

foreach ( "tcp", "myrinet" ) {
    $network = $_;
    open PLOT, ">plots/plot-$network.plot" or die "Cannot open/w 'plot-$network.plot'";
    printf PLOT ("set terminal postscript\n");
    printf PLOT ("set output \"plot-$network.ps\"\n");
    printf PLOT ("set xrange \[0:32\]\n");
    printf PLOT ("set yrange \[0:32\]\n");
    printf PLOT ("plot \\\n");
    $my_first = 1;
    foreach ( @serializations ) {
	$ser = $_;
	foreach ( @ibises ) {
	    $ibis = $_;
	    $archx = $ibis . "/" . $ser;
	    if ($archn { $archx } > 0) {
		if ( ( $network eq "tcp" && $ibis =~ /(tcp|sun)/ ) ||
		     ( $network ne "tcp" && $ibis !~ /(tcp|sun)/ ) ) {
		    $name = "plots/plot-$ibis-$ser.data";
		    if ($my_first) {
			$my_first = 0;
			printf PLOT "	";
		    } else {
			printf PLOT "	, ";
		    }
		    printf PLOT "\'$name\' with lines \\\n";
		    open DATA, ">$name" or die "Cannot open '$name'";
		    foreach ( @nodes ) {
			$node = $_;
			$ix = $ibis . "/" . $ser . "@" . $node;
			if ( $n { $ix } > 0) {
			    printf DATA ("%4d %8.3f\n", $node, $data{ $ix });
			}
		    }
		    close DATA;
		}
	    }
	}
    }
    close PLOT;
}

foreach ( "tcp", "myrinet" ) {
    system "gnuplot", "plots/plot-$_.plot";
}
