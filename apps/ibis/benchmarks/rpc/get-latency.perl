#! /usr/bin/perl

while ( <> ) {
    if (/ibis = (\S+) ser = (\S+) count = [0-9]+ size = [0-9]+ app-flags =.*/) {
	$ibis = $1;
	$ser  = $2;
    }
    if (/Latency: ([0-9]+) calls took [0-9.]+ s, time\/call = ([0-9.]+) us/) {
	$calls = $1;
	$lat   = $2;

	# printf "ibis $ibis; ser $ser; calls $calls; lat $lat\n";

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
	foreach ( @ibises ) {
	    if ( $_ eq $ibis ) {
		$found = 1;
		break;
	    }
	}
	if ( ! $found ) {
	    @ibises = sort( @ibises, $ibis );
	}

	$ix = $ibis . "/" . $ser;

	$average{ $ix } += $lat;
	$n { $ix } ++;
	if ($min_lat { $ix } == 0 || $lat < $min_lat { $ix } ) {
	    $min_lat { $ix } = $lat;
	}
    }
}

$marker = "--------------------------------------------------------------------\n";
printf("%-4s %-28s %-8s %12s %12s\n",
    "N", "Ser", "Ibis", "min(us)", "average(us)");
print $marker;
foreach ( @serializations ) {
$ser = $_;
    foreach ( @ibises ) {
	$ibis = $_;
	$ix = $ibis . "/" . $ser;
	if ( $n { $ix } > 0) {
	    printf("%4d %-8s %-28s %12.1f %12.1f\n",
		$n { $ix },
		"$ser", 
		"$ibis",
		$min_lat { $ix },
		$average{ $ix } / $n { $ix } );
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
