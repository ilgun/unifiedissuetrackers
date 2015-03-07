package Adaptors.HelperMethods;

import Model.SocialMedia.SocialMediaChannel;
import Model.SocialMedia.SocialMediaEvent;

import java.sql.*;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.join;

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
                ++entryCount;
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
            throw new RuntimeException(e);
        }
        return projectId;
    }

    public int getOrCreateIssueRepositoryUser(String authorName, String authorEmail, int issueRepositoryId) {
        int output = 0;

        Map<String, TableColumnName> authorNameMap = newHashMap();
        authorNameMap.put(authorName, TableColumnName.username);

        Map<Integer, TableColumnName> issueRepositoryIdMap = newHashMap();
        issueRepositoryIdMap.put(issueRepositoryId, TableColumnName.issueRepositoryId);

        int existingId = checkIfExits("issuerepositoryuser", authorNameMap, issueRepositoryIdMap);
        if (existingId != 0) return existingId;

        try {
            int userId = getOrCreateUser(authorName);
            String sql = "INSERT INTO issuerepositoryuser (`userId`,\n" +
                    "`issueRepositoryId`,\n" +
                    "`userName`,\n" +
                    "`userEmail`)" +
                    "VALUES (?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setInt(1, userId);
            statement.setInt(2, issueRepositoryId);
            statement.setString(3, authorName);
            statement.setString(4, authorEmail);
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

    public void createUserRelationship(String newUsername, String oldUsername, String reason) {
        int relatedUserId = getUser(newUsername);
        int baseUserId = getUser(oldUsername);

        createRelationshipFor(baseUserId, relatedUserId, reason);
    }

    private void createRelationshipFor(int userId, int relatedUserIds, String reason) {
        try {
            String sql = "UPDATE user SET relatedUserIds = ? , reason = ? where id = ?";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setInt(1, userId);
            statement.setString(2, reason);
            statement.setInt(3, relatedUserIds);

            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Integer> getAllRelatedUserIdsFor(int userId) {
        Set<Integer> allRelatedUserIds = newHashSet();
        int relatedUserId = getRelatedUserIdsFor(userId);
        while (relatedUserId != 0) {
            allRelatedUserIds.add(relatedUserId);
            relatedUserId = getRelatedUserIdsFor(relatedUserId);
        }
        return allRelatedUserIds;
    }

    private Integer getRelatedUserIdsFor(int userId) {
        Integer relatedUserIds = 0;
        try {
            String relatedUserSql = "SELECT id from user where relatedUserIds = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(relatedUserSql);
            preparedStatement.setInt(1, userId);

            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                relatedUserIds = rs.getInt("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return relatedUserIds;
    }

    private int getOrCreateUser(String authorName) {
        int userId = 0;

        int user = getUser(authorName);
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
            throw new RuntimeException("Username was: " + authorName, e);
        }
        return userId;
    }

    public int getUser(String authorName) {
        Map<String, TableColumnName> values = newHashMap();
        values.put(authorName, TableColumnName.name);

        return checkIfExits("user", values);
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
        int userId = getOrCreateIssueRepositoryUser(authorName, authorEmail, issueRepositoryId);
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
            throw new RuntimeException(e);
        }
    }

    public int getOrCreateSocialMediaUser(int userId, int socialMediaRepositoryId, String authorName, String authorEmail) {
        String trimmedUserName = authorName.trim();
        int socialMediaUserId = 0;

        int user = getSocialMediaUser(socialMediaRepositoryId, authorEmail, trimmedUserName);
        if (user != 0) return user;

        try {
            int userIdToBeInserted;
            if (userId != 0) {
                userIdToBeInserted = userId;
            } else {
                userIdToBeInserted = getOrCreateUser(trimmedUserName);
            }
            String sql = "INSERT INTO socialmediauser (`userId`,\n" +
                    "`socialMediaRepositoryId`,\n" +
                    "`userName`,\n" +
                    "`userEmail`)" +
                    "VALUES (?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setInt(1, userIdToBeInserted);
            statement.setInt(2, socialMediaRepositoryId);
            statement.setString(3, trimmedUserName);
            statement.setString(4, authorEmail);

            if (statement.executeUpdate() > 0) {
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (null != generatedKeys && generatedKeys.next()) {
                    socialMediaUserId = generatedKeys.getInt(1);
                    generatedKeys.close();
                }
            }
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return socialMediaUserId;
    }

    public int getOrCreateSocialMediaUser(int socialMediaRepositoryId, String authorName, String authorEmail) {
        return getOrCreateSocialMediaUser(0, socialMediaRepositoryId, authorName, authorEmail);
    }

    public int getSocialMediaUser(int socialMediaRepositoryId, String authorEmail, String username) {
        String trimmedUserName = username.trim();
        Map<Integer, TableColumnName> socialMediaRepositoryIdMap = newHashMap();
        socialMediaRepositoryIdMap.put(socialMediaRepositoryId, TableColumnName.socialMediaRepositoryId);

        Map<String, TableColumnName> userMap = newHashMap();
        userMap.put(trimmedUserName, TableColumnName.userName);
        userMap.put(authorEmail, TableColumnName.userEmail);

        return checkIfExits("socialmediauser", userMap, socialMediaRepositoryIdMap);
    }

    public void saveSocialMediaEntry(int socialMediaRepositoryId, int senderUserId, String originalEntryId, String context, String inResponseTo,
                                     String receiver, String subject, String sentDate, Object receivedDate, Object seenDate, Object attachments, String location) {
/*
        Map<Integer, TableColumnName> socialMediaRepositoryIdMap = newHashMap();
        socialMediaRepositoryIdMap.put(socialMediaRepositoryId, TableColumnName.socialMediaRepositoryId);

        Map<String, TableColumnName> valuesMap = newHashMap();
        valuesMap.put(originalEntryId, TableColumnName.originalEntryId);
        valuesMap.put(context, TableColumnName.context);

        int entryId = checkIfExits("socialmediaentries", valuesMap, socialMediaRepositoryIdMap);
        if (entryId != 0) return;*/
        try {
            String sql = "INSERT INTO socialmediaentries (`socialMediaRepositoryId`,\n" +
                    "`senderUserId`,\n" +
                    "`originalEntryId`,\n" +
                    "`context`,\n" +
                    "`inResponseTo`,\n" +
                    "`receiver`,\n" +
                    "`subject`,\n" +
                    "`sentDate`,\n" +
                    "`receivedDate`,\n" +
                    "`seenDate`,\n" +
                    "`attachments`,\n" +
                    "`location`)" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, socialMediaRepositoryId);
            preparedStatement.setInt(2, senderUserId);
            preparedStatement.setString(3, originalEntryId);
            preparedStatement.setString(4, context);
            preparedStatement.setString(5, inResponseTo);
            preparedStatement.setString(6, receiver);
            preparedStatement.setString(7, subject);
            preparedStatement.setString(8, sentDate);
            preparedStatement.setString(9, null);
            preparedStatement.setString(10, null);
            preparedStatement.setString(11, null);
            preparedStatement.setString(12, location);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getOrCreateSocialMediaRepository(int projectId, String repositoryUrl, SocialMediaChannel channelType) {
        int repositoryId = 0;

        Map<String, TableColumnName> mappedValues = newHashMap();
        mappedValues.put(repositoryUrl, TableColumnName.repositoryUrl);

        int existingId = checkIfExits("socialmediarepository", mappedValues);
        if (existingId != 0) return existingId;

        try {
            String sql = "INSERT INTO socialmediarepository (`projectId`,\n" +
                    "`repositoryUrl`,\n" +
                    "`repositoryType`)" +
                    "VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setInt(1, projectId);
            statement.setString(2, repositoryUrl);
            statement.setString(3, channelType.name());
            if (statement.executeUpdate() > 0) {
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (null != generatedKeys && generatedKeys.next()) {
                    repositoryId = generatedKeys.getInt(1);
                    generatedKeys.close();
                }
            }
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return repositoryId;
    }

    public void createSocialMediaEvent(int socialMediaRepositoryId, int socialMediaUserId, String eventDate, SocialMediaEvent eventType, String content) {
        try {
            String sql = "INSERT INTO socialmediaevents (`socialMediaRepositoryId`,\n" +
                    "`socialMediaUserId`,\n" +
                    "`eventDate`,\n" +
                    "`eventType`,\n" +
                    "`content`)" +
                    "VALUES (?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, socialMediaRepositoryId);
            preparedStatement.setInt(2, socialMediaUserId);
            preparedStatement.setString(3, eventDate);
            preparedStatement.setString(4, eventType.name());
            preparedStatement.setString(5, content);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commitTransaction() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public int getLastInsertedIdFor(TableName tableName) {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("select MAX(id) as last_id FROM " + tableName)) {
            rs.next();
            return rs.getInt("last_id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String getFinalRelatedIds(int userId, Set<String> relatedUserIds) {
        relatedUserIds.add(String.valueOf(userId));
        return join(",", relatedUserIds);
    }
}
