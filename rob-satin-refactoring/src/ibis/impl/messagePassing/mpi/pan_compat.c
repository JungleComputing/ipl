/*
 * (c) copyright 1998 by the Vrije Universiteit, Amsterdam, The Netherlands.
 * For full copyright and restrictions on use see the file COPYRIGHT in the
 * top level of the Panda distribution.
 */

#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include <stdio.h>
#include <ctype.h>
#if defined __SVR4 || defined _SYSTYPE_SVR4
#include <alloca.h>
#endif

#include "pan_sys.h"

#include "ibmpi_mp.h"


void *
pan_malloc(size_t size)
{
    return malloc(size);
}


void
pan_free(void *ptr)
{
    free(ptr);
}


void *
pan_realloc(void *ptr, size_t size)
{
    return realloc(ptr, size);
}


void *
pan_calloc(size_t elts, size_t size)
{
    return calloc(elts, size);
}


int
pan_thread_nonblocking(void)
{
    return 0;
}


#include "pan_util.h"

static int pan_opt_verbose(void)
{
    return 0;
}


static char *
pan_arg_env_case_match(const char *arg)
{
    char       *other;
    char       *val;
    int		len;
    int		i;

    val = getenv(arg);
    if (val != NULL) {
	return val;
    }
    len = strlen(arg);
    other = alloca(len + 1);
    for (i = 0; i < len; i++) {
	if (arg[i] == '-') {
	    other[i] = '_';
	} else {
	    other[i] = toupper(arg[i]);
	}
    }
    other[len] = '\0';

    return getenv(other);
}


static char *
pan_arg_env_match(const char *arg)
{
    char       *other;
    char       *val;
    int		len;
    int		i;

    arg++;		/* Skip the leading '-' */
    val = getenv(arg);
    if (val != NULL) {
	return val;
    }
    len = strlen(arg);
    other = alloca(len + 1);
    for (i = 0; i < len; i++) {
	if (arg[i] == '-') {
	    other[i] = '_';
	} else {
	    other[i] = arg[i];
	}
    }
    other[len] = '\0';

    val = getenv(other);
    if (val != NULL) {
	return val;
    }

    return pan_arg_env_case_match(arg);
}


int
pan_arg_match(const char *option, const char *arg)
{
    char *other;
    char *hit;

    if (strcmp(option, arg) == 0) {
	return 1;
    }
    other = strdup(option);
    while ((hit = strchr(other + 1, '-')) != NULL) {
	*hit = '_';
    }
    if (strcmp(other, arg) == 0) {
	free(other);
	return 1;
    }
    while ((hit = strchr(other + 1, '_')) != NULL) {
	*hit = '-';
    }
    if (strcmp(other, arg) == 0) {
	free(other);
	return 1;
    }
    free(other);
    return 0;
}


int
pan_narg_match(const char *option, const char *arg, int n)
{
    char *other;
    char *hit;

    if (strncmp(option, arg, n) == 0) {
	return 1;
    }
    other = strdup(option);
    while ((hit = strchr(other + 1, '-')) != NULL) {
	*hit = '_';
    }
    if (strncmp(other, arg, n) == 0) {
	free(other);
	return 1;
    }
    while ((hit = strchr(other + 1, '_')) != NULL) {
	*hit = '-';
    }
    if (strncmp(other, arg, n) == 0) {
	free(other);
	return 1;
    }
    free(other);

    return 0;
}


static int
pan_arg_occurs(int *argc, char *argv[], const char *option, int shift,
	       char **opt_value)
{
    int		i;
    int		j;
    char       *r;

    if (argc != NULL) {
	for (i = 1; i < *argc; i++) {
	    if (pan_arg_match(option, argv[i])) {
		if (i >= *argc - (shift - 1)) {
		    return -1;
		}
		if (opt_value != NULL) {
		    for (j = 0; j < shift - 1; j++) {
			opt_value[j] = argv[i + j + 1];
		    }
		}
		for (j = i; j < *argc - (shift - 1); j++) {
		    argv[j] = argv[j + shift];
		}
		(*argc) -= shift;
		return 1;
	    }
	}
    }

    r = pan_arg_env_match(option);
    if (r == NULL) {
	return 0;
    }

    if (opt_value != NULL) {
	if (shift > 2) {
	    fprintf(stderr,
		    "Warning: environment option %s do not support > 1 value\n",
		    option);
	}
	*opt_value = r;
    }

    return 1;
}


int
pan_arg_string(int *argc, char *argv[], const char *option,
	       char **opt_value)
{
    int		r;

    r = pan_arg_occurs(argc, argv, option, 2, opt_value);

    if (r == -1) {
	if (pan_opt_verbose()) {
	    fprintf(stderr, "option %s requires a string arg\n",
		    option);
	}
    }

    return r;
}


int
pan_arg_bool(int *argc, char *argv[], const char *option)
{
    return (pan_arg_occurs(argc, argv, option, 1, NULL));
}


int
pan_arg_int(int *argc, char *argv[], const char *option, int *val)
{
    char       *opt_value;
    int		r;

    r = pan_arg_occurs(argc, argv, option, 2, &opt_value);

    if (r == -1) {
	if (pan_opt_verbose()) {
	    fprintf(stderr, "option %s requires an int arg\n",
		    option);
	}
    } else if (r == 1 && sscanf(opt_value, "%d", val) != 1) {
	if (pan_opt_verbose()) {
	    fprintf(stderr, "option %s requires an int arg, not \"%s\"\n",
		    option, opt_value);
	}
	r = -1;
    }

    return r;
}


int
pan_arg_int_N(int *argc, char *argv[], const char *option, int n, int *val)
{
    char      **opt_value;
    int		r;
    int		i;

    opt_value = alloca(n * sizeof(char *));
    r = pan_arg_occurs(argc, argv, option, n + 1, opt_value);

    if (r == -1) {
	if (pan_opt_verbose()) {
	    fprintf(stderr, "option %s requires %d int args\n",
		    option, n);
	}
    } else if (r == 1) {
	for (i = 0; i < n; i++) {
	    if (sscanf(opt_value[i], "%d", &val[i]) != 1) {
		if (pan_opt_verbose()) {
		    fprintf(stderr,
			    "option %s[%d] requires an int arg, not \"%s\"\n",
			    option, i, opt_value[i]);
		}
		r = -1;
		break;
	    }
	}
    }

    return r;
}


int
pan_arg_double(int *argc, char *argv[], const char *option,
	       double *val)
{
    char       *opt_value;
    int		r;

    r = pan_arg_occurs(argc, argv, option, 2, &opt_value);

    if (r == -1) {
	if (pan_opt_verbose()) {
	    fprintf(stderr, "option %s requires a double arg\n",
		    option);
	}
    } else if (r == 1 && sscanf(opt_value, "%lf", val) != 1) {
	if (pan_opt_verbose()) {
	    fprintf(stderr, "option %s requires a double arg, not \"%s\"\n",
		    option, opt_value);
	}
	r = -1;
    }

    return r;
}
