package Adaptors.HelperMethods;

import Adaptors.IssueRepositories.TableColumnName;
import Model.SocialMedia.SocialMediaChannel;

import java.sql.*;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.collect.Maps.newHashMap;

public class DatabaseHelperMethods {
    private final Connection connection;

    public DatabaseHelperMethods(Connection connection) {
        this.connection = connection;
    }

    public int checkIfExits(String tableName, Map<String, TableColumnName> variablesToCheck) {
        int output = 0;
        try {
            ResultSet rs;
            Entry<String, TableColumnName> entries = variablesToCheck.entrySet().iterator().next();
            String sql = "Select id from " + tableName + " where " + entries.getValue().name() + " = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, entries.getKey());
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                output = rs.getInt("id");
            }
            rs.close();
            preparedStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    /*
    * Supports multiple string entries but not integer entries.
    * */
    public int checkIfExits(String tableName, Map<String, TableColumnName> stringFields, Map<Integer, TableColumnName> idField) {
        int output = 0;
        try {
            ResultSet rs;
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("Select id from ").append(tableName);
            Entry<Integer, TableColumnName> intEntry = idField.entrySet().iterator().next();
            sqlBuilder.append(" where ").append(intEntry.getValue()).append(" = ? ");
            for (Entry<String, TableColumnName> stringEntry : stringFields.entrySet()) {
                sqlBuilder.append(" AND ").append(stringEntry.getValue()).append(" = ?");
            }

            PreparedStatement preparedStatement = connection.prepareStatement(sqlBuilder.toString());
            preparedStatement.setInt(1, intEntry.getKey());

            int entryCount = 2;
            for (Entry<String, TableColumnName> stringEntry : stringFields.entrySet()) {
                preparedStatement.setString(entryCount, stringEntry.getKey());
                entryCount++;
            }

            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                output = rs.getInt("id");
            }
            rs.close();
            preparedStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    public int getOrCreateProject(String projectName, String projectUrl) {
        int projectId = 0;

        Map<String, TableColumnName> mappedValues = newHashMap();
        mappedValues.put(projectName, TableColumnName.projectName);

        int existingId = checkIfExits("project", mappedValues);
        if (existingId != 0) return existingId;

        try {
            String sql = "INSERT INTO project (`projectName`,\n" +
                    "`projectUrl`)" +
                    "VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setString(1, projectName);
            statement.setString(2, projectUrl);
            if (statement.executeUpdate() > 0) {
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (null != generatedKeys && generatedKeys.next()) {
                    projectId = generatedKeys.getInt(1);
                    generatedKeys.close();
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return projectId;
    }

    public int getOrCreateRepositoryUser(String authorName, String authorEmail, int issueRepositoryId) {
        int output = 0;

        Map<String, TableColumnName> authorNameMap = newHashMap();
        authorNameMap.put(authorName, TableColumnName.username);

        Map<Integer, TableColumnName> issueRepositoryIdMap = newHashMap();
        issueRepositoryIdMap.put(issueRepositoryId, TableColumnName.issueRepositoryId);

        int existingId = checkIfExits("issuerepositoryuser", authorNameMap, issueRepositoryIdMap);
        if (existingId != 0) return existingId;

        try {
            int userId = getOrCreateUser(authorName);
            int userNameTableId = getOrCreateUserName(userId, authorEmail);
            String sql = "INSERT INTO issuerepositoryuser (`userId`,\n" +
                    "`issueRepositoryId`,\n" +
                    "`username`)" +
                    "VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setInt(1, userId);
            statement.setInt(2, issueRepositoryId);
            statement.setString(3, authorName);
            if (statement.executeUpdate() > 0) {
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (null != generatedKeys && generatedKeys.next()) {
                    output = generatedKeys.getInt(1);
                    generatedKeys.close();
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return output;
    }

    private int getOrCreateUserName(int userId, String authorEmail) {
        int userNameId = 0;

        Map<String, TableColumnName> authorEmailMap = newHashMap();
        authorEmailMap.put(authorEmail, TableColumnName.userId);

        Map<Integer, TableColumnName> accountNameMap = newHashMap();
        accountNameMap.put(userId, TableColumnName.accountName);

        int foundId = checkIfExits("username", authorEmailMap, accountNameMap);
        if (foundId != 0) return foundId;

        try {
            String sql = "INSERT INTO username (`userId`,\n" +
                    "`accountName`)" +
                    "VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setInt(1, userId);
            statement.setString(2, authorEmail);
            if (statement.executeUpdate() > 0) {
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (null != generatedKeys && generatedKeys.next()) {
                    userNameId = generatedKeys.getInt(1);
                    generatedKeys.close();
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return userNameId;
    }

    private int getOrCreateUser(String authorName) {
        int userId = 0;

        Map<String, TableColumnName> values = newHashMap();
        values.put(authorName, TableColumnName.name);

        int user = checkIfExits("user", values);
        if (user != 0) return user;

        try {
            String sql = "INSERT INTO user (`name`)" +
                    "VALUES (?)";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setString(1, authorName);
            if (statement.executeUpdate() > 0) {
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (null != generatedKeys && generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                    generatedKeys.close();
                }
            }
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return userId;
    }

    public void saveHistory(int issueId, String from, String to, String field, int userId, String date) {
        try {
            String sql = "INSERT INTO history (`issueId`,\n" +
                    "`from`,\n" +
                    "`to`,\n" +
                    "`field`,\n" +
                    "`userId`,\n" +
                    "`createdDate`)" +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, issueId);
            preparedStatement.setString(2, from);
            preparedStatement.setString(3, to);
            preparedStatement.setString(4, field);
            preparedStatement.setInt(5, userId);
            preparedStatement.setString(6, date);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public int getOrCreateIssueRepository(String repositoryUrl, String repositoryType, int projectId) {
        int repositoryId = 0;

        Map<String, TableColumnName> mappedValues = newHashMap();
        mappedValues.put(repositoryUrl, TableColumnName.issueRepositoryUrl);

        int existingId = checkIfExits("issuerepository", mappedValues);
        if (existingId != 0) return existingId;

        try {
            String sql = "INSERT INTO issuerepository (`projectId`,\n" +
                    "`issueRepositoryUrl`,\n" +
                    "`issueRepositoryType`)" +
                    "VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setInt(1, projectId);
            statement.setString(2, repositoryUrl);
            statement.setString(3, repositoryType);
            if (statement.executeUpdate() > 0) {
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (null != generatedKeys && generatedKeys.next()) {
                    repositoryId = generatedKeys.getInt(1);
                    generatedKeys.close();
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return repositoryId;
    }

    public int createOrGetPriorityId(String priorityName, String description, int issueRepositoryId) {
        int output = 0;

        Map<String, TableColumnName> priorityNameMap = newHashMap();
        priorityNameMap.put(priorityName, TableColumnName.priorityName);

        Map<Integer, TableColumnName> issueRepositoryIdMap = newHashMap();
        issueRepositoryIdMap.put(issueRepositoryId, TableColumnName.issueRepositoryId);

        int returnId = checkIfExits("priorities", priorityNameMap, issueRepositoryIdMap);
        if (returnId != 0) return returnId;

        try {
            String sql = "INSERT INTO priorities (`issueRepositoryId`,\n" +
                    "`priorityName`,\n" +
                    "`description`)" +
                    "VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setInt(1, issueRepositoryId);
            statement.setString(2, priorityName);
            statement.setString(3, description);
            if (statement.executeUpdate() > 0) {
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (null != generatedKeys && generatedKeys.next()) {
                    output = generatedKeys.getInt(1);
                    generatedKeys.close();
                }
            }
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    public int getOrCreateCustomField(int issueRepositoryId, String description) {
        int newCustomFieldId = 0;

        Map<String, TableColumnName> stringMap = newHashMap();
        stringMap.put(description, TableColumnName.description);
        Map<Integer, TableColumnName> intMap = newHashMap();
        intMap.put(issueRepositoryId, TableColumnName.issueRepositoryId);

        int customFieldId = checkIfExits("customfield", stringMap, intMap);
        if (customFieldId != 0) return customFieldId;

        try {
            String sql = "INSERT INTO customfield (`issueRepositoryId`,\n" +
                    "`description`)" +
                    "VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setInt(1, issueRepositoryId);
            statement.setString(2, description);
            if (statement.executeUpdate() > 0) {
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (null != generatedKeys && generatedKeys.next()) {
                    newCustomFieldId = generatedKeys.getInt(1);
                    generatedKeys.close();
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return newCustomFieldId;
    }

    public void saveCustomFieldValue(int issueId, int customFieldId, String value) {
        try {
            String sql = "INSERT INTO customfieldvalue (`issueId`,\n" +
                    "`customFieldId`,\n" +
                    "`value`)" +
                    "VALUES (?, ?, ?)";

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, issueId);
            preparedStatement.setInt(2, customFieldId);
            preparedStatement.setString(3, value);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void saveComment(int issueRepositoryId, int databaseIssueId, String authorName, String authorEmail, String content) {
        int userId = getOrCreateRepositoryUser(authorName, authorEmail, issueRepositoryId);
        try {
            String sql = "INSERT INTO comments (`issueId`,\n" +
                    "`userId`,\n" +
                    "`context`)" +
                    "VALUES (?, ?, ?)";

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, databaseIssueId);
            preparedStatement.setInt(2, userId);
            preparedStatement.setString(3, content);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void saveIssueLink(int issueRepositoryId, int databaseIssueId, String issueType, String linkedIssueId) {
        try {
            String sql = "INSERT INTO issuelinks (`issueRepositoryId`,\n" +
                    "`issueId`,\n" +
                    "`relatedIssueId`,\n" +
                    "`linkType`)" +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, issueRepositoryId);
            preparedStatement.setInt(2, databaseIssueId);
            preparedStatement.setString(3, linkedIssueId);
            preparedStatement.setString(4, issueType);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public int getOrCreateSocialMediaUser(int projectId, String name, String email) {
        int userId = 0;

        Map<Integer, TableColumnName> projectIdMap = newHashMap();
        projectIdMap.put(projectId, TableColumnName.projectId);

        Map<String, TableColumnName> stringMap = newHashMap();
        stringMap.put(name, TableColumnName.userName);
        stringMap.put(email, TableColumnName.userEmail);

        int user = checkIfExits("socialmediauser", stringMap, projectIdMap);
        if (user != 0) return user;

        try {
            String sql = "INSERT INTO socialmediauser (`projectId`,\n" +
                    "`userName`,\n" +
                    "`userEmail`)" +
                    "VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setInt(1, projectId);
            statement.setString(2, name);
            statement.setString(3, email);

            if (statement.executeUpdate() > 0) {
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (null != generatedKeys && generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                    generatedKeys.close();
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return userId;
    }

    public void saveSocialMediaEntry(int projectId, int senderUserId, String originalEntryId, String context, SocialMediaChannel channelType, String inResponseTo,
                                     String receiver, String subject, String sentDate, Object receivedDate, Object seenDate, Object attachments, Object location) {
        try {
            String sql = "INSERT INTO socialmediaentries (`projectId`,\n" +
                    "`senderUserId`,\n" +
                    "`originalEntryId`,\n" +
                    "`context`,\n" +
                    "`channelType`,\n" +
                    "`inResponseTo`,\n" +
                    "`receiver`,\n" +
                    "`subject`,\n" +
                    "`sentDate`,\n" +
                    "`receivedDate`,\n" +
                    "`seenDate`,\n" +
                    "`attachments`,\n" +
                    "`location`)" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, projectId);
            preparedStatement.setInt(2, senderUserId);
            preparedStatement.setString(3, originalEntryId);
            preparedStatement.setString(4, context);
            preparedStatement.setString(5, channelType.name());
            preparedStatement.setString(6, inResponseTo);
            preparedStatement.setString(7, receiver);
            preparedStatement.setString(8, subject);
            preparedStatement.setString(9, sentDate);
            preparedStatement.setString(10, null);
            preparedStatement.setString(11, null);
            preparedStatement.setString(12, null);
            preparedStatement.setString(13, null);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public int getLastInsertedId() {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select MAX(id) as last_id FROM issues")) {
            rs.next();
            int lastId = rs.getInt("last_id");

            return lastId;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
