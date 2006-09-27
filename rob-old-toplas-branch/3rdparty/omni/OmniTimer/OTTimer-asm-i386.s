// Copyright 1997-2001 Omni Development, Inc.  All rights reserved.
//
// This software may only be used and reproduced according to the
// terms in the file OmniSourceLicense.html, which should be
// distributed with this project and can also be found at
// http://www.omnigroup.com/DeveloperResources/OmniSourceLicense.html.

#ifdef __i386__

.text
.align 4

.globl _OTReadCounter
_OTReadCounter:
	pushl %ebx
	movl 8(%esp), %ebx
        // We have to hand encode rdtsc because of a bug in the kernel.  If compiled directly to 
        // executable code, the cpuSubtype in the mach-o header will be set to i586.  This has the
        // undesired side-effect of making the code non-executable.  Sigh.
	// rdtsc
        .byte 0x0f
        .byte 0x31
        movl %eax, (%ebx)
        movl %edx, 4(%ebx)
        popl %ebx 
	ret

.globl _OTAddDeltaSinceStart
_OTAddDeltaSinceStart:
	pushl %ebp
	movl  %esp, %ebp

	pushl %ebx
	pushl %ecx
	pushl %edi

	// %ebx = startTimer
	movl  8(%ebp), %ebx

	// read the current time stamp into %edx:%eax
        // rdtsc
        .byte 0x0f
        .byte 0x31

	// compute the delta into %edx:%eax
	movl (%ebx),  %edi
	movl 4(%ebx), %ecx
	subl %edi,    %eax
	sbbl %ecx,    %edx

	// %ebx = sumTimer
	movl 12(%ebp), %ebx

	// add to the sum
	addl %eax,  (%ebx)
	adcl %edx, 4(%ebx)

	// restore the callee registers and return
	popl %edi
	popl %ecx
	popl %ebx

	movl %ebp, %esp
	popl %ebp
	ret



.align 4
.globl _OTDeltaTimers
_OTDeltaTimers:
	// Store off the callee save registers that we are going to use
	pushl %ebp
	movl %esp, %ebp

	pushl %ebx
        pushl %ecx
	pushl %esi

	// %eax = startTimer, %ebx = endTimer, %ecx = deltaTimer
	movl 8(%ebp), %eax
	movl 12(%ebp), %ebx
	movl 16(%ebp), %esi

	// load the start and end low values
	movl (%eax), %edx
	movl (%ebx), %ecx

	// subtract and store the result in the low of deltaTimer
	subl %edx, %ecx
	movl %ecx, (%esi)

	// load the start and end high values
	movl 4(%eax), %edx
	movl 4(%ebx), %ecx

	// subtract (with borrow) and store the in the high of deltaTimer
	sbbl %edx, %ecx
	movl %ecx, 4(%esi)

	// Restore stuff and return
	popl %esi
	popl %ecx
	popl %ebx
        movl %ebp, %esp
        popl %ebp
	ret

.align 4
.globl _OTSumTimers
_OTSumTimers:
	// Store off the callee save registers that we are going to use
	pushl %ebp
	movl %esp, %ebp

	pushl %ebx
        pushl %ecx
	pushl %esi

	// %eax = timer1, %ebx = timer2, %ecx = sumTimer
	movl 8(%ebp), %eax
	movl 12(%ebp), %ebx
	movl 16(%ebp), %esi

	// load the two low values
	movl (%eax), %edx
	movl (%ebx), %ecx

	// add and store the result in the low of sumTimer
	addl %edx, %ecx
	movl %ecx, (%esi)

	// load the two high values
	movl 4(%eax), %edx
	movl 4(%ebx), %ecx

	// add (with carry) and store the in the high of sumTimer
	adcl %edx, %ecx
	movl %ecx, 4(%esi)

	// Restore stuff and return
	popl %esi
	popl %ecx
	popl %ebx
        movl %ebp, %esp
        popl %ebp
	ret


#endif // i386
