#! /usr/bin/perl

# @datatypes = ( "byte", "int", "double", "tree" );
# @serializations = ( "byte", "ibis", "sun" );
# @ibises = ( "tcp", "net.bytes.gen.tcp_blk", "net.multi.bytes.gen.tcp_blk", "panda", "net.gm", "net.multi.gm" );

$header = 1;
while ( <> ) {
    if (/ibis = (\S+) ser = (\S+) count = [0-9]+ size = [0-9]+ app-flags = "-?(.*?)" java-flags = ".*"/) {
	$ibis     = $1;
	$ser      = $2;
	$datatype = $3;
	# printf "ibis $ibis; ser $ser; datatype $datatype\n";
    } elsif (/ibis = (\S+) ser = (\S+) count = [0-9]+ size = [0-9]+ app-flags = -?(.*?)/) {
	$ibis     = $1;
	$ser      = $2;
	$datatype = $3;
	# printf "ibis $ibis; ser $ser; datatype $datatype\n";
    }
    if (/Latency: ([0-9]+) calls took [0-9.]+ s, time\/call = ([0-9.]+) us/) {
	$calls = $1;
	$lat   = $2;
    }
    if (/Throughput ([0-9.]*) MB\/s/) {
	$throughput = $1;
	if ($datatype eq "") {
	    $datatype = "byte";
	}

	# printf "ibis $ibis; ser $ser; datatype $datatype; calls $calls; thrp $throughput\n";

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
	foreach ( @datatypes ) {
	    if ( $_ eq $datatype ) {
		$found = 1;
		break;
	    }
	}
	if ( ! $found ) {
	    @datatypes = sort( @datatypes, $datatype );
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

	$ix = $ibis . "/" . $ser . "/" . $datatype;

	$average{ $ix } += $throughput;
	if ($throughput > $max_thrp{ $ix } ) {
	    $max_thrp{ $ix } = $throughput;
	}
	$n { $ix } ++;
    }
}

$marker="-----------------------------------------------------------------------------\n";
printf("%4s %-8s %-8s %-28s %12s %12s\n",
    "N", "Ser", "Datatype", "Ibis", "max(MB/s)", "average(MB/s)");
print $marker;
foreach ( @serializations ) {
    $ser = $_;
    foreach ( @datatypes ) {
	$datatype = $_;
	foreach ( @ibises ) {
	    $ibis = $_;
	    $ix = $ibis . "/" . $ser . "/" . $datatype;
	    if ( $n { $ix } > 0) {
		printf("%4d %-8s %-8s %-28s %12.1f %12.1f\n",
		    $n { $ix },
		    "$ser", "$datatype", "$ibis",
		    $max_thrp{ $ix },
		    $average{ $ix } / $n { $ix });
	    }
	}
    }
    print $marker;
}

# foreach ( keys %average ) {
#     print "$_ : ";
#     printf "Average ($n{ $_ }) = ";
#     printf $average{ $_ } / $n { $_ };
#     printf " us\n";
# }
