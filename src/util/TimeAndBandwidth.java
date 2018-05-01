package util;

public class TimeAndBandwidth {

	public Bandwidth[] offline_band;
	public Bandwidth[] online_band;
	public Timer[] timer;

	public TimeAndBandwidth() {
		offline_band = new Bandwidth[P.size];
		online_band = new Bandwidth[P.size];
		timer = new Timer[P.size];
		for (int i = 0; i < P.size; i++) {
			offline_band[i] = new Bandwidth(P.names[i] + "_off");
			online_band[i] = new Bandwidth(P.names[i] + "_on");
			timer[i] = new Timer(P.names[i]);
		}
	}

	public void resetTime() {
		for (Timer t : timer)
			t.reset();
	}
}
