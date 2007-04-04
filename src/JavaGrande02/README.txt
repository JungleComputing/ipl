This benchmark program can be used to measure latency and throughput
for several data types. See the program for the currently recognized
options.

The possible program options are:

-ibis
    Use Ibis serialization instead of sun serialization.

-upcalls
    Receive by means of upcalls.

-len <len>
    numbes of elements in trees, lists, dlists, oarrays. Default is 1023.

-arraysize <size>
    sets array size (see the -array option). Default is 16K.

-array
    Send arrays of bytes, ints, and doubles. Total size of the arrays is <size>.

-objectarray
    Send arrays of <len> objects with elements of type "Data".

-list
    Send a linked list of <len> nodes, each node 4 integers 
    and a "next" reference.

-dlist
    Send a doubly linked list of <len> nodes, each node 4 integers,
    a "prev" reference and a "next" reference.

-tree
    Send a binary tree with <size> nodes, each node containing 4 integers and a
    "left" and "right" reference.

-twoway
    Send data twoway, the server sends back the data it receives.
    (the default is that the server sends back an empty answer).

-count <count>
    The benchmark constists of sending <count> messages (the default
    is 10000).
    
-retries <retries>
    Do the benchmark <retries> times (default is 10).

-stream
    Do streaming sends: No answer is sent.
