package myOrmTest.db;

import myOrmTest.annotations.Column;
import myOrmTest.annotations.Entity;
import myOrmTest.annotations.Id;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

public class EntityManager implements DbContext {

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private Connection connection;
    private Set<Object> persistedEntities;
    private Statement statement;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    /** Соединение с базой и запись всех ее сущностей в HashSet  **/
    public EntityManager(Connection connection) {
        this.connection = connection;
        this.persistedEntities = new HashSet<>();
    }

    @Override
    public <E> boolean persist(E entity) throws IllegalAccessException, SQLException {

        Field primary = this.getId(entity.getClass());
        primary.setAccessible(true);
        Object value = primary.get(entity);

        this.doCreate(entity, primary);

        if (value == null || (Long) value <= 0) {
            return this.doInsert(entity, primary);
        }

        return this.doUpdate(entity, primary);
    }

/**  SELECT без условия**/
    @Override
    @SuppressWarnings("unchecked")
    public <E> Set<E> find(Class<E> table) throws SQLException, IllegalAccessException, InstantiationException {

        this.statement = this.connection.createStatement();

        String query = "SELECT * FROM " + this.getTableName(table);

        this.resultSet = this.statement.executeQuery(query);

        if (this.persistedEntities.size() > 0) {
            this.persistedEntities.clear();
        }

        while (this.resultSet.next()) {
            E entity = table.newInstance();
            entity = this.fillEntity(table, this.resultSet, entity);
            this.persistedEntities.add(entity);
        }
        return Collections.unmodifiableSet(new HashSet<>(this.persistedEntities.stream()
                .map(e -> ((E) e)).collect(Collectors.toSet())));
    }
    /**  SELECT с условием**/
    @Override
    @SuppressWarnings("unchecked")
    public <E> Set<E> find(Class<E> table, String where) throws SQLException, IllegalAccessException, InstantiationException {
        this.statement = this.connection.createStatement();

        String query = "SELECT * FROM " + this.getTableName(table) + " WHERE 1=1 "
                + (where != null ? " AND " + where : "");

        ResultSet resultSet = this.statement.executeQuery(query);
        if (this.persistedEntities.size() > 0) {
            this.persistedEntities.clear();
        }
        while (resultSet.next()) {
            E entity = table.newInstance();
            entity = this.fillEntity(table, resultSet, entity);
            this.persistedEntities.add(entity);
        }

        return Collections.unmodifiableSet(new HashSet<>(this.persistedEntities.stream()
                .map(e -> ((E) e)).collect(Collectors.toSet())));
    }
    /**  SELECT первого элемента**/
    @Override
    public <E> E findFirst(Class<E> table) throws SQLException, IllegalAccessException, InstantiationException {

        this.statement = this.connection.createStatement();

        String query = "SELECT * FROM " + this.getTableName(table) + " LIMIT 1";

        ResultSet resultSet = this.statement.executeQuery(query);

        E entity = table.newInstance();

        resultSet.next();

        return this.fillEntity(table, resultSet, entity);
    }

    /**  SELECT первого элемента по условию**/
    @Override
    public <E> E findFirst(Class<E> table, String where) throws SQLException, IllegalAccessException, InstantiationException {

        this.statement = this.connection.createStatement();

        String query = "SELECT * FROM " + this.getTableName(table) + " WHERE 1 "
                + (where != null ? " AND " + where : "") + " LIMIT 1";

        ResultSet resultSet = this.statement.executeQuery(query);

        E entity = table.newInstance();

        resultSet.next();

        return this.fillEntity(table, resultSet, entity);
    }

    /**  Удаление элемента**/
    @Override
    public <E> boolean delete(Class<E> table, Long id) throws SQLException, IllegalAccessException {
        String query = "DELETE FROM " + this.getTableName(table)
                + " WHERE " + Mapper.getFieldName(this.getId(table)) + "= ?";
        this.preparedStatement = this.connection.prepareStatement(query);
        this.preparedStatement.setLong(1, id);
        return this.preparedStatement.execute();
    }

    public void closeConnections() throws SQLException {

        if (this.resultSet != null) {
            this.resultSet.close();
        }

        if (this.statement != null) {
            this.statement.close();
        }

        if (this.preparedStatement != null) {
            this.preparedStatement.close();
        }
    }

    /**  Возвращает имя сущности**/
    private <E> String getTableName(Class<E> entity) {

        if (entity.isAnnotationPresent(Entity.class)) {

            Entity pojo = entity.getAnnotation(Entity.class);

            return pojo.name().isEmpty() ? entity.getSimpleName() : pojo.name();
        }

        return entity.getSimpleName();
    }

    /**  Возвращает поле с id**/
    private Field getId(Class clazz) throws IllegalAccessException {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new IllegalAccessException("@Id parameter is missing");
    }

    /** Возвращает тип данных **/
    private String getDbType(Field field, Field primary) {

        field.setAccessible(true);

        if (field.getName().equals(primary.getName())) {
            return "BIGINT AUTO_INCREMENT PRIMARY KEY";
        }

        switch (field.getType().getSimpleName().toLowerCase()) {

            case "int":

                return "INT";

            case "string":
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    return "VARCHAR" + "(" + column.length() + ")";
                }

                return "VARCHAR(255)";

            case "date":
                return "DATE";
            case "boolean":
                return "BIT";
            case "double":
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    return "DOUBLE" + "(" + column.scale() + "," + column.precision() + ")";
                }
                return "DOUBLE";
        }
        return null;
    }

    /** Создает столбец если его нет **/
    private <E> boolean doCreate(E entity, Field primary) throws SQLException {

        String query = "CREATE TABLE IF NOT EXISTS " +
                this.getTableName(entity.getClass()) + " (";

        List<String> queryItems = new ArrayList<>();

        for (Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            String item = Mapper.getFieldName(field) + " " +
                    this.getDbType(field, primary);
            queryItems.add(item);
        }

        query = query + String.join(", ", queryItems) + ")";

        this.statement = this.connection.createStatement();
        return this.statement.execute(query);
    }

    /** Делает вставку **/
    private <E> boolean doInsert(E entity, Field primary) throws IllegalAccessException, SQLException {

        String query = "INSERT INTO "
                + this.getTableName(entity.getClass())
                + " " + "( ";

        List<String> columns = new ArrayList<>();

        List<String> values = new ArrayList<>();

        for (Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(Id.class)) {
                columns.add(Mapper.getFieldName(field));
                if (!field.getType().isAssignableFrom(Date.class)) {
                    values.add("\'" + field.get(entity) + "\'");
                } else {
                    values.add("\'" + DateConverter.parseDate((Date) field.get(entity), DATE_FORMAT) + "\'");
                }
            }
        }

        query = query + String.join(", ", columns) + ") " +
                "VALUES " + "(" + String.join(", ", values) + ")";

        return connection.prepareStatement(query).execute();
    }

    /** Делает обновление **/
    private <E> boolean doUpdate(E entity, Field primary) throws SQLException, IllegalAccessException {
        String query = "UPDATE " + this.getTableName(entity.getClass()) + " SET ";
        String where = " WHERE " + primary.getName() + "=?";

        List<String> rows = new ArrayList<>();

        for (Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(Id.class)) {
                String row = Mapper.getFieldName(field) + "=";
                if (!field.getType().isAssignableFrom(Date.class)) {
                    rows.add(row + "\'" + field.get(entity) + "\'");
                } else {
                    rows.add(row + "\'" + DateConverter.parseDate((Date) field.get(entity), DATE_FORMAT) + "\'");
                }
            }
        }

        query = query + String.join(", ", rows);
        Long id = (Long)primary.get(entity);

        this.preparedStatement = connection.prepareStatement(query + where);
        this.preparedStatement.setLong(1,id);
        return this.preparedStatement.execute();
    }


    /** Заполняет сущность полученными данными **/
    private <E> E fillEntity(Class<E> table, ResultSet resultSet, E entity) throws SQLException, IllegalAccessException {

        return Mapper.map(entity, resultSet, table.getDeclaredFields());

    }
}