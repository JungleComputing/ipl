def sort1( text, indices, offset ):
    """ Returns an index and commonality array sorted by the character
    at the given offset."""
    slot = 256*[-1]
    prev = 256*[-1]
    # For now we leve the range up to start unused.
    next = len(text)*[-1]

    # Fill the hash buckets
    for i in indices:
        if i+offset<len( text ):
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

def isAcceptable1( indices, offset ):
    """ Determine whether the given range of indices is useful for the
    non-overlapping repeat we're searching. Too short or only overlapping
    repeats cause a reject. """
    if len( indices )<2:
        return 0
    for ix in range( len(indices) ):
        for iy in range( ix+1, len( indices ) ):
            if (indices[ix]+offset)<=indices[iy]:
                return 1
    return 0

def isAcceptable( indices, offset ):
    res = isAcceptable1( indices, offset )
    print "offset:", offset, "indices:", indices, "res:", res
    return res

def sort( text ):
    """ Returns the fully sorted text."""
    (indices,comm) = sort1( text, range( len( text ) ), 0 )
    offset = 0
    while 1:
        indices1 = []
        comm1 = []
        ix = 0
        offset += 1
        acceptable = 0
        while ix<len( indices ):
            start = ix
            ix = ix+1
            while ix<len( indices ) and comm[ix] == offset:
                ix = ix+1
            (i1,c1) = sort1( text, indices[start:ix], offset )
            if isAcceptable( i1, offset ):
                acceptable = 1
                c1[0] = comm[start]
                indices1 += i1
                comm1 += c1
        if acceptable == 0:
            break
        indices = indices1
        comm = comm1
    return( offset-1, indices, comm )

def sort2( text ):
    l = range( len( text ) )
    return sort1( text, l, 0 )

text = [0, 1, 0, 1, 2, 3, 3, 4, 2, 1, 0, 1, 2, 3, 3, 4, 2, 1, 0, 1, 2]
#text = 10*[0, 1, 2]
#(ix,comm) = sort2( text )
(offset,ix,comm) = sort( text )

print "offset:", offset, "commonality:", comm
for i in range( len( ix ) ):
    print "[%2d] %2d: %s" % (comm[i], ix[i], text[ix[i]:])
