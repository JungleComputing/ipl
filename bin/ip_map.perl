#!/usr/bin/perl

$ip_map = $ARGV[0];

# printf "arguments @ARGV\n";

shift @ARGV;
# printf "shifted arguments @ARGV\n";

# printf "Now try to open file \"$ip_map\" to lookup $names\n";

open(IP_FILE, "$ip_map") or die "Cannot open mapping file $ip_map";
while ( <IP_FILE> ) {
    chomp;
    # printf "Now read line \"$_\"\n";
    ( $node, $ip ) = split(' ');
    $mapping{ $node } = $ip;
    # printf "Add mapping [ $node, $ip ]\n";
}

foreach ( @ARGV ) {
    $name = $_;
    # printf "Now lookup $name\n";
    ( $name, $cpu ) = split(/\//, $name);
    # print "name \"$name\" cpu \"$cpu\"\n";
    print "$mapping{ $name }";
    if ( $cpu ne "" ) {
	    print "/$cpu";
    }
    print " ";
}
print "\n";
