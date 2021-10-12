package test;
import misc.TrackerHandler;

public class TestTracker {

	public static void main(String[] args) {
		
		TrackerHandler tracker = new TrackerHandler(null, null, null, 0);
		String peerId = tracker.genPeerId();
		System.out.println(peerId.concat("x"));
	}

}
