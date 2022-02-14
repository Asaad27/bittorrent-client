package com.messages;
import java.io.IOException;
import java.io.OutputStream;
public interface Framer {
	void frameMessage(byte[] msg, OutputStream out) throws IOException;
	byte[] ReadnextMsg() throws IOException;
}
