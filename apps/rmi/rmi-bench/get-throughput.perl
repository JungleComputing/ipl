#! /usr/bin/perl

while ( <> ) {
    if (/ibis = (\S+) ser = (\S+) count = [0-9]+ size = [0-9]+ app-flags = "-one-way *-?(.*)" java-flags = ".*"/) {
	$ibis = $1;
	$ser = $2;
	$datatype = $3;
    } elsif (/ibis = (\S+) ser = (\S+) count = [0-9]+ size = [0-9]+ app-flags = -one-way *-?(.*)/) {
	$ibis = $1;
	$ser = $2;
	$datatype = $3;
    }

    if (/RMI Bench: [0-9]+ ms for ([0-9]+) RMIs of size ([0-9.]+) \S+; ([0-9.]+) us.\/RMI/) {
	$calls = $1;
	$size = $1;
	$lat = $1;
    }

    if (/(payload|Throughput) ([0-9.]*) MB\/s/) {
	$throughput = $2;

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
	foreach ( @ibises ) {
	    if ( $_ eq $ibis ) {
		$found = 1;
		break;
	    }
	}
	if ( ! $found ) {
	    @ibises = sort( @ibises, $ibis );
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

	$ix = $ibis . "/" . $ser . "/" . $datatype;

	$average{ $ix } += $throughput;
	if ($throughput > $max_thrp{ $ix } ) {
	    $max_thrp{ $ix } = $throughput;
	}
	$n { $ix } ++;
    }
}

$marker = "--------------------------------------------------------------------\n";
printf("%-8s %-8s %-24s %12s %12s\n",
    "Ser", "Datatype", "Ibis", "max(MB/s)", "average(MB/s)");
print $marker;
foreach ( @ibises ) {
    $ibis = $_;
    foreach ( @serializations ) {
	$ser = $_;
	foreach ( @datatypes ) {
	    $datatype = $_;
	    $ix = $ibis . "/" . $ser . "/" . $datatype;
	    if ( $n { $ix } > 0) {
		printf("%-8s %-8s %-24s %12.2f %12.2f\n",
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
