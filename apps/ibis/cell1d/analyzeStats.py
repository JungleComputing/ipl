import sys
import string

def growArray( A, n, v ):
    while len( A )<=n:
        A.append( v )
    return A

def growSubArrays( A, n ):
    for i in range( len( A ) ):
        A[i] = growArray( A[i], n, -1  )
    return A

def growMembers( A, n, gen ):
    while len( A )<=n:
        A.append( (gen+1)*[-n] )
    return A

population = []
members = []
sentToLeft = []
sentToRight = []
Tcomm = []
Tcomp = []
Tadmin = []
requestedByLeft = []
requestedByRight = []

knownMembers = 0
knownGenerations = -1

def registerLog( arg ):
    global population, members, sentToLeft, sentToRight, Tcomm, Tcomp, Tadmin, requestedByLeft, requestedByRight
    global knownMembers, knownGenerations

    f = open( arg )
    while 1:
        l = f.readline()
        if l == '':
            break
        words = l.split()
        if len( words ) != 0 and words[0] == 'STATS':
            [key,SP,gen,mem,pop,sl,sr,tp,tc,ta,stl,str] = words
            g = int( gen )
            P = int( SP )
            if g>knownGenerations:
                knownGenerations = g
                population = growSubArrays( population, g )
                members = growSubArrays( members, g )
                sentToLeft = growSubArrays( sentToLeft, g )
                sentToRight = growSubArrays( sentToRight, g )
                Tcomm = growSubArrays( Tcomm, g )
                Tcomp = growSubArrays( Tcomp, g )
                Tadmin = growSubArrays( Tadmin, g )
                requestedByLeft = growSubArrays( requestedByLeft, g )
                requestedByRight = growSubArrays( requestedByRight, g )
            if P>=knownMembers:
                knownMembers = P+1
                population = growMembers( population, knownMembers, knownGenerations )
                members = growMembers( members, knownMembers, knownGenerations )
                sentToLeft = growMembers( sentToLeft, knownMembers, knownGenerations )
                sentToRight = growMembers( sentToRight, knownMembers, knownGenerations )
                Tcomm = growMembers( Tcomm, knownMembers, knownGenerations )
                Tcomp = growMembers( Tcomp, knownMembers, knownGenerations )
                Tadmin = growMembers( Tadmin, knownMembers, knownGenerations )
                requestedByLeft = growMembers( requestedByLeft, knownMembers, knownGenerations )
                requestedByRight = growMembers( requestedByRight, knownMembers, knownGenerations )
            population[P][g] = int( pop )
            members[P][g] = int( mem )
            sentToLeft[P][g] = int( sl )
            sentToRight[P][g] = int( sr )
            Tcomp[P][g] = int( tp )
            Tcomm[P][g] = int( tc )
            Tadmin[P][g] = int( ta )
            requestedByLeft[P][g] = int( stl )
            requestedByRight[P][g] = int( str )

def isRepeat( A, gen ):
    for P in range( knownMembers ):
        if A[P][gen-1] != A[P][gen]:
            return 0
    return 1

def dumpArray( A ):
    repeats = 0
    for gen in range( knownGenerations ):
        if gen>0 and isRepeat( A, gen ):
            repeats = repeats+1
        else:
            if repeats>0:
                print "(repeated %d times)" % repeats
            repeats = 0
            print "%3d:" % gen,
            for P in range( knownMembers ):
                print "%4d " % A[P][gen],
            print  ""
    if repeats>0:
        print "(repeated %d times)" % repeats

def main():
    for arg in sys.argv[1:]:
        registerLog( arg )
    print 
    print "population"
    dumpArray( population )
    print 
    print "members"
    dumpArray( members )
    print 
    print "requestedByLeft"
    dumpArray( requestedByLeft )
    print
    print "sentToLeft"
    dumpArray( sentToLeft )
    print 
    print "requestedByRight"
    dumpArray( requestedByRight )
    print
    print "sentToRight"
    dumpArray( sentToRight )
    print 
    print "Tcomp"
    dumpArray( Tcomp )
    print 
    print "Tcomm"
    dumpArray( Tcomm )
    print 
    print "Tadmin"
    dumpArray( Tadmin )

if __name__ == "__main__":
    main()
