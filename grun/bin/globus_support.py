#
# Copyright (C) 2004  Vrije Universiteit, Amsterdam, The Netherlands.
# For full copyright and restrictions on use, see the file COPYRIGHT
# in the Grun software distribution.
#
# Author: Kees Verstoep (versto@cs.vu.nl)
#

from pyGlobus.gramClient import *

# And many more missing currently..:
ERROR_DUCT_INIT_FAILED=33
ERROR_DUCT_LSP_FAILED=34
ERROR_INVALID_HOST_COUNT=35
ERROR_UNSUPPORTED_PARAMETER=36
ERROR_INVALID_QUEUE=37
ERROR_INVALID_PROJECT=38
ERROR_RSL_EVALUATION_FAILED=39
ERROR_BAD_RSL_ENVIRONMENT=40
ERROR_DRYRUN=41
ERROR_ZERO_LENGTH_RSL=42
ERROR_STAGING_EXECUTABLE=43
ERROR_STAGING_STDIN=44
ERROR_INVALID_JOB_MANAGER_TYPE=45
ERROR_BAD_ARGUMENTS=46
ERROR_GATEKEEPER_MISCONFIGURED=47
ERROR_BAD_RSL=48
ERROR_VERSION_MISMATCH=49
ERROR_RSL_ARGUMENTS=50
ERROR_RSL_COUNT=51
ERROR_RSL_DIRECTORY=52
ERROR_RSL_DRYRUN=53
ERROR_RSL_ENVIRONMENT=54
ERROR_RSL_EXECUTABLE=55
ERROR_RSL_HOST_COUNT=56
ERROR_RSL_JOBTYPE=57
ERROR_RSL_MAXTIME=58
ERROR_RSL_MYJOB=59
ERROR_RSL_PARADYN=60
ERROR_RSL_PROJECT=61
ERROR_RSL_QUEUE=62
ERROR_RSL_STDERR=63
ERROR_RSL_STDIN=64
ERROR_RSL_STDOUT=65
ERROR_OPENING_JOBMANAGER_SCRIPT=66
ERROR_CREATING_PIPE=67
ERROR_FCNTL_FAILED=68
ERROR_STDOUT_FILENAME_FAILED=69
ERROR_STDERR_FILENAME_FAILED=70
ERROR_FORKING_EXECUTABLE=71
ERROR_EXECUTABLE_PERMISSIONS=72
ERROR_OPENING_STDOUT=73
ERROR_OPENING_STDERR=74
ERROR_OPENING_CACHE_USER_PROXY=75
ERROR_OPENING_CACHE=76
ERROR_INSERTING_CLIENT_CONTACT=77
ERROR_CLIENT_CONTACT_NOT_FOUND=78
ERROR_CONTACTING_JOB_MANAGER=79
ERROR_INVALID_JOB_CONTACT=80
ERROR_UNDEFINED_EXE=81
ERROR_CONDOR_ARCH=82
ERROR_CONDOR_OS=83
ERROR_RSL_MIN_MEMORY=84
ERROR_RSL_MAX_MEMORY=85
ERROR_INVALID_MIN_MEMORY=86
ERROR_INVALID_MAX_MEMORY=87
ERROR_HTTP_FRAME_FAILED=88
ERROR_HTTP_UNFRAME_FAILED=89
ERROR_HTTP_PACK_FAILED=90
ERROR_HTTP_UNPACK_FAILED=91
ERROR_INVALID_JOB_QUERY=92
ERROR_SERVICE_NOT_FOUND=93
ERROR_JOB_QUERY_DENIAL=94
ERROR_CALLBACK_NOT_FOUND=95
ERROR_BAD_GATEKEEPER_CONTACT=96
ERROR_POE_NOT_FOUND=97
ERROR_MPIRUN_NOT_FOUND=98
ERROR_RSL_START_TIME=99
ERROR_RSL_RESERVATION_HANDLE=100
ERROR_RSL_MAX_WALL_TIME=101
ERROR_INVALID_MAX_WALL_TIME=102
ERROR_RSL_MAX_CPU_TIME=103
ERROR_INVALID_MAX_CPU_TIME=104
ERROR_JM_SCRIPT_NOT_FOUND=105
ERROR_JM_SCRIPT_PERMISSIONS=106
ERROR_SIGNALING_JOB=107
ERROR_UNKNOWN_SIGNAL_TYPE=108
ERROR_GETTING_JOBID=109
ERROR_WAITING_FOR_COMMIT=110
ERROR_COMMIT_TIMED_OUT=111
ERROR_RSL_SAVE_STATE=112
ERROR_RSL_RESTART=113
ERROR_RSL_TWO_PHASE_COMMIT=114
ERROR_INVALID_TWO_PHASE_COMMIT=115
ERROR_RSL_STDOUT_POSITION=116
ERROR_INVALID_STDOUT_POSITION=117
ERROR_RSL_STDERR_POSITION=118
ERROR_INVALID_STDERR_POSITION=119
ERROR_RESTART_FAILED=120
ERROR_NO_STATE_FILE=121
ERROR_READING_STATE_FILE=122
ERROR_WRITING_STATE_FILE=123
ERROR_OLD_JM_ALIVE=124
ERROR_TTL_EXPIRED=125
ERROR_SUBMIT_UNKNOWN=126
ERROR_RSL_REMOTE_IO_URL=127
ERROR_WRITING_REMOTE_IO_URL=128
ERROR_STDIO_SIZE=129
ERROR_JM_STOPPED=130
ERROR_USER_PROXY_EXPIRED=131
ERROR_JOB_UNSUBMITTED=132
ERROR_INVALID_COMMIT=133
ERROR_RSL_SCHEDULER_SPECIFIC=134
ERROR_STAGE_IN_FAILED=135
ERROR_INVALID_SCRATCH=136
ERROR_RSL_CACHE=137
ERROR_INVALID_SUBMIT_ATTRIBUTE=138
ERROR_INVALID_STDIO_UPDATE_ATTRIBUTE=139
ERROR_INVALID_RESTART_ATTRIBUTE=140
ERROR_RSL_FILE_STAGE_IN=141
ERROR_RSL_FILE_STAGE_IN_SHARED=142
ERROR_RSL_FILE_STAGE_OUT=143
ERROR_RSL_GASS_CACHE=144
ERROR_RSL_FILE_CLEANUP=145
ERROR_RSL_SCRATCH=146
ERROR_INVALID_SCHEDULER_SPECIFIC=147
ERROR_UNDEFINED_ATTRIBUTE=148
ERROR_INVALID_CACHE=149
ERROR_INVALID_SAVE_STATE=150
ERROR_OPENING_VALIDATION_FILE=151
ERROR_READING_VALIDATION_FILE=152
ERROR_RSL_PROXY_TIMEOUT=153
ERROR_INVALID_PROXY_TIMEOUT=154
ERROR_STAGE_OUT_FAILED=155
ERROR_JOB_CONTACT_NOT_FOUND=156
ERROR_DELEGATION_FAILED=157
ERROR_LOCKING_STATE_LOCK_FILE=158
ERROR_INVALID_ATTR=159
ERROR_NULL_PARAMETER=160
ERROR_STILL_STREAMING=161

def gram_error(e):
    if   e == ERROR_PARAMETER_NOT_SUPPORTED:
            return "parameter not supported"
    elif e == ERROR_INVALID_REQUEST:
            return "invalid request"
    elif e == ERROR_NO_RESOURCES:
            return "no resources"
    elif e == ERROR_BAD_DIRECTORY:
            return "bad directory"
    elif e == ERROR_EXECUTABLE_NOT_FOUND:
            return "executable not found"
    elif e == ERROR_INSUFFICIENT_FUNDS:
            return "insufficient funds"
    elif e == ERROR_AUTHORIZATION:
            return "authorization"
    elif e == ERROR_USER_CANCELLED:
            return "user cancelled"
    elif e == ERROR_SYSTEM_CANCELLED:
            return "system cancelled"
    elif e == ERROR_PROTOCOL_FAILED:
            return "protocol failed"
    elif e == ERROR_STDIN_NOT_FOUND:
            return "stdin not found"
    elif e == ERROR_CONNECTION_FAILED:
            return "connection failed"
    elif e == ERROR_INVALID_MAXTIME:
            return "invalid maxtime"
    elif e == ERROR_INVALID_COUNT:
            return "invalid count"
    elif e == ERROR_NULL_SPECIFICATION_TREE:
            return "null specification tree"
    elif e == ERROR_JM_FAILED_ALLOW_ATTACH:
            return "jobmanager failed allow attach"
    elif e == ERROR_JOB_EXECUTION_FAILED:
            return "job execution failed"
    elif e == ERROR_INVALID_PARADYN:
            return "invalid paradyn"
    elif e == ERROR_INVALID_JOBTYPE:
            return "invalid jobtype"
    elif e == ERROR_INVALID_GRAM_MYJOB:
            return "invalid GRAM myjob"
    elif e == ERROR_BAD_SCRIPT_ARG_FILE:
            return "bad script argument file"
    elif e == ERROR_ARG_FILE_CREATION_FAILED:
            return "argument file creation failed"
    elif e == ERROR_INVALID_JOBSTATE:
            return "invalid jobstate"
    elif e == ERROR_INVALID_SCRIPT_REPLY:
            return "invalid script reply"
    elif e == ERROR_INVALID_SCRIPT_STATUS:
            return "invalid script status"
    elif e == ERROR_JOBTYPE_NOT_SUPPORTED:
            return "jobtype not supported"
    elif e == ERROR_UNIMPLEMENTED:
            return "unimplemented"
    elif e == ERROR_TEMP_SCRIPT_FILE_FAILED:
            return "temporary script file failed"
    elif e == ERROR_USER_PROXY_NOT_FOUND:
            return "user proxy not found"
    elif e == ERROR_OPENING_USER_PROXY:
            return "opening user proxy"
    elif e == ERROR_JOB_CANCEL_FAILED:
            return "job cancel failed"
    elif e == ERROR_MALLOC_FAILED:
            return "malloc failed"
    elif e == ERROR_DUCT_INIT_FAILED:
            return "duct init failed"
    elif e == ERROR_DUCT_LSP_FAILED:
            return "duct lsp failed"
    elif e == ERROR_INVALID_HOST_COUNT:
            return "invalid host count"
    elif e == ERROR_UNSUPPORTED_PARAMETER:
            return "unsupported parameter"
    elif e == ERROR_INVALID_QUEUE:
            return "invalid queue"
    elif e == ERROR_INVALID_PROJECT:
            return "invalid project"
    elif e == ERROR_RSL_EVALUATION_FAILED:
            return "rsl evaluation failed"
    elif e == ERROR_BAD_RSL_ENVIRONMENT:
            return "bad rsl environment"
    elif e == ERROR_DRYRUN:
            return "dryrun"
    elif e == ERROR_ZERO_LENGTH_RSL:
            return "zero length rsl"
    elif e == ERROR_STAGING_EXECUTABLE:
            return "staging executable"
    elif e == ERROR_STAGING_STDIN:
            return "staging stdin"
    elif e == ERROR_INVALID_JOB_MANAGER_TYPE:
            return "invalid job manager type"
    elif e == ERROR_BAD_ARGUMENTS:
            return "bad arguments"
    elif e == ERROR_GATEKEEPER_MISCONFIGURED:
            return "gatekeeper misconfigured"
    elif e == ERROR_BAD_RSL:
            return "bad rsl"
    elif e == ERROR_VERSION_MISMATCH:
            return "version mismatch"
    elif e == ERROR_RSL_ARGUMENTS:
            return "rsl arguments"
    elif e == ERROR_RSL_COUNT:
            return "rsl count"
    elif e == ERROR_RSL_DIRECTORY:
            return "rsl directory"
    elif e == ERROR_RSL_DRYRUN:
            return "rsl dryrun"
    elif e == ERROR_RSL_ENVIRONMENT:
            return "rsl environment"
    elif e == ERROR_RSL_EXECUTABLE:
            return "rsl executable"
    elif e == ERROR_RSL_HOST_COUNT:
            return "rsl host_count"
    elif e == ERROR_RSL_JOBTYPE:
            return "rsl jobtype"
    elif e == ERROR_RSL_MAXTIME:
            return "rsl maxtime"
    elif e == ERROR_RSL_MYJOB:
            return "rsl myjob"
    elif e == ERROR_RSL_PARADYN:
            return "rsl paradyn"
    elif e == ERROR_RSL_PROJECT:
            return "rsl project"
    elif e == ERROR_RSL_QUEUE:
            return "rsl queue"
    elif e == ERROR_RSL_STDERR:
            return "rsl stderr"
    elif e == ERROR_RSL_STDIN:
            return "rsl stdin"
    elif e == ERROR_RSL_STDOUT:
            return "rsl stdout"
    elif e == ERROR_OPENING_JOBMANAGER_SCRIPT:
            return "opening jobmanager script"
    elif e == ERROR_CREATING_PIPE:
            return "creating pipe"
    elif e == ERROR_FCNTL_FAILED:
            return "fcntl failed"
    elif e == ERROR_STDOUT_FILENAME_FAILED:
            return "stdout filename failed"
    elif e == ERROR_STDERR_FILENAME_FAILED:
            return "stderr filename failed"
    elif e == ERROR_FORKING_EXECUTABLE:
            return "forking executable"
    elif e == ERROR_EXECUTABLE_PERMISSIONS:
            return "executable permissions"
    elif e == ERROR_OPENING_STDOUT:
            return "opening stdout"
    elif e == ERROR_OPENING_STDERR:
            return "opening stderr"
    elif e == ERROR_OPENING_CACHE_USER_PROXY:
            return "opening cache user proxy"
    elif e == ERROR_OPENING_CACHE:
            return "opening cache"
    elif e == ERROR_INSERTING_CLIENT_CONTACT:
            return "inserting client contact"
    elif e == ERROR_CLIENT_CONTACT_NOT_FOUND:
            return "client contact_not_found"
    elif e == ERROR_CONTACTING_JOB_MANAGER:
            return "contacting job manager"
    elif e == ERROR_INVALID_JOB_CONTACT:
            return "invalid job contact"
    elif e == ERROR_UNDEFINED_EXE:
            return "undefined exe"
    elif e == ERROR_CONDOR_ARCH:
            return "condor arch"
    elif e == ERROR_CONDOR_OS:
            return "condor os"
    elif e == ERROR_RSL_MIN_MEMORY:
            return "rsl min memory"
    elif e == ERROR_RSL_MAX_MEMORY:
            return "rsl max memory"
    elif e == ERROR_INVALID_MIN_MEMORY:
            return "invalid min memory"
    elif e == ERROR_INVALID_MAX_MEMORY:
            return "invalid max memory"
    elif e == ERROR_HTTP_FRAME_FAILED:
            return "http frame failed"
    elif e == ERROR_HTTP_UNFRAME_FAILED:
            return "http unframe failed"
    elif e == ERROR_HTTP_PACK_FAILED:
            return "http pack failed"
    elif e == ERROR_HTTP_UNPACK_FAILED:
            return "http unpack failed"
    elif e == ERROR_INVALID_JOB_QUERY:
            return "invalid job query"
    elif e == ERROR_SERVICE_NOT_FOUND:
            return "service not found"
    elif e == ERROR_JOB_QUERY_DENIAL:
            return "job query denial"
    elif e == ERROR_CALLBACK_NOT_FOUND:
            return "callback not found"
    elif e == ERROR_BAD_GATEKEEPER_CONTACT:
            return "bad gatekeeper contact"
    elif e == ERROR_POE_NOT_FOUND:
            return "poe not found"
    elif e == ERROR_MPIRUN_NOT_FOUND:
            return "mpirun not found"
    elif e == ERROR_RSL_START_TIME:
            return "rsl start time"
    elif e == ERROR_RSL_RESERVATION_HANDLE:
            return "rsl reservation handle"
    elif e == ERROR_RSL_MAX_WALL_TIME:
            return "rsl max wall time"
    elif e == ERROR_INVALID_MAX_WALL_TIME:
            return "invalid max wall time"
    elif e == ERROR_RSL_MAX_CPU_TIME:
            return "rsl max cpu time"
    elif e == ERROR_INVALID_MAX_CPU_TIME:
            return "invalid max cpu time"
    elif e == ERROR_JM_SCRIPT_NOT_FOUND:
            return "jm script not found"
    elif e == ERROR_JM_SCRIPT_PERMISSIONS:
            return "jm script permissions"
    elif e == ERROR_SIGNALING_JOB:
            return "signaling job"
    elif e == ERROR_UNKNOWN_SIGNAL_TYPE:
            return "unknown signal type"
    elif e == ERROR_GETTING_JOBID:
            return "getting jobid"
    elif e == ERROR_WAITING_FOR_COMMIT:
            return "waiting for commit"
    elif e == ERROR_COMMIT_TIMED_OUT:
            return "commit timed out"
    elif e == ERROR_RSL_SAVE_STATE:
            return "rsl save state"
    elif e == ERROR_RSL_RESTART:
            return "rsl restart"
    elif e == ERROR_RSL_TWO_PHASE_COMMIT:
            return "rsl two phase commit"
    elif e == ERROR_INVALID_TWO_PHASE_COMMIT:
            return "invalid two phase commit"
    elif e == ERROR_RSL_STDOUT_POSITION:
            return "rsl stdout position"
    elif e == ERROR_INVALID_STDOUT_POSITION:
            return "invalid stdout position"
    elif e == ERROR_RSL_STDERR_POSITION:
            return "rsl stderr position"
    elif e == ERROR_INVALID_STDERR_POSITION:
            return "invalid stderr position"
    elif e == ERROR_RESTART_FAILED:
            return "restart failed"
    elif e == ERROR_NO_STATE_FILE:
            return "no state file"
    elif e == ERROR_READING_STATE_FILE:
            return "reading state file"
    elif e == ERROR_WRITING_STATE_FILE:
            return "writing state file"
    elif e == ERROR_OLD_JM_ALIVE:
            return "old jm alive"
    elif e == ERROR_TTL_EXPIRED:
            return "ttl expired"
    elif e == ERROR_SUBMIT_UNKNOWN:
            return "submit unknown"
    elif e == ERROR_RSL_REMOTE_IO_URL:
            return "rsl remote io url"
    elif e == ERROR_WRITING_REMOTE_IO_URL:
            return "writing remote io url"
    elif e == ERROR_STDIO_SIZE:
            return "stdio size"
    elif e == ERROR_JM_STOPPED:
            return "jm stopped"
    elif e == ERROR_USER_PROXY_EXPIRED:
            return "user proxy expired"
    elif e == ERROR_JOB_UNSUBMITTED:
            return "job unsubmitted"
    elif e == ERROR_INVALID_COMMIT:
            return "invalid commit"
    elif e == ERROR_RSL_SCHEDULER_SPECIFIC:
            return "rsl scheduler specific"
    elif e == ERROR_STAGE_IN_FAILED:
            return "stage in failed"
    elif e == ERROR_INVALID_SCRATCH:
            return "invalid scratch"
    elif e == ERROR_RSL_CACHE:
            return "rsl cache"
    elif e == ERROR_INVALID_SUBMIT_ATTRIBUTE:
            return "invalid submit attribute"
    elif e == ERROR_INVALID_STDIO_UPDATE_ATTRIBUTE:
            return "invalid stdio update attribute"
    elif e == ERROR_INVALID_RESTART_ATTRIBUTE:
            return "invalid restart attribute"
    elif e == ERROR_RSL_FILE_STAGE_IN:
            return "rsl file stage in"
    elif e == ERROR_RSL_FILE_STAGE_IN_SHARED:
            return "rsl file stage in shared"
    elif e == ERROR_RSL_FILE_STAGE_OUT:
            return "rsl file stage out"
    elif e == ERROR_RSL_GASS_CACHE:
            return "rsl gass cache"
    elif e == ERROR_RSL_FILE_CLEANUP:
            return "rsl file cleanup"
    elif e == ERROR_RSL_SCRATCH:
            return "rsl scratch"
    elif e == ERROR_INVALID_SCHEDULER_SPECIFIC:
            return "invalid scheduler specific"
    elif e == ERROR_UNDEFINED_ATTRIBUTE:
            return "undefined attribute"
    elif e == ERROR_INVALID_CACHE:
            return "invalid cache"
    elif e == ERROR_INVALID_SAVE_STATE:
            return "invalid save state"
    elif e == ERROR_OPENING_VALIDATION_FILE:
            return "opening validation file"
    elif e == ERROR_READING_VALIDATION_FILE:
            return "reading validation file"
    elif e == ERROR_RSL_PROXY_TIMEOUT:
            return "rsl proxy timeout"
    elif e == ERROR_INVALID_PROXY_TIMEOUT:
            return "invalid proxy timeout"
    elif e == ERROR_STAGE_OUT_FAILED:
            return "stage out failed"
    elif e == ERROR_JOB_CONTACT_NOT_FOUND:
            return "job contact not found"
    elif e == ERROR_DELEGATION_FAILED:
            return "delegation failed"
    elif e == ERROR_LOCKING_STATE_LOCK_FILE:
            return "locking state lock file"
    elif e == ERROR_INVALID_ATTR:
            return "invalid attr"
    elif e == ERROR_NULL_PARAMETER:
            return "null parameter"
    elif e == ERROR_STILL_STREAMING:
            return "still streaming"
    else:
            return "unknown error %d" % e
