package online.cszt0.jpmahjong.socket;

import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/game")
@Component
public class GamePlayerSocket {

    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        session.setMaxIdleTimeout(60000);
        System.out.println("onOpen()");
    }

    @OnClose
    public void onClose() {
        System.out.println("onClose()");
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("onMessage: " + message);
        session.getAsyncRemote().sendText(message);
    }

    @OnError
    public void onError(Throwable e) {
        e.printStackTrace();
    }
}
