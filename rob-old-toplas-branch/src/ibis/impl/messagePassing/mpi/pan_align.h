#ifndef __IBIS_IMPL_MP_MPI_PAN_ALIGN_H__
#define __IBIS_IMPL_MP_MPI_PAN_ALIGN_H__


#include <stddef.h>


					/* Must write -1 + (n) since (n) - 1
					 * translates to (int)(-1), which
					 * would be legal */
#define align_of(tp)		offsetof(struct{char __a; tp __tp;}, __tp)
#define do_align(a, n)		(((a) - 1 + (n)) & ~(-1 + (n)))
#define aligned(a, n)		(((a) & (-1 + (n))) == 0)
#define align_to(a, tp) 	do_align(a, align_of(tp))
#define aligned_to(a, tp)	aligned(a, align_of(tp))

#define pan_2log(n) \
    ((n) == 0 ? -1 : \
     ((n) == (1 << 0) ? 0 : \
      ((n) == (1 << 1) ? 1 : \
       ((n) == (1 << 2) ? 2 : \
	((n) == (1 << 3) ? 3 : \
	 ((n) == (1 << 4) ? 4 : \
	  ((n) == (1 << 5) ? 5 : \
	   ((n) == (1 << 6) ? 6 : \
	    ((n) == (1 << 7) ? 7 : \
	     ((n) == (1 << 8) ? 8 : \
	      ((n) == (1 << 9) ? 9 : \
	       ((n) == (1 << 10) ? 10 : \
		((n) == (1 << 11) ? 11 : \
		 ((n) == (1 << 12) ? 12 : \
		  ((n) == (1 << 13) ? 13 : \
		   ((n) == (1 << 14) ? 14 : \
		    ((n) == (1 << 15) ? 15 : \
		     ((n) == (1 << 16) ? 16 : \
		      ((n) == (1 << 17) ? 17 : \
		       ((n) == (1 << 18) ? 18 : \
			((n) == (1 << 19) ? 19 : \
			 ((n) == (1 << 20) ? 20 : \
			  ((n) == (1 << 21) ? 21 : \
			   ((n) == (1 << 22) ? 22 : \
			    ((n) == (1 << 23) ? 23 : \
			     ((n) == (1 << 24) ? 24 : \
			      ((n) == (1 << 25) ? 25 : \
			       ((n) == (1 << 26) ? 26 : \
				((n) == (1 << 27) ? 27 : \
				 ((n) == (1 << 28) ? 28 : \
				  ((n) == (1 << 29) ? 29 : \
				   ((n) == (1 << 30) ? 30 : \
				    ((n) == (1 << 31) ? 31 : \
				     abort(), 0)))))))))))))))))))))))))))))))))

#define pan_upround_2log(n, r)	((((n) + (1 << r) - 1) >> (r)) << (r))
#define pan_upround_2pow(n, r)	pan_upround_2log(n, pan_2log(r))

#endif
