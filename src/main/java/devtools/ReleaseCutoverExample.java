package devtools;

public final class ReleaseCutoverExample {
    private ReleaseCutoverExample() {
    }

    public static void main(String[] args) {
        SessionRotationDecision.ReleaseEvent release =
                new SessionRotationDecision.ReleaseEvent("2026.10", true);
        String currentSession = "session-course-admin";
        SessionRotationDecision.Result decision =
                SessionRotationDecision.decide(release, currentSession);

        System.out.println("release=" + release.releaseId());
        System.out.println("decision=" + decision);
        System.out.println("old-session=revoked");
        System.out.println("new-session=active");

        String apiKey = System.getenv("INFRAI_API_KEY");
        System.out.println(apiKey == null || apiKey.isBlank()
                ? "api-client=ready; set INFRAI_API_KEY for live calls"
                : "api-client=ready; bearer key loaded from environment");
    }
}
