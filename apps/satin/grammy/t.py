def sort1( text, start, end, offset ):
    slot = 256*[-1]
    prev = 256*[-1]
    # For now we leve the range up to start unused.
    next = end*[-1]

    # Fill the hash buckets
    for i in range( start, end ):
        ix = text[i+offset]
        if prev[ix] == -1:
            slot[ix] = i
        else:
            next[prev[ix]] = i
        prev[ix] = i
    res = []
    comm = []
    for i in slot:
        j = i
        c = offset
        if j != -1 and next[j] != -1:
            while j != -1:
                res.append( j )
                comm.append( c )
                j = next[j]
                c = offset+1
    return (res, comm)

def sort( text ):
    return sort1( text, 0, len( text ), 0 )

text = [0, 1, 0, 1, 2, 3, 3, 4, 2, 1, 0, 1, 2]
(ix,comm) = sort( text )

print comm
for i in ix:
    print "%s: %s" % (i, text[i:])


