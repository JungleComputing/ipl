import sys
import string

population = [[ ]]
members = [[ ]]
Tcomm = [[ ]]
Tcomp = [[ ]]
Tadmin = [[ ]]

knownMembers = 0
knownGenerations = -1

def growArray( A, n, v ):
    while len( A )<=n:
        A.append( v )
    return A

def growSubArrays( A, n ):
    for i in range( len( A ) ):
        A[i] = growArray( A[i], n, 0 )
    return A

def growMembers( A, n, gen ):
    return growArray( A, n, (gen+1)*[0] )

def registerLog( arg ):
    global population, members, Tcomm, Tcomp, Tadmin
    global knownMembers, knownGenerations

    f = open( arg )
    while 1:
        l = f.readline()
        if l == '':
            break
        words = l.split()
        if len( words ) != 0 and words[0] == 'STATS':
            [key,SP,gen,mem,pop,tp,tc,ta] = words
            g = int( gen )
            P = int( SP )
            if P>=knownMembers:
                knownMembers = P+1
                population = growMembers( population, knownMembers, knownGenerations )
                members = growMembers( members, knownMembers, knownGenerations )
                Tcomm = growMembers( Tcomm, knownMembers, knownGenerations )
                Tcomp = growMembers( Tcomp, knownMembers, knownGenerations )
                Tadmin = growMembers( Tadmin, knownMembers, knownGenerations )
            if g>knownGenerations:
                knownGenerations = g
                population = growSubArrays( population, g )
                members = growSubArrays( members, g )
                Tcomm = growSubArrays( Tcomm, g )
                Tcomp = growSubArrays( Tcomp, g )
                Tadmin = growSubArrays( Tadmin, g )
            population[P][g] = int( pop )
            members[P][g] = int( mem )
            Tcomm[P][g] = int( tc )
            Tcomp[P][g] = int( tp )
            Tadmin[P][g] = int( ta )

def dumpArray( A ):
    for gen in range( knownGenerations ):
        for P in range( knownMembers ):
            print "%4d " % A[P][gen],
        print  ""

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
    print "Tcomm"
    dumpArray( Tcomm )
    print 
    print "Tcomp"
    dumpArray( Tcomp )
    print 
    print "Tadmin"
    dumpArray( Tadmin )

if __name__ == "__main__":
    main()
