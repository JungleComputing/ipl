def sort1( text, next, ix1, comm, p, indices, offset ):
    """ Returns an index and commonality array sorted by the character
    at the given offset."""
    slot = 256*[-1]
    prev = 256*[-1]

    # Fill the hash buckets
    for i in indices:
        if i+offset<len( text ):
            ix = text[i+offset]
            if prev[ix] == -1:
                slot[ix] = i
            else:
                next[prev[ix]] = i
            prev[ix] = i
    for i in prev:
        if i != -1:
            next[i] = -1
    for i in slot:
        j = i
        c = 0
        if j != -1 and next[j] != -1:
            while j != -1:
                ix1[p] = j
                comm[p] = c
                j = next[j]
                c = 1
                p += 1
    return p

def isAcceptable( indices, start, end, offset ):
    """ Determine whether the given list of indices is useful for the
    non-overlapping repeat we're searching. Too short or only overlapping
    repeats cause a reject. """
    if end-start<2:
        return 0
    for ix in range( start, end ):
        for iy in range( ix+1, end ):
            if (indices[ix]+offset)<=indices[iy]:
                return 1
    return 0

def sort( text ):
    """ Returns the fully sorted text."""
    sz = len( text )
    next = sz*[0]
    indices = sz*[0]
    comm = sz*[0]
    offset = 0
    l = sort1( text, next, indices, comm, 0, range( len( text ) ), offset )
    while 1:
        indices1 = sz*[0]
        comm1 = sz*[0]
        ix = 0
        offset += 1
        acceptable = 0
        p = 0
        while ix<len( indices ):
            start = ix
            oldp = p
            ix = ix+1
            while ix<len( indices ) and comm[ix] != 0:
                ix = ix+1
            p = sort1( text, next, indices1, comm1, p, indices[start:ix], offset )
            if isAcceptable( indices1, oldp, p, offset ):
                acceptable = 1
                comm1[oldp] = 0
        if acceptable == 0:
            break
        indices = indices1
        comm = comm1
        l = p
    return( offset-1, l, indices, comm )

def sort2( text ):
    l = range( len( text ) )
    return sort1( text, l, 0 )

text = [0, 1, 0, 1, 2, 3, 3, 4, 2, 1, 0, 1, 2, 3, 3, 4, 2, 1, 0, 1, 2, 2, 3,3,3]
#text = 10*[0, 1, 2] + [0,1]
#(ix,comm) = sort2( text )
(offset,l,ix,comm) = sort( text )

print "offset:", offset, "commonality:", comm[0:l]
for i in range( l ):
    print "[%2d] %2d: %s" % (comm[i], ix[i], text[ix[i]:])
