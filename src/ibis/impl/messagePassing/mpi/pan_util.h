/*
%(c) copyright 1995 by the Vrije Universiteit, Amsterdam, The Netherlands.
%For full copyright and restrictions on use see the file COPYRIGHT in the
%top level of the Panda distribution.
 */

#ifndef __IBIS_IPL_IMPL_MP_MPI_PAN_UTIL_H__
#define __IBIS_IPL_IMPL_MP_MPI_PAN_UTIL_H__

int	pan_arg_match(const char *option, const char *arg);
int	pan_narg_match(const char *option, const char *arg, int n);

/*
With these functions, '_' and '-' are equivalent within (argument) strings
*/

int pan_arg_string(int *argc, char *argv[], const char *option, char **val);
int pan_arg_bool(int *argc, char *argv[], const char *option);
int pan_arg_int(int *argc, char *argv[], const char *option, int *val);
int pan_arg_double(int *argc, char *argv[], const char *option, double *val);
int pan_arg_int_N(int *argc, char *argv[], const char *option, int n, int *val);

/*
Query whether an option is set either on the command line (lower case, with
either '_' or '-') or in the environment (upper case, with '-').
These functions return 1 when the option is present and its value is valid,
0 when the option is absent, and -1 when the option is present and is value
is invalid.
*/

#endif


/*
%Local Variables:
%c-block-comments-indent-p: 5
%c-comment-leader: ""
%End:
*/
