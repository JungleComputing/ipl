#! /usr/bin/perl

@datatypes = ( "byte", "int", "double", "tree" );
@serializations = ( "none", "ibis", "sun" );
@ibises = ( "tcp", "net.bytes.gen.tcp_blk", "panda","net.gm" );

$header = 1;
while ( <> ) {
	if ( $header == 1) {
		$line1 = $_;
		++$header;
		next;
	}
	if ( $header == 2) {
		$line2 = $_;
		$header = 0;
		next;
	}
	# printf "header $line current $_\n";
	$header = 1;
	( $ibis, $ser, $datatype ) = ($line1 =~ /ibis = (\S+) ser = (\S+) count = [0-9]+ size = [0-9]+ app-flags = *-*(.*)/);
	( $calls, $lat ) = ($line2 =~ /Latency: ([0-9]+) calls took [0-9.]+ s, time\/call = ([0-9.]+) us/);
	( $throughput ) = ($_ =~ /Throughput ([0-9.]*) MB\/s/);
	if ($datatype eq "") {
		$datatype = "byte";
	}

	# printf "ibis $ibis; ser $ser; datatype $datatype; calls $calls; thrp $throughput\n";

	$ix = $ibis . "/" . $ser . "/" . $datatype;

	$average{ $ix } += $throughput;
	if ($throughput > $max_thrp{ $ix } ) {
		$max_thrp{ $ix } = $throughput;
	}
	$n { $ix } ++;
}

printf "Ser\t Datatype\t Ibis\t max(MB/s)\t average(MB/s)\n";
foreach ( @serializations ) {
	$ser = $_;
	foreach ( @datatypes ) {
		$datatype = $_;
		foreach ( @ibises ) {
			$ibis = $_;
			$ix = $ibis . "/" . $ser . "/" . $datatype;
			if ( $n { $ix } > 0) {
				printf "$ser\t $datatype\t $ibis\t ";
				printf $max_thrp{ $ix };
				printf "\t ";
				printf $average{ $ix } / $n { $ix };
				printf "\n";
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
