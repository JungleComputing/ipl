#!/usr/bin/perl

$ip_map = $ARGV[0];
$name = $ARGV[1];

# printf "Now try to open file \"$ip_map\" to lookup $names\n";

open(IP_FILE, "$ip_map") or die "Cannot open mapping file $ip_map";
while ( <IP_FILE> ) {
    chomp;
    # printf "Now read line \"$_\"\n";
    ( $node, $ip ) = split(' ');
    $mapping{ $node } = $ip;
    # printf "Add mapping [ $node, $ip ]\n";
}

# printf "Now lookup $name\n";
print "$mapping{ $name }\n";
