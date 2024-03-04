

import Instrumenter.Recorder;public class AppTest 
{
    static int x = 1;
    static int y = 10;
    static int z = 100;

    public static void check_1() {
        int k = 100; // Check for filter of local variables.
		Recorder.instrument(x, y, z);
		if (x < y && x != k && z == k) {
            return;
        }
    }

    public static void check_2() {
		Recorder.instrument(x, z);
		if (x == y) {
            return; 
        } else {
			Recorder.instrument(x, z);
			if (x == z) {
                y += x;
            } else {
                y *= x;
            }
        }
    }

    public static void main(String[] args) {
        check_1();
        check_2();
    }
}

