package myOrmTest.db;

import java.sql.SQLException;
import java.util.Set;

public interface DbContext {
    /** insert или update  **/
    <E> boolean persist(E entity) throws IllegalAccessException, SQLException;

    /** Поиск всех элементов */
    <E> Set<E> find(Class<E> table) throws SQLException, IllegalAccessException, InstantiationException;

    /** Поиск элементов по условию*/
    <E> Set<E> find(Class<E> table, String where) throws SQLException, IllegalAccessException, InstantiationException;

    /** Поиск первого в списке объекта */
    <E> E findFirst(Class<E> table) throws SQLException, IllegalAccessException, InstantiationException;

    /** Поиск первого элемента по условию*/
    <E> E findFirst(Class<E> table, String where) throws SQLException, IllegalAccessException, InstantiationException;

    /** Удаление сущности по id **/
    <E> boolean delete(Class<E> table, Long id) throws SQLException, IllegalAccessException;

    /** Закрывает соединение **/
    void closeConnections() throws SQLException;
}