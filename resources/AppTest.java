public class AppTest 
{
    static int x = 1;
    static int y = 10;
    static int z = 100;

    public static void check_1() {
        int k = 100; // Check for filter of local variables.
		if (x < y && x != k && z == k) {
            return;
        }
    }

    public static void check_2() {
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

    public static class Another {
        int a;
        int b;
    }

    public static void main(String[] args) {
        check_1();
        check_2();
    }
}

