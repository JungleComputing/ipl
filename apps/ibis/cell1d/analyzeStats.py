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
sentLeft = []
sentRight = []
Tcomm = []
Tcomp = []
Tadmin = []
stealLeft = []
stealRight = []

knownMembers = 0
knownGenerations = -1

def registerLog( arg ):
    global population, members, sentLeft, sentRight, Tcomm, Tcomp, Tadmin, stealLeft, stealRight
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
                sentLeft = growSubArrays( sentLeft, g )
                sentRight = growSubArrays( sentRight, g )
                Tcomm = growSubArrays( Tcomm, g )
                Tcomp = growSubArrays( Tcomp, g )
                Tadmin = growSubArrays( Tadmin, g )
                stealLeft = growSubArrays( stealLeft, g )
                stealRight = growSubArrays( stealRight, g )
            if P>=knownMembers:
                knownMembers = P+1
                population = growMembers( population, knownMembers, knownGenerations )
                members = growMembers( members, knownMembers, knownGenerations )
                sentLeft = growMembers( sentLeft, knownMembers, knownGenerations )
                sentRight = growMembers( sentRight, knownMembers, knownGenerations )
                Tcomm = growMembers( Tcomm, knownMembers, knownGenerations )
                Tcomp = growMembers( Tcomp, knownMembers, knownGenerations )
                Tadmin = growMembers( Tadmin, knownMembers, knownGenerations )
                stealLeft = growMembers( stealLeft, knownMembers, knownGenerations )
                stealRight = growMembers( stealRight, knownMembers, knownGenerations )
            population[P][g] = int( pop )
            members[P][g] = int( mem )
            sentLeft[P][g] = int( sl )
            sentRight[P][g] = int( sr )
            Tcomp[P][g] = int( tp )
            Tcomm[P][g] = int( tc )
            Tadmin[P][g] = int( ta )
            stealLeft[P][g] = int( stl )
            stealRight[P][g] = int( str )

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
    print "sentLeft"
    dumpArray( sentLeft )
    print
    print "sentRight"
    dumpArray( sentRight )
    print 
    print "Tcomp"
    dumpArray( Tcomp )
    print 
    print "Tcomm"
    dumpArray( Tcomm )
    print 
    print "Tadmin"
    dumpArray( Tadmin )
    print 
    print "stealLeft"
    dumpArray( stealLeft )
    print 
    print "stealRight"
    dumpArray( stealRight )

if __name__ == "__main__":
    main()
