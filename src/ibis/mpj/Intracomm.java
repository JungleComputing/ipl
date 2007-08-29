/* $Id$ */

/*
 * Created on 21.01.2005
 */
package ibis.mpj;

import ibis.ipl.IbisIdentifier;

import java.util.Vector;

/**
 * Communicator for the collective operations.
 */
public class Intracomm extends Comm {
    private static final int TAG_BARRIER = -1;
    private static final int TAG_BCAST   = -2;
    private static final int TAG_REDUCE  = -3;
    private static final int TAG_ALLREDUCE_1 = -4;
    private static final int TAG_GATHER = -5;
    private static final int TAG_GATHERV = -6;
    private static final int TAG_ALLGATHER = -7;
    //	private static final int TAG_ALLGATHERV = -8;
    private static final int TAG_SCATTER = -9;
    private static final int TAG_SCATTERV = -10;
    private static final int TAG_ALLTOALL = -11;
    private static final int TAG_ALLTOALLV = -12;
    private static final int TAG_REDUCESCATTER = -13;
    private static final int TAG_SCAN = -14;
    private static final int TAG_SPLIT = -15;
    //	private static final int TAG_MINVALUE = -15;


    public Intracomm() throws MPJException {
        super();
    }

    public Object clone() {

        try {
            if (!testInter()) {
                Intracomm cpComm = new Intracomm();

                cpComm.group = new Group();

                cpComm.group.table = new Vector<IbisIdentifier>(this.group.table);
                return(cpComm);

            }
            return(null);
        }
        catch (MPJException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return(null);
        }

    }

    /**
     * Create a new intra communicator
     * @param group group to be associated with the new communicator
     * @return new intra communicator
     * @throws MPJException
     */
    public Intracomm create(Group group) throws MPJException {

        Intracomm intracomm = new Intracomm();
        intracomm.group = group;

        return(intracomm);
    }



    /**
     * Partition the group associated with this communicator and create a new communicator
     * within each subgroup.
     * @param colour control of subset assignment
     * @param key control of rank assignment
     * @return new intra communicator
     * @throws MPJException
     */
    public Intracomm split(int colour, int key) throws MPJException {
        ColSplit split = new ColSplit(colour, key, this, this.TAG_SPLIT);

        return(split.call());
    }


    /**
     * A call to barrier blocks the caller until all processes in the group have called it.
     * 
     * @throws MPJException
     */
    public void barrier() throws MPJException {
        ColBarrier barrier = new ColBarrier(this, this.TAG_BARRIER);

        barrier.call();

    }


    /**
     * Broadcast a message from the process with rank root to all processes of the group.
     *  
     * @param sendbuf buffer array
     * @param offset initial offset in buffer
     * @param count number of items in buffer
     * @param datatype datatype of each item in buffer
     * @param root rank of broadcast root
     * @throws MPJException
     */
    public void bcast(Object sendbuf, int offset, int count, Datatype datatype, int root) throws MPJException {

        ColBcast bcast = new ColBcast(sendbuf, offset, count, datatype, root, this, this.TAG_BCAST);

        bcast.call();

    }


    /**
     * Each process sends the contents of its send buffer to the root process.
     * 
     * @param sendbuf send buffer array
     * @param sendoffset initial offset in send buffer
     * @param sendcount number of items to send
     * @param sendtype datatype of each item in send buffer
     * @param recvbuf receive buffer array 
     * @param recvoffset initial offset in receive buffer
     * @param recvcount number of items in receive buffer
     * @param recvtype datatype of each item in receive buffer
     * @param root rank of receiving process
     * @throws MPJException
     */
    public void gather(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
            Object recvbuf, int recvoffset, int recvcount, Datatype recvtype,
            int root) throws MPJException {


        ColGather gather = new ColGather(sendbuf, sendoffset, sendcount, sendtype,
                recvbuf, recvoffset, recvcount,
                recvtype, root, this, this.TAG_GATHER);

        gather.call();

    }


    /**
     * Extends funcionality of gather by allowing varying counts of data from each process.
     * The sizes of arrays recvcount and displs should be the size of the group. Entry i of displs
     * specifies the displacement relative to element recvoffset of recvbuf at which to place incoming
     * data. Note that if recvtype is a derived datatype, elements of displs are in units of the
     * derived type extent, (unlike recvoffset, which is a direct index into the buffer array).
     * 
     * @param sendbuf send buffer array
     * @param sendoffset initial offset in send buffer
     * @param sendcount number of items in send buffer
     * @param sendtype datatype of each item in send buffer 
     * @param recvbuf receive buffer array
     * @param recvoffset initial offset in receive buffer
     * @param recvcount number of items in receive buffer
     * @param displs displacements at which to place incoming data
     * @param recvtype datatype of each item in receive buffer
     * @param root rank of receiving process
     * @throws MPJException
     */
    public void gatherv(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
            Object recvbuf, int recvoffset, int[] recvcount, int[] displs,
            Datatype recvtype, int root) throws MPJException {

        ColGatherV gatherv = new ColGatherV(sendbuf, sendoffset, sendcount, sendtype,
                recvbuf, recvoffset, recvcount, displs,
                recvtype, root, this, this.TAG_GATHERV);

        gatherv.call();

    }



    /**
     * Inverse of the operation gather.
     * 
     * @param sendbuf send buffer array
     * @param sendoffset initial offset in send buffer
     * @param sendcount number of items sent to each process
     * @param sendtype datatype of send buffer items
     * @param recvbuf receive buffer array
     * @param recvoffset initial offset in receive buffer
     * @param recvcount number of items in receive buffer
     * @param recvtype datatype of receive buffer items
     * @param root rank of sending process
     * @throws MPJException
     */
    public void scatter(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
            Object recvbuf, int recvoffset, int recvcount, Datatype recvtype,
            int root) throws MPJException {

        ColScatter scatter = new ColScatter(sendbuf, sendoffset, sendcount, sendtype,
                recvbuf, recvoffset, recvcount, recvtype, root, 
                this, this.TAG_SCATTER);

        scatter.call();


    }


    /** 
     * Inverse of the operation getherv.
     * 
     * @param sendbuf send buffer array
     * @param sendoffset initial offset in send buffer
     * @param sendcount number of items sent to each process
     * @param displs displacements from which to take outgoing data
     * @param sendtype datatype of each item in send buffer
     * @param recvbuf receive buffer array
     * @param recvoffset initial offset in receive buffer
     * @param recvcount number of elements in receive buffer
     * @param recvtype datatype of receive buffer items
     * @param root rank of sending process
     * @throws MPJException
     */
    public void scatterv(Object sendbuf, int sendoffset, int[] sendcount, int[] displs,
            Datatype sendtype,	
            Object recvbuf, int recvoffset, int recvcount, Datatype recvtype,
            int root) throws MPJException {

        ColScatterV scatterv = new ColScatterV(sendbuf, sendoffset, sendcount, displs, sendtype, 
                recvbuf, recvoffset, recvcount, recvtype, root, 
                this, this.TAG_SCATTERV);
        scatterv.call();

    }



    /**
     * Similar to gather, but all processes receive the result.
     * 
     * @param sendbuf send buffer array
     * @param sendoffset initial offset in send buffer
     * @param sendcount number of items sent to each process
     * @param sendtype datatype of send buffer items
     * @param recvbuf receive buffer array
     * @param recvoffset initial offset in receive buffer
     * @param recvcount number of items in receive buffer
     * @param recvtype datatype of receive buffer items
     * @throws MPJException
     */
    public void allgather(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
            Object recvbuf, int recvoffset, int recvcount, Datatype recvtype) throws MPJException {




        ColAllGather allGather = new ColAllGather(sendbuf, sendoffset, sendcount, sendtype,
                recvbuf, recvoffset, recvcount, recvtype, this, this.TAG_ALLGATHER);


        allGather.call();

    }


    /**
     * Similar to gatherv, but all processes receive the result.
     * 
     * @param sendbuf send buffer array
     * @param sendoffset initial offset in send buffer
     * @param sendcount number of items to send
     * @param sendtype datatype of each item in send buffer
     * @param recvbuf receive buffer array
     * @param recvoffset initial offset in receive buffer
     * @param recvcount numver of elements received from each process
     * @param displs displacements at which to place incoming data
     * @param recvtype datatype of each item in receive buffer
     * @throws MPJException
     */
    public void allgatherv(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
            Object recvbuf, int recvoffset, int[] recvcount, int[] displs, 
            Datatype recvtype) throws MPJException {

        ColAllGatherV allGatherV = new ColAllGatherV(sendbuf, sendoffset, sendcount, sendtype,
                recvbuf, recvoffset, recvcount, displs, recvtype, this, this.TAG_GATHERV);
        allGatherV.call();
    }



    /**
     * Extension to allgather to the case where each process sends distinct data to each of the receivers.
     * 
     * @param sendbuf send buffer array
     * @param sendoffset initial offset in send buffer
     * @param sendcount number of items sent to each process
     * @param sendtype datatype of send buffer items
     * @param recvbuf receive buffer array
     * @param recvoffset initial offset in receive buffer
     * @param recvcount number of items received fram any process
     * @param recvtype datatype of receive buffer items
     * @throws MPJException
     */
    public void alltoall(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
            Object recvbuf, int recvoffset, int recvcount, Datatype recvtype) throws MPJException {



        ColAllToAll allToAll = new ColAllToAll(sendbuf, sendoffset, sendcount, sendtype,
                recvbuf, recvoffset, recvcount, recvtype, 
                this, this.TAG_ALLTOALL);

        allToAll.call();

    }



    /**
     * Adds felxibility to alltoall: location of data for send is specified by sdispls and location to place the
     * data on receive side is specified by rdispls.
     * 
     * @param sendbuf send buffer array
     * @param sendoffset initial offset in send buffer
     * @param sendcount number of items sent to each process
     * @param sdispls displacements from which to take outgoing data
     * @param sendtype datatype of each item in send buffer
     * @param recvbuf receive buffer array
     * @param recvoffset initial offset in receive buffer
     * @param recvcount number of elements received from each process
     * @param rdispls displacements at which to place incoming data
     * @param recvtype datatype of each item in receive buffer
     * @throws MPJException
     */
    public void alltoallv(Object sendbuf, int sendoffset, int[] sendcount, int[] sdispls,
            Datatype sendtype,
            Object recvbuf, int recvoffset, int[] recvcount, int[] rdispls,
            Datatype recvtype) throws MPJException {
        ColAllToAllV allToAllV = new ColAllToAllV(sendbuf, sendoffset, sendcount, sdispls, sendtype,
                recvbuf, recvoffset, recvcount, rdispls, recvtype, 
                this, this.TAG_ALLTOALLV);

        allToAllV.call();
    }



    /**
     * Combine elements in input buffer of each process using the reduce operation, and return the combined 
     * value in the output buffer of the root process.
     * 
     * @param sendBuf send buffer array
     * @param sendOffset initial offset in send buffer
     * @param recvBuf receive buffer array
     * @param recvOffset initial offset in receive buffer
     * @param count number of items in send buffer
     * @param datatype datatype of each item in send buffer
     * @param op reduce operation
     * @param root rank of root process
     * @throws MPJException
     */
    public void reduce(Object sendBuf, int sendOffset, Object recvBuf, int recvOffset,  int count, Datatype datatype, Op op, int root) throws MPJException {

        ColReduce reduce = new ColReduce(sendBuf, sendOffset, recvBuf, recvOffset, count, datatype, op, root, this, this.TAG_REDUCE);
        reduce.call();


    }





    /**
     * Same as reduce except that the result appears in receive buffer of all processes in the group.
     * 
     * @param sendbuf send buffer array
     * @param sendoffset initial offset in send buffer
     * @param recvbuf receive buffer array
     * @param recvoffset initial offset in receive buffer
     * @param count number of items in send buffer
     * @param datatype data type of each item in send buffer
     * @param op reduce operation
     * @throws MPJException
     */
    public void allreduce(Object sendbuf, int sendoffset, Object recvbuf, int recvoffset, int count, Datatype datatype, Op op) throws MPJException {

        ColAllreduce allreduce = new ColAllreduce(sendbuf, sendoffset, recvbuf, recvoffset, count, datatype, op, this, this.TAG_ALLREDUCE_1);
        allreduce.call();

        /*		ColReduce reduce = new ColReduce(sendbuf, sendoffset, recvbuf, recvoffset, count, datatype, op, 0, this, this.TAG_ALLREDUCE_1);
                        reduce.execute();


                        ColBcast bcast = new ColBcast(recvbuf, recvoffset, count, datatype, 0, this, this.TAG_ALLREDUCE_2);
                        bcast.execute();*/


    }



    /**
     * Combine elements in input buffer of each process using the reduce operation, and scatter the combined
     * values vover the output buffers of the processes.
     * 
     * @param sendbuf send buffer array
     * @param sendoffset initial offset in send buffer
     * @param recvbuf receive buffer array
     * @param recvoffset initial offset in receive buffer
     * @param recvcounts number of result elements distributed to each process
     * @param datatype datatype of each item in send buffer
     * @param op reduce operation
     * @throws MPJException
     */
    public void reduceScatter(Object sendbuf, int sendoffset,
            Object recvbuf, int recvoffset, 
            int[] recvcounts, Datatype datatype,
            Op op) throws MPJException {


        ColReduceScatter reduceScatter = new ColReduceScatter(sendbuf, sendoffset, 
                recvbuf, recvoffset, recvcounts, 
                datatype, op, this, this.TAG_REDUCESCATTER);

        reduceScatter.call();

    }



    /** 
     * Perform a prefix reduction on data distributed across the group.
     * 
     * @param sendbuf send buffer array
     * @param sendoffset initial offset in send buffer
     * @param recvbuf receive buffer array
     * @param recvoffset initial offset in receive buffer
     * @param count number of items in input buffer
     * @param datatype datatype of each item in input buffer
     * @param op reduce operation
     * @throws MPJException
     */
    public void scan(Object sendbuf, int sendoffset, Object recvbuf, int recvoffset,
            int count, Datatype datatype, Op op) throws MPJException { 

        ColScan scan = new ColScan(sendbuf, sendoffset, recvbuf, 
                recvoffset, count, datatype, op, this, this.TAG_SCAN);

        scan.call();

    }


    /**
     * 	
     * <strong> NOT IMPLEMENTED YET. </strong>
     * 
     * Create a graph topology communicator whose group is a subset of the group of this communicator.
     * The number of nodes in the graph, nnodes, is taken to be size of the index argument. The size
     * of array edges must be index[nnodes-1].
     * 
     * @param index node degrees
     * @param edges graph edges
     * @param reorder true if ranking my be reordered, false if not
     * @return new graph topology communicator
     * @throws MPJException
     */
    public Graphcomm createGraph(int[] index, int[] edges, boolean reorder) throws MPJException {
        return(null);
    }

    /**
     * <strong> NOT IMPLEMENTED YET. </strong>
     * 
     * Create a Cartesian topology communicator whose group is a subset of the group of this
     * communicator. The number of dimensions of the Cartesian grid is taken to be the size of the
     * dims argument. The array periods must be the same size.
     * 
     * @param dims the number of processes in each dimension
     * @param periods true if grid is periodic, false if not, in each dimension
     * @param reorder true if ranking may be reordered, false if not
     * @return new Cartesian topology communicator
     * @throws MPJException
     */
    public Cartcomm createCart(int[] dims, boolean[] periods, boolean reorder) throws MPJException {
        return(null);
    }




}
