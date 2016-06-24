package ca.sfu.teambeta.persistence;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;

import ca.sfu.teambeta.core.Game;
import ca.sfu.teambeta.core.Ladder;
import ca.sfu.teambeta.core.Pair;
import ca.sfu.teambeta.core.Player;
import ca.sfu.teambeta.core.Scorecard;
import ca.sfu.teambeta.logic.GameSession;

/**
 * Utility class that reads and writes data to the database
 */
public class DBManager {
    private static String TESTING_ENV_VAR = "TESTING";
    private SessionFactory factory;
    private Session session;

    public DBManager(SessionFactory factory) {
        this.factory = factory;
        this.session = factory.openSession();
    }

    private static Configuration getDefaultConfiguration() {
        Configuration config = new Configuration();
        config.addAnnotatedClass(Player.class);
        config.addAnnotatedClass(Pair.class);
        config.addAnnotatedClass(Ladder.class);
        config.addAnnotatedClass(Scorecard.class);
        config.addAnnotatedClass(Game.class);
        config.addAnnotatedClass(GameSession.class);
        return config;
    }

    public static SessionFactory getHSQLSession() {
        Configuration config = getDefaultConfiguration();
        config.setProperty("hibernate.hbm2ddl.auto", "update");
        config.setProperty("hibernate.connection.username", "");
        config.setProperty("hibernate.connection.password", "");
        config.setProperty("hibernate.connection.pool_size", "1");
        config.setProperty("hibernate.connection.url", "jdbc:hsqldb:file:/home/freeman/prj/resources/database/test");
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        config.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        return config.buildSessionFactory();
    }

    public static SessionFactory getMySQLSession(boolean create) {
        Configuration config = getDefaultConfiguration();
        if (create) {
            config.setProperty("hibernate.hbm2ddl.auto", "create");
        } else {
            config.setProperty("hibernate.hbm2ddl.auto", "update");
        }
        config.setProperty("hibernate.connection.username", "beta-test");
        config.setProperty("hibernate.connection.password", "b3ta");
        config.setProperty("hibernate.connection.pool_size", "1");
        config.setProperty("hibernate.connection.url", "jdbc:mysql://cmpt373-beta.csil.sfu.ca:3306/test?serverTimezone=America/Vancouver");
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        config.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
        try {
            return config.buildSessionFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static SessionFactory getDockerSession(boolean create) {
        Configuration config = getDefaultConfiguration();
        if (create) {
            config.setProperty("hibernate.hbm2ddl.auto", "create");
        } else {
            config.setProperty("hibernate.hbm2ddl.auto", "update");
        }
        config.setProperty("hibernate.connection.username", "root");
        config.setProperty("hibernate.connection.password", "b3ta");
        config.setProperty("hibernate.connection.pool_size", "1");
        config.setProperty("hibernate.connection.url", "jdbc:mysql://mysql:3306/test?serverTimezone=America/Vancouver");
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        config.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
        try {
            return config.buildSessionFactory();
        } catch (Exception ex) {
            throw new RuntimeException();
        }
    }

    public static SessionFactory getTestingSession(boolean create) {
        boolean isTesting = System.getenv(TESTING_ENV_VAR) != null;
        if (isTesting) {
            return getDockerSession(create);
        } else {
            return getMySQLSession(create);
        }
    }

    public static void main(String[] args) {
        SessionFactory factory = getMySQLSession(false);
        DBManager dbMan = new DBManager(factory);
//        Player p1 = new Player("Bobby", "Chan", "");
//        Player p2 = new Player("Wing", "Man", "");
//        dbMan.persistEntity(new Pair(p1, p2));
//
//        Player p3 = new Player("Hello", "World!", "");
//        dbMan.persistEntity(new Pair(new Player("Bobby", "Chan", ""), p3));
//
//        Player test = dbMan.getPlayerFromID(5);

//        System.out.println(test.getFirstName());

        Ladder lad = dbMan.getLatestLadder();

        System.out.println(lad);
    }

    public int persistEntity(Persistable entity) {
        Transaction tx = null;
        int key = 0;
        try {
            tx = session.beginTransaction();
            key = (int) session.save(entity);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
        return key;
    }

    private Persistable getEntityFromID(Class persistable, int id) throws HibernateException {
        Transaction tx = null;
        Persistable entity = null;
        try {
            tx = session.beginTransaction();
            entity = (Persistable) session.get(persistable, id);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
        return entity;
    }

    public Player getPlayerFromID(int id) {
        Player player = null;
        try {
            player = (Player) getEntityFromID(Player.class, id);
        } catch (HibernateException e) {
            e.printStackTrace();
        }
        return player;
    }

    public Ladder getLatestLadder() {
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

    public void addPairToLatestLadder(Pair pair) {
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

    public void removePairFromLatestLadder(int pairID) {
        Transaction tx = null;
        Pair pair = null;
        Ladder ladder = null;
        try {
            tx = session.beginTransaction();
            pair = session.get(Pair.class, pairID);
            DetachedCriteria maxId = DetachedCriteria.forClass(Ladder.class)
                    .setProjection(Projections.max("id"));
            ladder = (Ladder) session.createCriteria(Ladder.class)
                    .add(Property.forName("id").eq(maxId))
                    .uniqueResult();
            ladder.removePair(pair);
            tx.commit();
        } catch (HibernateException e) {
            tx.rollback();
        }
    }
}
