package misc.peers;

import java.util.LinkedList;
import java.util.Queue;

public class ClientState extends State{
    public boolean isSeeder = false;  //100% seeder
    public Queue<Integer> piecesToRequest = new LinkedList<>();

    public ClientState(int numPieces) {
        super(numPieces);
    }


    public byte[] getBitfield(){
        return this.bitfield.value;
    }
}
