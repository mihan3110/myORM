package myOrmTest;


import myOrmTest.db.DbConnect;
import myOrmTest.db.EntityManager;
import myOrmTest.entities.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

public class Main {
    public static void main(String[] args) throws ParseException {

        EntityManager em = null;
        Connection connection = null;

        try {

         //Подключаемся
          //  DbConnect.initConnection("sqlserver", "root", "123456", "localhost", "1433", "myOrm");
            DbConnect.initConnection("mysql", "root", "123456", "localhost", "3306", "myOrm");
            connection = DbConnect.getConnection();

            em = new EntityManager(connection);
            System.out.println(1);
            //Создаем сущность и зsаполняем ее
            User user = new User("Ivan", "Ivanov", 27,new Date());
            System.out.println(2);
            em.persist(user);


            //Поиск 1-го
            User newUser = em.findFirst(User.class);
            System.out.println(String.format("Id: %d, Usernmae: %s, Password: %s, Age: %d, Register date: %s",
                    newUser.getId(), newUser.getUsername(), newUser.getPassword(), newUser.getAge(), newUser.getRegistrationDate().toString()));
            // Update
            newUser.setPassword("Update");
            em.persist(newUser);

            //Поиск
            em.find(User.class).forEach(e -> System.out.println(String.format("Id: %d, Usernmae: %s, Password: %s, Age: %d, Register date: %s",
                    e.getId(), e.getUsername(), e.getPassword(), e.getAge(), e.getRegistrationDate().toString())));

            //Удаление
            em.delete(User.class,2L);


        }catch (SQLException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        } finally {
            try {
                if (em != null) {
                    em.closeConnections();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}