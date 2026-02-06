package org.das2.util.filesystem;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Detect whether a URL points to a GitHub/GitLab repo resource, and extract
 * (host, namespace/owner, project/repo, ref, path) plus a canonical "raw" URL when possible.
 *
 * Designed for Autoplot-style "ForgeFileSystem" dispatch (GitHubFileSystem/GitLabFileSystem).
 */
public final class ForgeUrlDetector {

    public enum Forge { GITHUB, GITLAB }
    public enum Kind  { FILE, DIR, ARCHIVE, RELEASE_ASSET, API, UNKNOWN }

    public static final class ForgeRef {
        public final Forge forge;
        public final String host;        // e.g. "github.com", "gitlab.com", "gitlab.example.edu"
        public final String namespace;   // GitHub: owner, GitLab: group/subgroup
        public final String project;     // repo/project name
        public final String ref;         // branch/tag/sha (best-effort)
        public final String path;        // path within repo (no leading slash), may be "" for repo root
        public final Kind kind;
        public final String canonicalRawUrl; // may be null if not determinable

        public ForgeRef(Forge forge, String host, String namespace, String project,
                        String ref, String path, Kind kind, String canonicalRawUrl) {
            this.forge = forge;
            this.host = host;
            this.namespace = namespace;
            this.project = project;
            this.ref = ref;
            this.path = path;
            this.kind = kind;
            this.canonicalRawUrl = canonicalRawUrl;
        }

        @Override public String toString() {
            return "ForgeRef{" +
                    "forge=" + forge +
                    ", host='" + host + '\'' +
                    ", namespace='" + namespace + '\'' +
                    ", project='" + project + '\'' +
                    ", ref='" + ref + '\'' +
                    ", path='" + path + '\'' +
                    ", kind=" + kind +
                    ", canonicalRawUrl='" + canonicalRawUrl + '\'' +
                    '}';
        }
    }

    /** Main entry point. */
    public static Optional<ForgeRef> detectForgeUrl(String url) {
        if (url == null) return Optional.empty();
        url = url.trim();
        if (url.isEmpty()) return Optional.empty();

        final URI uri;
        try {
            // Drop fragments early (#L10 etc) by parsing via URI and ignoring fragment.
            uri = new URI(url);
        } catch (Exception e) {
            return Optional.empty();
        }

        String host = uri.getHost();
        if (host == null) return Optional.empty();
        host = host.toLowerCase(Locale.ROOT);

        // Normalize path (no query, no fragment)
        String rawPath = uri.getRawPath();  // keep encoded for safe segment splitting
        if (rawPath == null) rawPath = "";

        // Split path into segments (encoded segments); ignore empty segments.
        List<String> seg = splitPathSegments(rawPath);

        // Fast-path GitHub known hosts.
        if (host.equals("github.com") || host.equals("www.github.com")) {
            return parseGitHubWeb(host, seg);
        }
        if (host.equals("raw.githubusercontent.com")) {
            return parseGitHubRaw(host, seg);
        }
        if (host.equals("api.github.com")) {
            return parseGitHubApi(host, seg, uri.getRawQuery());
        }

        // Generic GitLab-style detection for ANY host: look for "/-/(blob|raw|tree|archive|...)".
        // This allows self-hosted GitLab instances to work.
        Optional<ForgeRef> gl = parseGitLabGeneric(host, seg, uri.getRawQuery());
        if (gl.isPresent()) return gl;

        // Could add heuristics for GitHub Enterprise hostnames if you want:
        // e.g., if segments match GitHub patterns and host isn't known.
        // For now: attempt "github-like" patterns on unknown hosts only if it *looks* exactly like GitHub web.
        Optional<ForgeRef> ghe = parseGitHubLikeUnknownHost(host, seg);
        if (ghe.isPresent()) return ghe;

        return Optional.empty();
    }

    // ----------------------- GitHub -----------------------

    // https://github.com/<owner>/<repo>/blob/<ref>/<path>
    // https://github.com/<owner>/<repo>/tree/<ref>/<path?>
    // https://github.com/<owner>/<repo>/releases/download/<tag>/<filename>
    private static Optional<ForgeRef> parseGitHubWeb(String host, List<String> seg) {
        if (seg.size() < 2) return Optional.empty();
        String owner = decode(seg.get(0));
        String repo  = stripDotGit(decode(seg.get(1)));

        if (!isPlausibleName(owner) || !isPlausibleName(repo)) return Optional.empty();

        if (seg.size() == 2) {
            // Repo root
            return Optional.of(new ForgeRef(Forge.GITHUB, host, owner, repo, null, "", Kind.UNKNOWN, null));
        }

        String op = decode(seg.get(2));
        if ("blob".equals(op) || "tree".equals(op)) {
            if (seg.size() < 4) return Optional.empty();
            String ref = decode(seg.get(3));
            String path = joinDecoded(seg, 4);
            Kind kind = "blob".equals(op) ? Kind.FILE : Kind.DIR;

            String raw = null;
            // Canonical raw only really makes sense for a file
            if (kind == Kind.FILE && !path.isEmpty()) {
                raw = "https://raw.githubusercontent.com/" +
                        encodePath(owner) + "/" + encodePath(repo) + "/" +
                        encodePath(ref) + "/" + encodePathSegments(path);
            }
            return Optional.of(new ForgeRef(Forge.GITHUB, host, owner, repo, ref, path, kind, raw));
        }

        if ("releases".equals(op) && seg.size() >= 6 && "download".equals(decode(seg.get(3)))) {
            String tag = decode(seg.get(4));
            String filename = joinDecoded(seg, 5);
            // This is not a repo-path file; treat as RELEASE_ASSET.
            return Optional.of(new ForgeRef(Forge.GITHUB, host, owner, repo, tag, filename, Kind.RELEASE_ASSET, null));
        }

        return Optional.empty();
    }

    // https://raw.githubusercontent.com/<owner>/<repo>/<ref>/<path>
    private static Optional<ForgeRef> parseGitHubRaw(String host, List<String> seg) {
        if (seg.size() < 4) return Optional.empty();
        String owner = decode(seg.get(0));
        String repo  = stripDotGit(decode(seg.get(1)));
        String ref   = decode(seg.get(2));
        String path  = joinDecoded(seg, 3);

        if (!isPlausibleName(owner) || !isPlausibleName(repo)) return Optional.empty();

        String canonical = "https://raw.githubusercontent.com/" +
                encodePath(owner) + "/" + encodePath(repo) + "/" +
                encodePath(ref) + "/" + encodePathSegments(path);

        return Optional.of(new ForgeRef(Forge.GITHUB, host, owner, repo, ref, path, Kind.FILE, canonical));
    }

    // https://api.github.com/repos/<owner>/<repo>/contents/<path>?ref=<ref>
    private static Optional<ForgeRef> parseGitHubApi(String host, List<String> seg, String rawQuery) {
        // Expect: repos/{owner}/{repo}/contents/{path...}
        if (seg.size() < 5) return Optional.empty();
        if (!"repos".equals(decode(seg.get(0)))) return Optional.empty();

        String owner = decode(seg.get(1));
        String repo  = stripDotGit(decode(seg.get(2)));
        String op    = decode(seg.get(3));
        if (!"contents".equals(op)) return Optional.empty();

        String path = joinDecoded(seg, 4);
        String ref = getQueryParam(rawQuery, "ref"); // may be null

        // Canonical raw can be built if we have ref
        String raw = null;
        if (ref != null && !ref.isEmpty() && !path.isEmpty()) {
            raw = "https://raw.githubusercontent.com/" +
                    encodePath(owner) + "/" + encodePath(repo) + "/" +
                    encodePath(ref) + "/" + encodePathSegments(path);
        }
        return Optional.of(new ForgeRef(Forge.GITHUB, host, owner, repo, ref, path, Kind.API, raw));
    }

    /**
     * GitHub Enterprise (or other host) often uses the same path patterns as github.com.
     * This is intentionally conservative: only accept if it matches "/owner/repo/(blob|tree)/ref/..."
     */
    private static Optional<ForgeRef> parseGitHubLikeUnknownHost(String host, List<String> seg) {
        if (seg.size() < 4) return Optional.empty();
        String owner = decode(seg.get(0));
        String repo  = stripDotGit(decode(seg.get(1)));
        String op    = decode(seg.get(2));
        if (!("blob".equals(op) || "tree".equals(op))) return Optional.empty();

        String ref = decode(seg.get(3));
        String path = joinDecoded(seg, 4);
        Kind kind = "blob".equals(op) ? Kind.FILE : Kind.DIR;

        // For unknown hosts we cannot assume raw host pattern; canonicalRawUrl left null.
        return Optional.of(new ForgeRef(Forge.GITHUB, host, owner, repo, ref, path, kind, null));
    }

    // ----------------------- GitLab -----------------------

    // https://<host>/<namespace...>/<project>/-/blob/<ref>/<path>
    // https://<host>/<namespace...>/<project>/-/raw/<ref>/<path>
    // https://<host>/<namespace...>/<project>/-/tree/<ref>/<path?>
    // https://<host>/<namespace...>/<project>/-/archive/<ref>/<project>-<ref>.zip
    // https://<host>/api/v4/projects/<id>/repository/files/<path>/raw?ref=<ref> (handled partially)
    private static Optional<ForgeRef> parseGitLabGeneric(String host, List<String> seg, String rawQuery) {
        if (seg.isEmpty()) return Optional.empty();

        // GitLab API v4 form with project id (can’t extract namespace/project name without API call).
        // We still classify as GitLab if it matches.
        if (seg.size() >= 7 && "api".equals(decode(seg.get(0))) && "v4".equals(decode(seg.get(1)))
                && "projects".equals(decode(seg.get(2)))) {
            // /api/v4/projects/<id>/repository/files/<path...>/raw?ref=<ref>
            String projectId = decode(seg.get(3));
            if (seg.size() >= 7 && "repository".equals(decode(seg.get(4)))
                    && "files".equals(decode(seg.get(5)))) {
                // Find trailing "/raw"
                int rawIdx = lastIndexOf(seg, "raw");
                if (rawIdx > 5) {
                    String encPath = joinRaw(seg, 6, rawIdx); // still encoded segments, join with /
                    String filePath = decodePathPreservingSlashes(encPath);
                    String ref = getQueryParam(rawQuery, "ref");
                    return Optional.of(new ForgeRef(Forge.GITLAB, host,
                            null, projectId, ref, filePath, Kind.API, null));
                }
            }
        }

        // Look for the "/-/" sentinel (segments: ..., "-", "<op>", ...)
        int dashIdx = indexOf(seg, "-");
        if (dashIdx < 0) return Optional.empty();
        if (dashIdx + 1 >= seg.size()) return Optional.empty();

        String op = decode(seg.get(dashIdx + 1));
        if (!( "blob".equals(op) || "raw".equals(op) || "tree".equals(op) || "archive".equals(op) )) {
            return Optional.empty();
        }

        // Everything before "-" is namespace.../project
        if (dashIdx < 1) return Optional.empty(); // need at least project
        String project = stripDotGit(decode(seg.get(dashIdx - 1)));
        String namespace = (dashIdx - 1 == 0) ? "" : joinDecoded(seg, 0, dashIdx - 1);

        // After op: expect ref next (except some archive variants, but usually ref is next)
        if (dashIdx + 2 >= seg.size()) return Optional.empty();
        String ref = decode(seg.get(dashIdx + 2));

        Kind kind;
        String path = "";
        String canonical = null;

        switch (op) {
            case "blob":
                kind = Kind.FILE;
                path = joinDecoded(seg, dashIdx + 3);
                if (!path.isEmpty()) {
                    canonical = buildGitLabRaw(host, namespace, project, ref, path);
                }
                break;
            case "raw":
                kind = Kind.FILE;
                path = joinDecoded(seg, dashIdx + 3);
                if (!path.isEmpty()) {
                    canonical = buildGitLabRaw(host, namespace, project, ref, path);
                }
                break;
            case "tree":
                kind = Kind.DIR;
                path = joinDecoded(seg, dashIdx + 3); // may be ""
                // no canonicalRawUrl for directories
                break;
            case "archive":
                kind = Kind.ARCHIVE;
                path = joinDecoded(seg, dashIdx + 3); // often "<project>-<ref>.zip" but keep as-is
                // no canonicalRawUrl
                break;
            default:
                kind = Kind.UNKNOWN;
        }

        return Optional.of(new ForgeRef(Forge.GITLAB, host,
                namespace.isEmpty() ? null : namespace, project, ref, path, kind, canonical));
    }

    private static String buildGitLabRaw(String host, String namespace, String project, String ref, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://").append(host).append("/");
        if (namespace != null && !namespace.isEmpty()) {
            sb.append(encodePathSegments(namespace)).append("/");
        }
        sb.append(encodePath(project)).append("/-/raw/")
          .append(encodePath(ref)).append("/")
          .append(encodePathSegments(path));
        return sb.toString();
    }

    // ----------------------- Helpers -----------------------

    private static List<String> splitPathSegments(String rawPath) {
        // rawPath starts with '/', split on '/', ignore empties
        String[] parts = rawPath.split("/");
        ArrayList<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            out.add(p);
        }
        return out;
    }

    private static String decode(String rawSegment) {
        // URLDecoder turns '+' into space, but in paths '+' is literal plus; we only have percent-encoding here.
        // Use URLDecoder anyway; Git forges generally percent-encode spaces as %20.
        try {
            return URLDecoder.decode(rawSegment, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return rawSegment;
        }
    }

    private static String decodePathPreservingSlashes(String encodedJoinedPath) {
        // encodedJoinedPath contains literal '/' between encoded segments; decode percent-escapes but keep '/'.
        // URLDecoder will keep '/' unchanged.
        return decode(encodedJoinedPath);
    }

    private static String stripDotGit(String name) {
        if (name == null) return null;
        if (name.endsWith(".git") && name.length() > 4) return name.substring(0, name.length() - 4);
        return name;
    }

    private static boolean isPlausibleName(String s) {
        if (s == null || s.isEmpty()) return false;
        // fairly permissive: GitHub allows letters, digits, '-', '_', '.'; GitLab similar.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.') continue;
            return false;
        }
        return true;
    }

    private static String joinDecoded(List<String> seg, int start) {
        return joinDecoded(seg, start, seg.size());
    }

    private static String joinDecoded(List<String> seg, int start, int endExclusive) {
        if (start >= endExclusive) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < endExclusive; i++) {
            if (i > start) sb.append('/');
            sb.append(decode(seg.get(i)));
        }
        return sb.toString();
    }

    private static String joinRaw(List<String> seg, int start, int endExclusive) {
        if (start >= endExclusive) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < endExclusive; i++) {
            if (i > start) sb.append('/');
            sb.append(seg.get(i)); // raw encoded segment
        }
        return sb.toString();
    }

    private static int indexOf(List<String> seg, String literal) {
        for (int i = 0; i < seg.size(); i++) {
            if (literal.equals(decode(seg.get(i)))) return i;
        }
        return -1;
    }

    private static int lastIndexOf(List<String> seg, String literal) {
        for (int i = seg.size() - 1; i >= 0; i--) {
            if (literal.equals(decode(seg.get(i)))) return i;
        }
        return -1;
    }

    private static String getQueryParam(String rawQuery, String key) {
        if (rawQuery == null || rawQuery.isEmpty()) return null;
        // rawQuery is still encoded; split & then decode key/value
        String[] parts = rawQuery.split("&");
        for (String p : parts) {
            int eq = p.indexOf('=');
            String k = eq >= 0 ? p.substring(0, eq) : p;
            String v = eq >= 0 ? p.substring(eq + 1) : "";
            if (key.equals(decode(k))) {
                String dv = decode(v);
                return dv.isEmpty() ? null : dv;
            }
        }
        return null;
    }

    private static String encodePath(String s) {
        // Encode for path segment (not full path). URLEncoder is for query, so fix '+' => %20 and keep safe chars.
        // We'll be conservative: encode everything URLEncoder would, then convert '+' to %20.
        String enc = URLEncoder.encode(s);
        return enc.replace("+", "%20");
    }

    private static String encodePathSegments(String path) {
        if (path == null || path.isEmpty()) return "";
        String[] parts = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(encodePath(parts[i]));
        }
        return sb.toString();
    }

    // ----------------------- tiny demo -----------------------
    public static void main(String[] args) {
        String[] tests = {
                "https://github.com/autoplot/dev/blob/main/README.md",
                "https://raw.githubusercontent.com/autoplot/dev/main/README.md",
                "https://gitlab.com/group/subgroup/proj/-/blob/main/a/b.txt#L10",
                "https://gitlab.example.edu/g/proj/-/raw/main/data/file.cdf?plain=1",
                "https://api.github.com/repos/autoplot/dev/contents/README.md?ref=main"
        };
        for (String t : tests) {
            System.out.println(t);
            System.out.println("  -> " + detectForgeUrl(t).orElse(null));
        }
    }
}
