package ca.sfu.teambeta.persistence;

import ca.sfu.teambeta.accounts.UserRole;
import ca.sfu.teambeta.core.*;
import ca.sfu.teambeta.core.exceptions.AccountRegistrationException;
import ca.sfu.teambeta.core.exceptions.IllegalDatabaseOperation;
import ca.sfu.teambeta.core.exceptions.NoSuchUserException;
import ca.sfu.teambeta.logic.GameSession;
import ca.sfu.teambeta.logic.VrcLadderReorderer;
import ca.sfu.teambeta.logic.VrcScorecardGenerator;
import ca.sfu.teambeta.logic.VrcTimeSelection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Utility class that reads and writes data to the database
 */
public class DBManager {
    private static final String LOCAL_TESTING_CFG_XML = "hibernate.testing.cfg.xml";
    private static final String HIBERNATE_CLASSES_XML = "hibernate.classes.xml";
    private static final String PRODUCTION_CFG_XML = "hibernate.production.cfg.xml";
    private static final String DOCKER_CFG_XML = "hibernate.docker.cfg.xml";
    private static final String H2_CFG_XML = "hibernate.h2.cfg.xml";
    private static String TESTING_ENV_VAR = "TESTING";
    private Session session;

    public DBManager(SessionFactory factory) {
        this.session = factory.openSession();
    }

    // Used for testing purposes where session needs to be closed
    public DBManager(Session session) {
        this.session = session;
    }

    // Use me if the database is down
    public static SessionFactory getHSQLSession() {
        Configuration config = new Configuration();
        config.configure(H2_CFG_XML);
        config.configure(HIBERNATE_CLASSES_XML);
        try {
            return config.buildSessionFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static SessionFactory getMySQLSession(boolean create) {
        Configuration config = new Configuration();
        config.configure(LOCAL_TESTING_CFG_XML);
        config.configure(HIBERNATE_CLASSES_XML);
        if (create) {
            config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        } else {
            config.setProperty("hibernate.hbm2ddl.auto", "update");
        }
        try {
            return config.buildSessionFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static SessionFactory getProductionSession() {
        Configuration config = new Configuration();
        config.configure(PRODUCTION_CFG_XML);
        config.configure(HIBERNATE_CLASSES_XML);
        try {
            return config.buildSessionFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static SessionFactory getTestingSession(boolean create) {
        boolean isTesting = System.getenv(TESTING_ENV_VAR) != null;
        Configuration config = new Configuration();
        if (isTesting) {
            config.configure(DOCKER_CFG_XML);
        } else {
            config.configure(LOCAL_TESTING_CFG_XML);
        }
        config.configure(HIBERNATE_CLASSES_XML);
        if (create) {
            config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        } else {
            config.setProperty("hibernate.hbm2ddl.auto", "update");
        }
        try {
            return config.buildSessionFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    public synchronized void persistEntity(Persistable entity) {
        Transaction tx = session.beginTransaction();
        try {
            session.saveOrUpdate(entity);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            e.printStackTrace();
        }
    }

    private Persistable getEntityFromID(Class persistable, int id) throws HibernateException {
        Transaction tx = null;
        Persistable entity = null;
        try {
            tx = session.beginTransaction();
            entity = (Persistable) session.load(persistable, id);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
        return entity;
    }

    public synchronized Player getPlayerFromID(int id) {
        Player player = null;
        try {
            player = (Player) getEntityFromID(Player.class, id);
        } catch (HibernateException e) {
            e.printStackTrace();
        }
        return player;
    }

    public synchronized Pair getPairFromID(int id) {
        Pair pair = null;
        try {
            pair = (Pair) getEntityFromID(Pair.class, id);
        } catch (HibernateException e) {
            e.printStackTrace();
        }
        return pair;
    }

    public synchronized Ladder getLatestLadder() {
        Transaction tx = null;
        Ladder ladder = null;
        try {
            tx = session.beginTransaction();
            DetachedCriteria maxId = DetachedCriteria.forClass(Ladder.class)
                    .setProjection(Projections.max("id"));
            ladder = (Ladder) session.createCriteria(Ladder.class)
                    .add(Property.forName("id").eq(maxId))
                    .uniqueResult();
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
        return ladder;
    }

    private synchronized GameSession getGameSessionByVersion(GameSessionVersion version) {
        Transaction tx = null;
        List gameSessions = null;
        GameSession gameSession = null;
        try {
            tx = session.beginTransaction();
            DetachedCriteria idCriteria = DetachedCriteria.forClass(GameSession.class)
                    .setProjection(Projections.id());
            gameSessions = session.createCriteria(GameSession.class)
                    .add(Property.forName("id").in(idCriteria))
                    .addOrder(Order.desc("timestamp"))
                    .list();
            tx.commit();

            int gameSessionIndex = -1;

            if (version == null || version == GameSessionVersion.CURRENT) {
                gameSessionIndex = 0;
            } else if (version == GameSessionVersion.PREVIOUS) {
                gameSessionIndex = 1;
            }

            gameSession = (GameSession) gameSessions.get(gameSessionIndex);

        } catch (HibernateException e) {
            tx.rollback();
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        return gameSession;
    }

    public synchronized GameSession getGameSessionLatest() {
        return getGameSessionByVersion(GameSessionVersion.CURRENT);
    }

    public synchronized GameSession getGameSessionPrevious() {
        return getGameSessionByVersion(GameSessionVersion.PREVIOUS);
    }

    public void addPenaltyToPair(GameSession gameSession, int pairId, Penalty penalty) {
        Pair pair = getPairFromID(pairId);
        gameSession.setPenaltyToPair(pair, penalty);
    }

    public synchronized void addPairToLatestLadder(Pair pair) {
        Transaction tx = null;
        Ladder ladder = null;
        try {
            tx = session.beginTransaction();
            DetachedCriteria maxId = DetachedCriteria.forClass(Ladder.class)
                    .setProjection(Projections.max("id"));
            ladder = (Ladder) session.createCriteria(Ladder.class)
                    .add(Property.forName("id").eq(maxId))
                    .uniqueResult();
            ladder.insertAtEnd(pair);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
    }

    public synchronized void addPair(GameSession gameSession, Pair pair, int position) {
        gameSession.addNewPairAtIndex(pair, position);
        persistEntity(gameSession);
    }

    public synchronized void addPair(GameSession gameSession, Pair pair) {
        gameSession.addNewPairAtEnd(pair);
        persistEntity(gameSession);
    }

    public synchronized boolean removePair(int pairId) {
        GameSession gameSession = getGameSessionLatest();
        Transaction tx = session.beginTransaction();
        Pair pair = null;
        try {
            pair = session.load(Pair.class, pairId);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
            throw e;
        }

        boolean removed = gameSession.removePairFromLadder(pair);

        persistEntity(gameSession);

        return removed;
    }

    public synchronized boolean hasPairID(int id) {
        return getPairFromID(id) != null;
    }

    public synchronized void movePair(GameSession gameSession, int pairId, int newPosition) {
        Pair pair = getPairFromID(pairId);
        int previousPosition = pair.getLastWeekPosition();
        removePair(pairId);
        gameSession.addNewPairAtIndex(pair, newPosition);
        pair.setLastWeekPosition(previousPosition);
        persistEntity(gameSession);
    }

    public synchronized Player getAlreadyActivePlayer(
            GameSession gameSession, int id) throws Exception {
        Pair pair = getPairFromID(id);
        Player player;
        try {
            player = gameSession.getAlreadyActivePlayer(pair);
        } catch (Exception e) {
            throw e;
        }
        return player;
    }

    public synchronized boolean setPairActive(GameSession gameSession, int pairId) {
        Pair pair = getPairFromID(pairId);
        boolean activated = gameSession.setPairActive(pair);
        gameSession.setTimeSlot(pair, Time.NO_SLOT);
        gameSession.createGroups(new VrcScorecardGenerator(), new VrcTimeSelection());
        persistEntity(gameSession);
        return activated;
    }

    public synchronized void setPairInactive(GameSession gameSession, int pairId) {
        Pair pair = getPairFromID(pairId);
        gameSession.setPairInactive(pair);
        gameSession.createGroups(new VrcScorecardGenerator(), new VrcTimeSelection());
        persistEntity(gameSession);
    }

    public synchronized boolean isActivePair(GameSession gameSession, int pairId) {
        Pair pair = getPairFromID(pairId);

        boolean status = gameSession.isActivePair(pair);
        persistEntity(gameSession);
        return status;
    }

    public synchronized int getLadderSize(GameSession gameSession) {
        List<Pair> ladder = gameSession.getAllPairs();
        return ladder.size();
    }

    public synchronized String getJSONLadder(GameSession gameSession) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(GameSession.class, new LadderJSONSerializer())
                .create();
        return gson.toJson(gameSession);
    }

    public synchronized String getJSONScorecards(GameSession gameSession) {
        List<Scorecard> scorecards = gameSession.getScorecards();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Scorecard.class, new ScorecardSerializer())
                .create();

        return gson.toJson(scorecards);
    }

    public synchronized String getJSONSession(String sessionToken) {
        //ex: {"email":"test@gmail.com","admin":true}
        JSONSerializer serializer = new SessionJSONSerializer(sessionToken);
        return serializer.toJson();
    }

    private GameSession getGameSession(int gameSessionId) {
        Transaction tx = null;
        GameSession gameSession = null;
        try {
            tx = session.beginTransaction();
            gameSession = (GameSession) session.createCriteria(GameSession.class)
                    .add(Property.forName("id").eq(gameSessionId))
                    .uniqueResult();
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
        return gameSession;
    }

    public synchronized User getUser(String email) {
        Transaction tx = null;
        User user = null;
        try {
            tx = session.beginTransaction();
            user = (User) session.createCriteria(User.class)
                    .add(Restrictions.eq("email", email))
                    .uniqueResult();
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
        return user;
    }

    public synchronized List<User> getAllUsersOfRole(UserRole role) {
        Transaction tx = null;
        List<User> anonymousUsers = null;

        try {
            tx = session.beginTransaction();
            anonymousUsers = session.createCriteria(User.class).add(Restrictions.eq("role", role)).list();
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
        return anonymousUsers;
    }

    public synchronized void deleteUser(String userEmail) throws NoSuchUserException, IllegalDatabaseOperation {

        User user = getUser(userEmail);

        if (user == null) {
            throw new NoSuchUserException("No user exists for email: " + userEmail);
        }

        if (user.getUserRole() == UserRole.ADMINISTRATOR) {
            throw new IllegalDatabaseOperation("Cannot delete an administrator");
        }

        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.delete(user);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
    }

    public synchronized void addNewUser(User user) throws AccountRegistrationException {
        String email = user.getEmail();
        boolean uniqueEmail = (getUser(email) == null);
        if (!uniqueEmail) {
            throw new AccountRegistrationException("The email '" + email + "' is already in use");
        }

        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.saveOrUpdate(user);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
    }

    public synchronized void updateExistingUser(User user) {

        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.saveOrUpdate(user);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
    }

    public synchronized void addNewPlayer(Player player) throws AccountRegistrationException {
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.saveOrUpdate(player);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
    }

    // Rankings are in a format of pairID -> position in scorecard
    public synchronized void setMatchResults(int scorecardId, Map<Integer, Integer> rankings) {
        Scorecard sc = (Scorecard) getEntityFromID(Scorecard.class, scorecardId);
        for (Map.Entry<Integer, Integer> entry : rankings.entrySet()) {
            Pair pair = getPairFromID(entry.getKey());
            sc.setGameResults(pair, entry.getValue());
        }
        persistEntity(sc);
    }

    public synchronized void reorderLadder(GameSession gameSession) {
        gameSession.reorderLadder(new VrcLadderReorderer(), new VrcTimeSelection());
    }

    public synchronized GameSession createNewGameSession(GameSession sourceGameSession) {
        Ladder nextWeekLadder = sourceGameSession.getReorderedLadder();
        return new GameSession(nextWeekLadder);
    }

    public synchronized void saveGameSession(GameSession gameSession) {
        persistEntity(gameSession);
    }

    public synchronized void setTimeSlot(int pairId, Time time) {
        GameSession gameSession = getGameSessionLatest();
        Pair pair = getPairFromID(pairId);
        gameSession.setTimeSlot(pair, time);
        persistEntity(gameSession);
    }

    public boolean writeToCsvFile(OutputStream outputStream, GameSession gameSession) {
        try {
            CSVReader.exportCsv(outputStream, gameSession.getAllPairs());
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public synchronized boolean importLadderFromCsv(InputStreamReader inputStreamReader) {
        List<Integer> pairIds;
        try {
            pairIds = CSVReader.getPairIdsFromCsvStream(inputStreamReader);
        } catch (Exception e) {
            return false;
        }
        Ladder ladder = new Ladder();
        for (int id : pairIds) {
            Pair pair = getPairFromID(id);
            ladder.insertAtEnd(pair);
        }
        GameSession gameSession = getGameSessionLatest();
        gameSession.replaceLadder(ladder);
        persistEntity(gameSession);
        return true;
    }

    public enum GameSessionVersion {
        CURRENT,
        PREVIOUS
    }
}
