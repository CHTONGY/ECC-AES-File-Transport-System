package controller;

import com.yantong.filesys.client.Client;
import com.yantong.filesys.client.TCPClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ClientController {
    @FXML
    public ListView messageListView;
    public MenuButton clientMenuBar;
    public TextField clientNameTextField;
    public TextField sendFilePathTextField;
    public TextField receiveFilePatTextField;
    public Button connectBTN;

    private List<String> guiPromptMessages;
    private HashSet<String> addedClient;

    private static void printOnListView(List<String> messagesList, ListView messageListView) {
        messagesList.forEach(message -> {
            messageListView.getItems().add(message);
        });
    }

    public void onConnectClick(MouseEvent mouseEvent) {
        String name = clientNameTextField.getText();
        Client.start(name);
        printOnListView(Client.promptMessages, messageListView);
        Client.promptMessages.clear();
        addedClient = new HashSet<>();
        guiPromptMessages = new ArrayList<>();
        guiPromptMessages.add("Update neighbors:");
        removeAllClientMenuBarItems(clientMenuBar);

        connectBTN.setDisable(true);
    }

    private void removeAllClientMenuBarItems(MenuButton clientMenuBar) {
        int size = clientMenuBar.getItems().size();
        for (int i = 0; i < size; i++) {
            clientMenuBar.getItems().remove(0);
        }
    }

    public void onEncryptAndSendClick(MouseEvent mouseEvent) {
        String receiverName = clientMenuBar.getText();
        String filePath = sendFilePathTextField.getText();
        try {
            TCPClient.encryptAndSendFile(receiverName, filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        printOnListView(Client.promptMessages, messageListView);
        Client.promptMessages.clear();
    }

    public void onReceiveAndDecryptClick(MouseEvent mouseEvent) {
        String savePath = receiveFilePatTextField.getText();
        try {
            TCPClient.receiveAndDecryptFile(savePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        printOnListView(Client.promptMessages, messageListView);
        Client.promptMessages.clear();
    }

    public void onExitClick(MouseEvent mouseEvent) {
        System.exit(0);
    }

    public void onRefreshClientListClick(MouseEvent mouseEvent) {
        Client.clientInfo.getOtherPartyNames().forEach(name -> {
            if (!name.equals(Client.clientInfo.getName()) && !addedClient.contains(name)) {
                addedClient.add(name);
                MenuItem item = new MenuItem(name);
                clientMenuBar.getItems().add(item);
                guiPromptMessages.add(name);
                item.setOnAction(e -> {
                    clientMenuBar.setText(item.getText());
                });
            }
        });
        printOnListView(guiPromptMessages, messageListView);
    }
}
