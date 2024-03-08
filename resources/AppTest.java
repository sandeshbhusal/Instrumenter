public class AppTest {
    int x = 1;
    int y = 10;
    int z = 100;

    public void check_1() {
        int k = 100; // Check for filter of local variables.
	      
        if (x < y && x != k && z == k) {
            return;
        }
    }

    public void check_2() {
        if (x == y) {
            return;
        } else {
            if (x == z) {
                y += x;
            } else {
                y *= x;
            }
        }
    }

    public class Another {
        int a;
        int b;
    }
}