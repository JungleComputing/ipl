#! /usr/bin/perl

@serializations = ( "byte", "none", "ibis", "sun" );
@ibises = ( "tcp", "net.bytes.gen.tcp_blk", "panda","net.gm" );

$header = 1;
while ( <> ) {
	if (/ibis = (\S+) ser = (\S+) count = [0-9]+ size = [0-9]+ app-flags =.*/) {
		$ibis = $1;
		$ser  = $2;
	}
	if (/Latency: ([0-9]+) calls took [0-9.]+ s, time\/call = ([0-9.]+) us/) {
		$calls = $1;
		$lat   = $2;

		# printf "ibis $ibis; ser $ser; calls $calls; lat $lat\n";
		$header = 1;

		$ix = $ibis . "/" . $ser;

		$average{ $ix } += $lat;
		$n { $ix } ++;
		if ($min_lat { $ix } == 0 || $lat < $min_lat { $ix } ) {
			$min_lat { $ix } = $lat;
		}
	}
}

printf("%-4s %-24s %-8s %12s %12s\n",
	"N", "Ibis", "Ser", "min(us)", "average(us)");
foreach ( @ibises ) {
	$ibis = $_;
	foreach ( @serializations ) {
		$ser = $_;
		$ix = $ibis . "/" . $ser;
		if ( $n { $ix } > 0) {
			printf("%4d %-24s %-8s %12.1f %12.1f\n",
				$n { $ix },
				"$ibis", "$ser", 
				$min_lat { $ix },
				$average{ $ix } / $n { $ix } );
		}
	}
	printf("-----------------------------------------------------------\n");
}

# foreach ( keys %average ) {
# 	print "$_ : ";
# 	printf "Average ($n{ $_ }) = ";
# 	printf $average{ $_ } / $n { $_ };
# 	printf " us\n";
# }
