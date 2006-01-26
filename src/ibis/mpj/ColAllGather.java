/* $Id$ */

/*
 * Created on 05.04.2005
 */
package ibis.mpj;

/**
 * Implementation of the collective operation: allgather.
 */
public class ColAllGather {
    private final boolean DEBUG = false;

    private Object sendbuf = null;
    private int sendoffset = 0;
    private int sendcount = 0;
    private Datatype sendtype = null;
    private Object recvbuf = null;
    private int recvoffset = 0;
    private int recvcount = 0;
    private Datatype recvtype = null;
    private Intracomm comm = null;
    private int tag = 0;


    public ColAllGather(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype,
            Object recvbuf, int recvoffset, int recvcount, Datatype recvtype, Intracomm comm, int tag) {
        this.sendbuf = sendbuf;
        this.sendoffset = sendoffset;
        this.sendcount = sendcount;
        this.sendtype = sendtype;
        this.recvbuf = recvbuf;
        this.recvoffset = recvoffset;
        this.recvcount = recvcount;
        this.recvtype = recvtype;
        this.comm = comm;
        this.tag = tag;

    }

    //double ring algorithm (taken from CCJ)
    protected void call() throws MPJException {
        int rank = this.comm.rank();
        int size = this.comm.size();


        int leftRank = getPrevRank(1, rank, size);
        int rightRank = getNextRank(1, rank, size);
        int nextRank = 0;
        int prevRank = 0;
        //		int myPosition = 0;
        int nextPosition = 0;
        int prevPosition = 0;


        int rounds = (size - 1) / 2;
        boolean add_half_round = (((size - 1) % 2) != 0);

        this.comm.localcopy2types(sendbuf, sendoffset, sendcount, sendtype, 
                recvbuf, recvoffset + rank * recvcount * recvtype.extent(), recvcount, recvtype);





        try {
            for (int i = 0; i < rounds; i++) {
                prevRank = getPrevRank(i, rank, size);
                nextRank = getNextRank(i, rank, size);

                nextPosition = this.recvoffset + (nextRank * (recvcount * recvtype.extent()));// + displs[nextRank];
                prevPosition = this.recvoffset + (prevRank * (recvcount * recvtype.extent()));// + displs[prevRank];


                this.comm.send(recvbuf, nextPosition, this.recvcount, this.recvtype, leftRank, this.tag);
                this.comm.send(recvbuf, prevPosition, this.recvcount, this.recvtype, rightRank, this.tag);

                prevRank = getPrevRank(i+1, rank, size);
                nextRank = getNextRank(i+1, rank, size);

                nextPosition = this.recvoffset + (nextRank * (recvcount * recvtype.extent()));// + displs[nextRank];
                prevPosition = this.recvoffset + (prevRank * (recvcount * recvtype.extent()));// + displs[prevRank];

                if (DEBUG) {
                    System.out.println("prevRank: "+ prevRank + "; prevPos: " + prevPosition + "; rightRank: " + rightRank);
                    System.out.println("nextRank: "+ nextRank + "; nextPos: " + nextPosition + ";  leftRank: " + leftRank);
                }


                this.comm.recv(recvbuf, nextPosition, this.recvcount, this.recvtype, rightRank, this.tag);
                this.comm.recv(recvbuf, prevPosition, this.recvcount, this.recvtype, leftRank, this.tag);
            }
            if (add_half_round) {

                // just one send and one receive
                prevRank = getPrevRank(rounds+1, rank, size);
                nextRank = getNextRank(rounds, rank, size);

                nextPosition = this.recvoffset + (nextRank * (recvcount * recvtype.extent()));// + displs[nextRank];
                prevPosition = this.recvoffset + (prevRank * (recvcount * recvtype.extent()));// + displs[prevRank];

                if (DEBUG) {
                    System.out.println("prevRank: "+ prevRank + "; prevPos: " + prevPosition + "; rightRank: " + rightRank);
                    System.out.println("nextRank: "+ nextRank + "; nextPos: " + nextPosition + ";  leftRank: " + leftRank);
                }

                this.comm.send(recvbuf, nextPosition, this.recvcount, this.recvtype, leftRank, this.tag);
                this.comm.recv(recvbuf, prevPosition, this.recvcount, this.recvtype, rightRank, this.tag);

            }
        } 
        catch (Exception e) 
        {
            throw new MPJException(e.toString());
        }



    }

    private int getPrevRank(int round, int myRank, int size) {
        return((myRank + round) % size);


    }

    private int getNextRank(int round, int myRank, int size) {
        return((myRank + size - round) % size);
    }
}
