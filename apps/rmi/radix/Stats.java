import java.io.*;
import java.rmi.registry.*;

class Stats implements Serializable {

    long sortTime, totalTime, mergeTime, histogramTime, permuteTime;
    
    Stats() {
	sortTime = 0;
	totalTime = 0;
	mergeTime = 0;
	histogramTime = 0;
	permuteTime = 0;
    }	
}
