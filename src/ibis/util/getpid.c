#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>

int
main(int argc, char *argv[])
{
    printf("%d\n", getpid());

    return 0;
}
