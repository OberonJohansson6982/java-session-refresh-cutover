package devtools;

public final class SessionRotationDecisionTest {
    public static void main(String[] args) {
        SessionRotationDecision.ReleaseEvent published =
                new SessionRotationDecision.ReleaseEvent("lesson-7", true);
        SessionRotationDecision.Result result = SessionRotationDecision.decide(published, "session-1");
        if (result != SessionRotationDecision.Result.ROTATE_AND_REVOKE) {
            throw new AssertionError("published release must rotate and revoke");
        }
        System.out.println("decision-test=passed");
    }
}
