#! /usr/bin/perl

@ibises = ( "tcp", "tcp-ip-myri", "net.bytes.gen.tcp_blk", "net.bytes.gen.tcp-ip-myri_blk", "panda", "net.gm", "SUN", "SUN-ip-myri" );

while ( <> ) {
    if (/ibis = (\S+) ser = (\S+) count = [0-9]+ size = [0-9]+ app-flags =.*/) {
	$ibis = $1;
	$ser  = $2;
    }
    if (/RMI Bench: [0-9]+ ms. for ([0-9]+) RMIs of size ([0-9]+) bytes; ([0-9.]+) us.\/RMI/) {
	$calls = $1;
	$size  = $2;
	$lat   = $3;
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

$marker = "---------------------------------------------------------------------\n";
printf("%4s %-30s %-8s %12s %12s\n",
	"N", "Ibis", "Ser", "min(us)", "average(us)");
print $marker;
foreach ( @ibises ) {
    $ibis = $_;
    foreach ( @serializations ) {
	$ser = $_;
	$ix = $ibis . "/" . $ser;
	if ( $n { $ix } > 0) {
	    printf("%4d %-30s %-8s %12.1f %12.1f\n",
		    $n{ $ix }, "$ibis", "$ser", 
		    $min_lat { $ix },
		    $average{ $ix } / $n { $ix } );
	}
    }
    print $marker;
}

# foreach ( keys %average ) {
# 	print "$_ : ";
# 	printf "Average ($n{ $_ }) = ";
# 	printf $average{ $_ } / $n { $_ };
# 	printf " us\n";
# }
