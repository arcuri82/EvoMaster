package org.evomaster.client.java.controller.internal.db.constraint;


import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class H2ConstraintExtractor extends TableConstraintExtractor {

    /**
     * Expects the schema explained in
     * http://www.h2database.com/html/systemtables.html#information_schema
     *
     * @param connectionToH2 a connection to a H2 database
     * @param schemaDto      a DTO schema with retrieved information from the JBDC metada
     * @throws Exception
     */
    public List<DbTableConstraint> extract(Connection connectionToH2, DbSchemaDto schemaDto) throws SQLException {

        List<DbTableConstraint> columnConstraints = extractColumnConstraints(connectionToH2, schemaDto);

        List<DbTableConstraint> tableCheckExpressions = extractTableConstraints(connectionToH2, schemaDto);

        List<DbTableConstraint> allConstraints = new ArrayList<>();
        allConstraints.addAll(columnConstraints);
        allConstraints.addAll(tableCheckExpressions);
        return allConstraints;
    }

    /**
     * Logs that a constraint could not be handled by the extractor.
     *
     * @param constraintType
     */
    private static void cannotHandle(String constraintType) {
        SimpleLogger.uniqueWarn("WARNING, EvoMaster cannot extract H2 constraints with type '" + constraintType);
    }

    /**
     * For each table in the schema DTO, this method appends
     * the constraints that are originated in the ALTER TABLE commands
     * for those particular tables.
     * <p>
     * Foreign keys are handled separately in the JDBC metadata
     *
     * @param connectionToH2 a connection to a H2 database
     * @param schemaDto
     * @throws SQLException if the connection to the H2 database fails,
     */
    private List<DbTableConstraint> extractTableConstraints(Connection connectionToH2, DbSchemaDto schemaDto) throws SQLException {

        List<DbTableConstraint> tableCheckExpressions = new ArrayList<>();

        String tableSchema = schemaDto.name;
        for (TableDto tableDto : schemaDto.tables) {
            String tableName = tableDto.name;
            try (Statement statement = connectionToH2.createStatement()) {

                final String query = String.format("Select * From INFORMATION_SCHEMA.CONSTRAINTS where CONSTRAINTS.TABLE_SCHEMA='%s' and CONSTRAINTS.TABLE_NAME='%s' ", tableSchema, tableName);
                try (ResultSet constraints = statement.executeQuery(query)) {

                    while (constraints.next()) {
                        String constraintType = constraints.getString("CONSTRAINT_TYPE");
                        String sqlCheckExpression = constraints.getString("CHECK_EXPRESSION");
                        String columnList = constraints.getString("COLUMN_LIST");

                        if (constraintType.equals("UNIQUE")) {
                            assert (sqlCheckExpression == null);
                            List<String> uniqueColumnNames = Arrays.stream(columnList.split(",")).map(String::trim).collect(Collectors.toList());
                            DbTableUniqueConstraint uniqueConstraint = new DbTableUniqueConstraint(tableName, uniqueColumnNames);
                            tableCheckExpressions.add(uniqueConstraint);

                        } else if (constraintType.equals("REFERENTIAL")) {
                            /**
                             * This type of constraint is already handled by
                             * JDBC Metadata
                             **/
                        } else if (constraintType.equals("PRIMARY KEY") || constraintType.equals("PRIMARY_KEY")) {
                            /**
                             * This type of constraint is already handled by
                             * JDBC Metadata
                             **/
                        } else if (constraintType.equals("CHECK")) {
                            assert (columnList == null);

                            DbTableCheckExpression constraint = new DbTableCheckExpression(tableName, sqlCheckExpression);
                            tableCheckExpressions.add(constraint);

                        } else {
                            cannotHandle(constraintType);
                        }
                    }
                }
            }
        }

        return tableCheckExpressions;
    }

    /**
     * For each table in the schema DTO, this method appends
     * the constraints that are originated in the CREATE TABLE commands
     * for those particular tables.
     * <p>
     * Unique constraints and Foreign keys are handled separately in the JDBC metadata
     *
     * @param connectionToH2 a connection to a H2 database
     * @param schemaDto
     * @throws SQLException if the connection to the database fails
     */
    private List<DbTableConstraint> extractColumnConstraints(Connection connectionToH2, DbSchemaDto schemaDto) throws SQLException {
        String tableSchema = schemaDto.name;
        List<DbTableConstraint> columnConstraints = new ArrayList<>();
        for (TableDto tableDto : schemaDto.tables) {
            String tableName = tableDto.name;

            try (Statement statement = connectionToH2.createStatement()) {

                final String query = String.format("Select * From INFORMATION_SCHEMA.COLUMNS where COLUMNS.TABLE_SCHEMA='%s' and COLUMNS.TABLE_NAME='%s' ", tableSchema, tableName);

                try (ResultSet columns = statement.executeQuery(query)) {
                    while (columns.next()) {
                        String sqlCheckExpression = columns.getString("CHECK_CONSTRAINT");
                        if (sqlCheckExpression != null && !sqlCheckExpression.equals("")) {
                            DbTableCheckExpression constraint = new DbTableCheckExpression(tableName, sqlCheckExpression);
                            columnConstraints.add(constraint);
                        }
                    }
                }
            }
        }
        return columnConstraints;
    }

}

