#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <libgen.h>
#include <signal.h>
#ifdef __linux
#include <sched.h>
#include <linux/unistd.h>
#endif 

extern char **environ;

int main(int argc, char *argv[])
{
    char *prun_env = getenv("PRUN_ENV");
    FILE *env_file;
    int		app_argc;
    char       *wrapper;
#ifdef __linux
    /*
     * provide the proper syscall information if our libc
     * is not yet updated.
     */
#ifndef __NR_sched_setaffinity
#define __NR_sched_setaffinity  241
#define __NR_sched_getaffinity  242
    _syscall3 (int, sched_setaffinity, pid_t, pid, unsigned int, len, unsigned long *, user_mask_ptr)
    _syscall3 (int, sched_getaffinity, pid_t, pid, unsigned int, len, unsigned long *, user_mask_ptr)
#endif
    unsigned long cpu_affinity = 1;         /* Use one CPU */
    int len = sizeof(cpu_affinity);
#endif

    wrapper = basename(strdup(argv[0]));

    for (app_argc = 1; app_argc < argc; app_argc++) {
	if (0) {
	} else if (argv[app_argc][0] == '-') {
#ifdef __linux
	    if (strcmp(argv[app_argc], "-cpu") == 0) {
		app_argc++;
		if (app_argc == argc) {
		    fprintf(stderr, "%s: -cpu requires an integer argument\n",
			    wrapper);
		    exit(31);
		}
		if (sscanf(argv[app_argc], "%lu", &cpu_affinity) != 1) {
		    fprintf(stderr, "%s: -cpu requires an integer argument\n",
			    wrapper);
		    exit(33);
		}
	    }
#endif
	} else {
	    break;
	}
    }

#ifdef __linux
    if (cpu_affinity != 3) {
	fprintf(stderr, "%s: use CPU %lu only\n",
		wrapper, cpu_affinity);
    }
    if (sched_setaffinity(0, len, &cpu_affinity) < 0) {
	fprintf(stderr, "%s: sched_setaffinity fails",
		wrapper);
//	exit(0);
    }
#endif

    if (prun_env != NULL) {

	env_file = fopen(prun_env, "r");
	if (env_file == NULL) {
	    fprintf(stderr, "%s: want to open PRUN env file %s, but it does not exist.\n", wrapper, prun_env);
//	    exit(0);
	}
	while (1) {
#define ENV_LEN		4096
	    char *envdef = malloc(ENV_LEN);
	    char *nl;

	    if (envdef == NULL) {
		fprintf(stderr, "%s: error: malloc fails\n", wrapper);
		return 9;
	    }
	    fgets(envdef, ENV_LEN - 1, env_file);
	    if (feof(env_file)) {
		break;
	    }
	    nl = strchr(envdef, '\n');
	    if (nl != NULL) {
		*nl = '\0';
	    }
	    putenv(envdef);
	}

	(void)fclose(env_file);

    } else {
	fprintf(stderr, "%s: warning: env var PRUN_ENV not set\n", wrapper);
    }

    if (0) {
	sigset_t	mask;

	if (sigemptyset(&mask) != 0) {
	    fprintf(stderr, "%s.%d: sigemptyset fails\n", wrapper, __LINE__);
//	    exit(0);
	}
	if (sigaddset(&mask, SIGIO) != 0) {
	    fprintf(stderr, "%s.%d: sigaddset fails\n", wrapper, __LINE__);
//	    exit(0);
	}
	if (sigprocmask(SIG_BLOCK, &mask, NULL) != 0) {
	    fprintf(stderr, "%s.%d: sigprocmask fails\n", wrapper, __LINE__);
//	    exit(0);
	}
	fprintf(stderr, "%s: Blocked SIGIO\n", wrapper);
    }

    if (execve(argv[app_argc], &argv[app_argc], environ) == -1) {
	fprintf(stderr, "%s: execve fails for: ", wrapper);
	for (; app_argc < argc; app_argc++) {
	    fprintf(stderr, "%s ", argv[app_argc]);
	}
	fprintf(stderr, "\n");
	perror("execve failed\n");
	return 11;
    }

    return 0;
}
