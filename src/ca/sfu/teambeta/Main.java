package ca.sfu.teambeta;

import org.hibernate.SessionFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.sfu.teambeta.core.Ladder;
import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.Player;
import ca.sfu.teambeta.logic.GameManager;
import ca.sfu.teambeta.logic.LadderManager;
import ca.sfu.teambeta.persistence.DBManager;
import ca.sfu.teambeta.ui.UserInterface;


class Main {
    public static void main(String[] args) {

        /*Laddgiter loadedLadder = DBManager.loadFromDB();*/

        /* -----FOR TESTING*/
        List<Pair> ladderPairs = Arrays.asList(
                new Pair(new Player("Bobby", "Chan", ""), new Player("Wing", "Man", ""), false),
                new Pair(new Player("Ken", "Hazen", ""), new Player("Brian", "Fraser", ""), false),
                new Pair(new Player("Simon", "Fraser", ""), new Player("Dwight", "Howard", ""), false),
                new Pair(new Player("Bobby", "Chan", ""), new Player("Big", "Head", ""), false),
                new Pair(new Player("Alex", "Land", ""), new Player("Test", "Player", ""), false)
        );
        Ladder newLadder = new Ladder(ladderPairs);

        SessionFactory sessionFactory = DBManager.getMySQLSession(true);
        DBManager dbManager = new DBManager(sessionFactory);

        dbManager.persistEntity(newLadder);
        Ladder loadedLadder = dbManager.getLatestLadder();

        List<Pair> loadedLadderPairs = new ArrayList<>();
        loadedLadderPairs.addAll(loadedLadder.getPairs());

        LadderManager ladderManager = new LadderManager(loadedLadderPairs);

        ladderManager.getLadder().forEach(ladderManager::setIsPlaying);

        GameManager gameManager = new GameManager(ladderManager.getActivePairs(), ladderManager);
        //UserInterface.start(gameManager,ladderManager);
        AppController appController = new AppController(ladderManager, gameManager);


    }
}
