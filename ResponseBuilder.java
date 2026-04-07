package protocol;

public class ResponseBuilder {

    public static String welcome() {
        return "200 WELCOME";
    }

    public static String joined(String room) {
        return "210 JOINED " + room;
    }

    public static String sent() {
        return "211 SENT";
    }
}