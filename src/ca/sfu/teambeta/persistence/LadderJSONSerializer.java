package ca.sfu.teambeta.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Set;

import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.Player;

/**
 * Created by Gordon Shieh on 30/06/16.
 */
public class LadderJSONSerializer implements JSONSerializer {
    List<Pair> pairList;
    Set<Pair> activePairs;

    LadderJSONSerializer(List<Pair> pairList, Set<Pair> activePairs) {
        this.pairList = pairList;
        this.activePairs = activePairs;
    }

    private JsonObject getPairJsonObject(Pair pair, int position, boolean isPlaying) {
        JsonObject pairJson = new JsonObject();
        JsonArray playersArray = new JsonArray();
        for (Player player : pair.getPlayers()) {
            JsonObject playerJson = new JsonObject();
            playerJson.addProperty("firstName", player.getFirstName());
            playerJson.addProperty("lastName", player.getLastName());
            playerJson.addProperty("id", player.getID());
            playersArray.add(playerJson);
        }
        pairJson.add("players", playersArray);
        pairJson.addProperty("id", pair.getID());
        pairJson.addProperty("pairScore", pair.getPairScore());
        pairJson.addProperty("position", position);
        pairJson.addProperty("isPlaying", isPlaying);
        return pairJson;
    }

    @Override
    public String toJson() {
        JsonArray pairsArray = new JsonArray();
        int position = 1;
        for (Pair pair : pairList) {
            JsonObject pairJson = getPairJsonObject(pair, position,
                    activePairs.contains(pair));
            position++;
            pairsArray.add(pairJson);
        }
        return pairsArray.toString();
    }
}
