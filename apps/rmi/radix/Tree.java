import java.io.*;
import ibis.rmi.*;
import ibis.rmi.server.UnicastRemoteObject;

public class Tree extends UnicastRemoteObject implements TreeInterface {

    
    Prefix_Node[] tree;
    
    Tree(int length, int max_Radix)throws Exception{
	super();
	tree = new Prefix_Node[length];
	for(int i = 0; i < length; i++){
	    tree[i] = new Prefix_Node(max_Radix);
	}
    }
	
    public synchronized void waitPauze(int node)throws RemoteException{
	while(!tree[node].done){
	    try{
		wait();
	    }catch(Exception e){
		System.out.println(" Problem in wait" + e.getMessage());
	    } 
	}
	tree[node].waitPauze();
    }
	
    public synchronized void clearPauze(int node)throws RemoteException{
	tree[node].clearPauze();
	try{
	    notify();
	}catch(Exception e){
	    System.out.println(" Problem in clearPauze" + e.getMessage());
	}
    }
	
    public synchronized void setPauze(int node)throws RemoteException{	
	tree[node].setPauze();
    }
	
    public synchronized void putDensity(int node, int[] density)throws RemoteException{
	tree[node].densities = density;
    }
	
    public synchronized void putRank(int node, int[] rank)throws RemoteException{
	tree[node].ranks = rank;
    }
	
    public synchronized void putSet(int node, int[] density, int[] rank)throws RemoteException{
	tree[node].densities = density;
	tree[node].ranks = rank;
    }
	
    public int[] getDensity(int node)throws RemoteException{
	return tree[node].densities;
    }
	
    public int[] getRank(int node)throws RemoteException{
	return tree[node].ranks;
    }
	
}
