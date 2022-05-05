package netty.springboot.multipersonchatroom.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

    public WsMessage(int t, String n, String body) {
        this.t = t;
        this.n = n;
        this.body = body;
        this.err = 0;
    }
}