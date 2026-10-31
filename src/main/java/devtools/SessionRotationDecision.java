package devtools;

public final class SessionRotationDecision {
    public enum Result { ROTATE_AND_REVOKE, HOLD_FOR_REVIEW }

    private SessionRotationDecision() {
    }

    public static Result decide(ReleaseEvent event, String currentSessionId) {
        if (event.published() && currentSessionId != null && !currentSessionId.isBlank()) {
            return Result.ROTATE_AND_REVOKE;
        }
        return Result.HOLD_FOR_REVIEW;
    }

    public record ReleaseEvent(String releaseId, boolean published) {
    }
}
