package misc.peers;

public class DEBUG {

    public static boolean flag = true;

    public static final void log(String ...strings) {
        if (!flag)
            return ;
        for (int i = 0; i < strings.length ; i++) {
            System.out.print(strings[i] + " / ");
        }
        System.out.println("");
    }
}
