#    G N U P L O T
#    Linux version 3.5 (pre 3.6)
#    patchlevel beta 347
#    last modified Mon Jun 22 13:22:33 BST 1998
#
#    Copyright(C) 1986 - 1993, 1998
#    Thomas Williams, Colin Kelley and many others
#
#    Send comments and requests for help to info-gnuplot@dartmouth.edu
#    Send bugs, suggestions and mods to bug-gnuplot@dartmouth.edu
#
# set terminal x11 
# set output

# for latex
#set terminal postscript eps "Helvetica" 17

set terminal postscript "Helvetica" 17
set noclip points
set clip one
set noclip two
set bar 1.000000
set border 31
set xdata
set ydata
set zdata
set x2data
set y2data
set boxwidth
set dummy x,y
set format x "%g"
set format y "%g"
set format x2 "%g"
set format y2 "%g"
set format z "%g"
set grid
set key title ""
set key left top Right noreverse box linetype -2 linewidth 1.000 samplen 4 spacing 1 width 0
set nolabel
set noarrow
set nolinestyle
set nologscale
set offsets 0, 0, 0, 0
set pointsize 1
set encoding default
set nopolar
set angles radians
set noparametric
set view 60, 30, 1, 1
set samples 100, 100
set isosamples 10, 10
set surface
set nocontour
set clabel '%8.3g'
set nohidden3d
set cntrparam order 4
set cntrparam linear
set cntrparam levels auto 5
set cntrparam points 5
set size ratio 0 1,1
set origin 0,0
set data style points
set function style lines
set xzeroaxis lt -2 lw 1.000
set x2zeroaxis lt -2 lw 1.000
set yzeroaxis lt -2 lw 1.000
set y2zeroaxis lt -2 lw 1.000
set tics in
set ticslevel 0.5
set ticscale 1 0.5
set mxtics default
set mytics default
set mx2tics default
set my2tics default
set xtics border mirror norotate 
set ytics border mirror norotate 
set ztics border nomirror norotate 
set nox2tics
set noy2tics
set title "" 0.000000,0.000000  ""
set timestamp "" bottom norotate 0.000000,0.000000  ""
set rrange [ * : * ] noreverse nowriteback  # (currently [-0:10] )
set trange [ * : * ] noreverse nowriteback  # (currently [-5:5] )
set urange [ * : * ] noreverse nowriteback  # (currently [-5:5] )
set vrange [ * : * ] noreverse nowriteback  # (currently [-5:5] )
set xlabel "" 0.000000,0.000000  ""
set x2label "" 0.000000,0.000000  ""
set timefmt "%d/%m/%y\n%H:%M"
set xrange [ 1 : 64 ] noreverse nowriteback  # (currently [-10:10] )
set x2range [ * : * ] noreverse nowriteback  # (currently [-10:10] )
set ylabel "" 0.000000,0.000000  ""
set y2label "" 0.000000,0.000000  ""
set yrange [ 0 : 70 ] noreverse nowriteback  # (currently [-10:10] )
set y2range [ * : * ] noreverse nowriteback  # (currently [-10:10] )
set zlabel "" 0.000000,0.000000  ""
set zrange [ * : * ] noreverse nowriteback  # (currently [-10:10] )
set zero 1e-08
set lmargin -1
set bmargin -1
set rmargin -1
set tmargin -1
set locale "C"
plot \
"gnuplot-fib-speedup-seq.dat" title "fib" with linespoints, \
"gnuplot-tsp-speedup-seq.dat" title "tsp" with linespoints, \
"gnuplot-ida-speedup-seq.dat" title "ida" with linespoints, \
"gnuplot-knapsack-speedup-seq.dat" title "knapsack" with linespoints, \
"gnuplot-mmult-speedup-seq.dat" title "mmult" with linespoints, \
"gnuplot-nqueens-speedup-seq.dat" title "nqueens" with linespoints, \
"gnuplot-cover-speedup-seq.dat" title "cover" with linespoints, \
"gnuplot-noverk-speedup-seq.dat" title "noverk" with linespoints, \
"gnuplot-adapint-speedup-seq.dat" title "adapint" with linespoints, \
"gnuplot-fib_threshold-speedup-seq.dat" title "fib_threshold" with linespoints, \
"gnuplot-primfac-speedup-seq.dat" title "primfac" with linespoints, \
"gnuplot-raytracer-speedup-seq.dat" title "raytracer" with linespoints
#    EOF
