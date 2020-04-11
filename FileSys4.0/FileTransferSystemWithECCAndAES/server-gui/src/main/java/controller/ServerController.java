package controller;

import com.yantong.filesys.server.Server;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class ServerController {
    public ListView clientMessagesListView;
    public Button turnOnBTN;
    Server server;
    private HashSet<String> addedClientInfoStrings;

    public void onTurnOnServer(MouseEvent mouseEvent) throws IOException {
        this.server = new Server();
        server.start();
        Server.serverPromptMessages.forEach(message -> {
            clientMessagesListView.getItems().add(message);
        });
        addedClientInfoStrings = new HashSet<>();
        turnOnBTN.setDisable(true);
    }

    public void onTurnOffServer(MouseEvent mouseEvent) {
        server.stop();
        System.exit(0);
    }

    public void onRefreshClick(MouseEvent mouseEvent) {
        List<String> clientInfoStrings = server.getClientInfoMessage().getClientPublicInfoListString();
        clientInfoStrings.forEach(clientInfo -> {
            if(!addedClientInfoStrings.contains(clientInfo)) {
                addedClientInfoStrings.add(clientInfo);
                clientMessagesListView.getItems().add(clientInfo);
            }
        });
    }

}
