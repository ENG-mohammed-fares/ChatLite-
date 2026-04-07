package protocol;

public class CommandParser {

    public static String getCommand(String input) {
        return input.split(" ")[0];
    }
}