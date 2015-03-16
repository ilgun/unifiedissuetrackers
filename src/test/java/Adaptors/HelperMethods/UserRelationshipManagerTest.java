package Adaptors.HelperMethods;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static DatabaseConnectors.IssueTrackerConnector.getDatabaseConnection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class UserRelationshipManagerTest {

    UserRelationshipManager manager;
    DatabaseHelperMethods helpers;

    @Before
    public void setUp() throws Exception {
        helpers = new DatabaseHelperMethods(getDatabaseConnection());
        manager = new UserRelationshipManager(4, helpers);
    }

    @Test
    public void testCreateRelationshipsForNicknameChange() throws Exception {
        manager.createRelationshipsForNicknameChange("hardy__", "hardy");
        int firstUserId = helpers.getUser("hardy__");
        int secondUserId = helpers.getUser("hardy");
        Set<Integer> relatedIds = helpers.getAllRelatedUserIdsFor(firstUserId);
        relatedIds.addAll(helpers.getAllRelatedUserIdsFor(secondUserId));

        assertThat(relatedIds, hasItem(firstUserId));
    }
}