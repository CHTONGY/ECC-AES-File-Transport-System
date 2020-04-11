package com.yantong.filesys.server;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class BroadcastMessage {
    private List<JsonObject> clientPublicInfoList;

    public BroadcastMessage() {
        clientPublicInfoList = new ArrayList<>();
    }

    public JsonArrayBuilder toJsonArrayBuilder() {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for(JsonObject clientInfo : clientPublicInfoList) {
            jsonArrayBuilder.add(clientInfo);
        }
        return jsonArrayBuilder;
    }

    public List<JsonObject> getClientPublicInfoList() {
        return clientPublicInfoList;
    }

    public List<String> getClientPublicInfoListString() {
        List<String> infoListString = new ArrayList<>();
        for(JsonObject info : clientPublicInfoList) {
            infoListString.add(info.toString());
        }
        return infoListString;
    }

    public void addClientPublicInfo(JsonObject clientPublicInfo) {
        this.clientPublicInfoList.add(clientPublicInfo);
    }
}
