package Adaptors.HelperMethods;

import java.util.Set;

public class UserRelationshipManager {
    private static final int FALSE = 0;

    private final int socialMediaRepositoryId;
    private final DatabaseHelperMethods helperMethods;

    public UserRelationshipManager(int socialMediaRepositoryId, DatabaseHelperMethods helperMethods) {
        this.socialMediaRepositoryId = socialMediaRepositoryId;
        this.helperMethods = helperMethods;
    }

    public void createRelationshipsForNicknameChange(String oldNickname, String newNickname) {
        int isSocialMediaUserExists = helperMethods.getSocialMediaUser(socialMediaRepositoryId, newNickname, newNickname);

        if (isSocialMediaUserExists == FALSE) {
            int oldNickUserId = helperMethods.getUser(oldNickname);
            helperMethods.getOrCreateSocialMediaUser(oldNickUserId, socialMediaRepositoryId, newNickname, newNickname);
            int isNewNickHasUser = helperMethods.getUser(newNickname);
            if (isNewNickHasUser != FALSE) {
                helperMethods.createUserRelationship(isNewNickHasUser, oldNickUserId, "Old nick has similar user with the new nick.");
            }
        } else {
            int oldUserId = helperMethods.getUser(oldNickname);
            int newUserId = helperMethods.getUser(newNickname);
            if (oldUserId == newUserId) {
                // Do nothing because users are exact same!
            } else {
                Set<Integer> relatedUserIds = helperMethods.getAllRelatedUserIdsFor(newUserId);
                if (relatedUserIds.contains(oldUserId)) {
                    //Do nothing because users are already related!
                } else {
                    helperMethods.getOrCreateSocialMediaUser(oldUserId, socialMediaRepositoryId, newNickname, newNickname);
                    if (newUserId < oldUserId) {
                        helperMethods.createUserRelationship(oldUserId, newUserId, "New nick is already present in the system, possible duplicate.");
                    } else {
                        helperMethods.createUserRelationship(newUserId, oldUserId, "New nick is already present in the system, possible duplicate.");
                    }
                }
            }
        }
    }
}
