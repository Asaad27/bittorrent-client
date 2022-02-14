package misc.messages;

import java.io.*;

public class DelimFramer implements Framer {
    private static final byte DELIMITER = 04; // message delimiter
    private final InputStream in; // data source

    public DelimFramer(InputStream in) {
        this.in = in;
    }

    public void frameMessage(byte[] message, OutputStream out) throws IOException {
        // ensure that the message does not contain the delimiter
        for (byte b : message) {
            if (b == DELIMITER) {
                throw new IOException("Message contains delimiter");
            }
        }
        out.write(message);
        out.write(DELIMITER);
        out.flush();
    }

    public byte[] ReadnextMsg() throws IOException {
        ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();
        int nextByte;

        // fetch bytes until find delimiter
        while ((nextByte = in.read()) != DELIMITER) {
            if (nextByte == -1) { // end of stream?
                if (messageBuffer.size() == 0) { // if no byte read
                    return null;
                } else { // if bytes followed by end of stream: framing error
                    throw new EOFException("Non-empty message without delimiter");
                }
            }
            messageBuffer.write(nextByte); // write byte to buffer
        }
        return messageBuffer.toByteArray();
    }
}