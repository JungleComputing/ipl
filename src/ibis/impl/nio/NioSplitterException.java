package ibis.impl.nio;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.ArrayList;

class NioSplitterException extends IOException {

    private ArrayList channels = new ArrayList();
    private ArrayList exceptions = new ArrayList();

    NioSplitterException() {
    }

    void add(Channel channel, Exception e) {
	channels.add(channel);
	exceptions.add(e);
    }

    int count() {
	return channels.size();
    }

    Channel getChannel(int pos) {
	return (Channel) channels.get(pos);
    }

    Exception getException(int pos) {
	return (Exception) exceptions.get(pos);
    }

    public String toString() {
	String res = "got " + channels.size() + " exceptions: \n";
	for(int i = 0; i < channels.size();i++) {
	    res += "   " + exceptions.get(i) + "\n";
	}

	return res;
    }
}

