package netty.springboot.multipersonchatroom.pojo;

public class WsMessage {

    private int t;  //message type
    private String n;   //username
    private long roomId;   //room ID
    private String body;    //message body
    private int err;    //error code

    @Override
    public String toString() {
        return "WebSocketMessage{" +
                "messageType=" + t +
                ", username='" + n + '\'' +
                ", roomId=" + roomId +
                ", messageBody='" + body + '\'' +
                ", errorCode=" + err +
                '}';
    }

    public WsMessage(int t, String n) {
        this.t = t;
        this.n = n;
        this.err = 0;
    }

    public WsMessage(int t, String n, int err) {
        this.t = t;
        this.n = n;
        this.err = err;
    }

    public WsMessage(int t, String n, String body, int err) {
        this.t = t;
        this.n = n;
        this.body = body;
        this.err = err;
    }

    public WsMessage(int t, String n, String body) {
        this.t = t;
        this.n = n;
        this.body = body;
        this.err = 0;
    }

    public int getErr() {
        return this.err;
    }

    public void setErr(int err) {
        this.err = err;
    }

    public int getT() {
        return this.t;
    }

    public void setT(int t) {
        this.t = t;
    }

    public String getN() {
        return this.n;
    }

    public void setN(String n) {
        this.n = n;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}