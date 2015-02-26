package Adaptors.HelperMethods;

public class UserRelationshipManager {
    private static final int FALSE = 0;

    private final int socialMediaRepositoryId;
    private final DatabaseHelperMethods helperMethods;

    public UserRelationshipManager(int socialMediaRepositoryId, DatabaseHelperMethods helperMethods) {
        this.socialMediaRepositoryId = socialMediaRepositoryId;
        this.helperMethods = helperMethods;
    }

    public void createRelationshipsForNicknameChange(String oldNickname, String newNickname) {
        int isUserExists = helperMethods.getSocialMediaUser(socialMediaRepositoryId, newNickname, newNickname);

        if (isUserExists == FALSE) {
            int oldNickUserId = helperMethods.getUser(oldNickname);
            int socialMediaUserId = helperMethods.getOrCreateSocialMediaUser(oldNickUserId, socialMediaRepositoryId, newNickname, newNickname);
        } else {
            int oldUserId = helperMethods.getUser(oldNickname);
            int newUserId = helperMethods.getUser(newNickname);
            if (oldUserId == newUserId) {
                //store the new something that i cannot read
            } else {
                if ("are the users related".equals(true)) {

                } else {
                    int oldNickUserId = helperMethods.getUser(oldNickname);
                    helperMethods.getOrCreateSocialMediaUser(oldNickUserId, socialMediaRepositoryId, newNickname, newNickname);
                    helperMethods.createUserRelationship(oldNickname, newNickname, "reason");

                }
            }
        }
    }
}
