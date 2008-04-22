This directory contains some Ibis example programs. They can be compiled by 
using "ant". Some of these examples are also used in the user's guide and 
programmer's manual (located in the docs/ directory)

Hello:

A simple application that sends a single message from one Ibis to another

HelloUpcall:

Same as above, but uses the ibis upcall mechanism to receive the message

RegistryUpcalls:

A simple demo of the capabilities of the Ibis registry. Prints out any events
it received from the registry (uses upcalls)

RegistryDowncalls:

Same as above, but with downcalls.

OneToMany:

Shows how to multicast data to multiple receivers

ManyToOne:

How to connect multiple senders to a single receiver

ClientServer:

Implementation of a small client-server application.
