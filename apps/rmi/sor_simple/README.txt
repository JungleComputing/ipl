Hai,

> 
> Ah, mooi.  Hoe bepaalt hij het totaal aantal nodes?  Environment
> parameter (welke?) of commandline?
> 

Je kunt het totaal aantal machines in een run en de rank van een machine aan ibis meegeven met 

-Dibis.pool.total_hosts=XXX
-Dibis.pool.host_number=YYY

Dit zijn algemene ibis opties. Sommige ibis applicaties gebruiken ze,
andere niet.  Satin gebruikt alleen de eerste, en dan nog alleen als
je met een closed world draait. RMI gebruikt volgens mij geen van
beide. Maar andere ibis applicaties (GMI applicaties bv) gebruiken ze
weer wel.  Het hangt dus een beetje van de situatie af of je deze
opties per se mee moet geven of niet. Het mag natuurlijk altijd.


> 
> O, ik dacht dat dat sor_grid voorbeeld wegens dat pool object
> weer aparte servers, environment parameters, etc nodig zou
> kunnen hebben.  Maar misschien valt het dus erg mee :-)
> 

Nou, niet helemaal :-) Die SOR is een RMI programma. Het werkt zowel
met als zonder ibis. Daarom kan SOR dus geen gebruik maken van ibis
specifieke dingen als de rank en size parameters.  Voor RMI moet je
dit dus allemaal nog een keer op applicatie niveau doen. Dat doet SOR
inderdaad met een extra server.  Dus, in dat geval moet je weer andere
properties mee geven...

Eerst even als "gewone" java applicatie: er is dus helemaal geen ibis nodig:

Compilen met:

rm -f *.class; javac *.java; rmic GlobalData SOR

Een pool server draaien met:

java PoolInfoServer <PORT_NR>

Dit is dus een java servertje, heeft niets met ibis te maken.
Je hebt geen ibis nameserver nodig.

Ik draai nu als volgt:

prun -1 -v ~/projects/ibis/bin/run_ibis 2 9988 fs0.das2.cs.vu.nl -Dpool.total_hosts=2 -Dpool.server.host=fs0.das2.cs.vu.nl -Dpool.server.port=9977 Main 500 500

Zoals je kunt zien heb je dus extra properties nodig. Dit is nodig
voor die RMI pool server. Ik draai voor het gemak altijd met run_ibis,
maar dat is dus niet nodig.



OK, nu met ibis:

Compilen:

rm -f *.class; ant clean; ant

Een pool server draaien met:

java -classpath build PoolInfoServer <PORT_NR>

Ibis nameserver starten:

ibis_nameserver -port 9988

(of natuurlijk een andere port)

en draaien met:

prun -1 -v ~/projects/ibis/bin/run_ibis 2 9988 fs0.das2.cs.vu.nl -Dpool.total_hosts=2 -Dpool.server.host=fs0.das2.cs.vu.nl -Dpool.server.port=9977 Main 500 500

Je hebt met ibis dus helaas 2 nameservers nodig. Een die in de
applicatie zelf zit, en een ibis nameserver.

Dat moet helaas wel, omdat RMI nu eenmaal niets weet van pools of
ranks of wat dan ook. Het is zuiver client/server, point to point.

succes!

Rob
