import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads posters for movies whose poster_url is empty and updates MySQL only
 * after a valid local image has been written successfully.
 */
public final class PosterBackfill {
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36";
    private static final String REFERER = "https://movie.douban.com/";
    private static final int DOWNLOAD_THREADS = 4;
    private static final Pattern POSTER_URL_PATTERN = Pattern.compile(
            "https://[^\\\"']+\\.doubanio\\.com/view/photo/[^\\\"']+/public/p[0-9]+\\.jpg(?:\\?[^\\\"']*)?");

    private PosterBackfill() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("Usage: PosterBackfill <jdbc-url> <db-user> <db-password> <movie-csv> <poster-dir>");
            System.exit(2);
        }

        String jdbcUrl = args[0];
        String dbUser = args[1];
        String dbPassword = args[2];
        Path csvPath = Path.of(args[3]).toAbsolutePath().normalize();
        Path posterDir = Path.of(args[4]).toAbsolutePath().normalize();
        Files.createDirectories(posterDir);

        Map<String, String> posterSources = loadPosterSources(csvPath);
        posterSources.putAll(loadPosterOverrides(csvPath.resolveSibling("poster-overrides.properties")));
        List<MovieRow> missingMovies;
        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            missingMovies = loadMissingMovies(connection);
        }

        System.out.printf("Missing poster rows: %d%n", missingMovies.size());
        System.out.printf("CSV poster sources: %d%n", posterSources.size());

        List<DownloadResult> results = downloadPosters(missingMovies, posterSources, posterDir);

        int updated;
        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            connection.setAutoCommit(false);
            updated = updateDatabase(connection, results);
            connection.commit();
        }

        List<DownloadResult> failures = new ArrayList<>();
        int reused = 0;
        int downloaded = 0;
        for (DownloadResult result : results) {
            if (result.success) {
                if (result.reused) {
                    reused++;
                } else {
                    downloaded++;
                }
            } else {
                failures.add(result);
            }
        }

        Path failureReport = posterDir.getParent().resolve("poster-backfill-failures.tsv");
        writeFailureReport(failureReport, failures);

        System.out.printf("Downloaded: %d%n", downloaded);
        System.out.printf("Reused local files: %d%n", reused);
        System.out.printf("Database rows updated: %d%n", updated);
        System.out.printf("Failures: %d%n", failures.size());
        if (!failures.isEmpty()) {
            System.out.println("Failure report: " + failureReport);
            System.exit(1);
        }
    }

    private static Map<String, String> loadPosterSources(Path csvPath) throws IOException {
        Map<String, String> result = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> fields = parseCsvLine(line);
                if (fields.size() < 18) {
                    continue;
                }
                String doubanId = fields.get(15).trim();
                String posterUrl = fields.get(17).trim();
                if (!doubanId.isEmpty() && posterUrl.startsWith("http")) {
                    result.putIfAbsent(doubanId, posterUrl);
                }
            }
        }
        return result;
    }

    private static Map<String, String> loadPosterOverrides(Path overridePath) throws IOException {
        Map<String, String> result = new HashMap<>();
        if (!Files.isRegularFile(overridePath)) {
            return result;
        }
        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(overridePath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        for (String doubanId : properties.stringPropertyNames()) {
            String url = properties.getProperty(doubanId, "").trim();
            if (!doubanId.trim().isEmpty() && url.startsWith("http")) {
                result.put(doubanId.trim(), url);
            }
        }
        return result;
    }

    private static List<MovieRow> loadMissingMovies(Connection connection) throws Exception {
        List<MovieRow> rows = new ArrayList<>();
        String sql = "SELECT id, douban_id, title FROM movie "
                + "WHERE poster_url IS NULL OR TRIM(poster_url) = '' ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rows.add(new MovieRow(
                        resultSet.getLong("id"),
                        resultSet.getString("douban_id"),
                        resultSet.getString("title")));
            }
        }
        return rows;
    }

    private static List<DownloadResult> downloadPosters(List<MovieRow> movies,
                                                         Map<String, String> sources,
                                                         Path posterDir) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(DOWNLOAD_THREADS);
        try {
            List<Future<DownloadResult>> futures = new ArrayList<>();
            for (MovieRow movie : movies) {
                Callable<DownloadResult> task = () -> downloadPoster(movie, sources.get(movie.doubanId), posterDir);
                futures.add(executor.submit(task));
            }

            List<DownloadResult> results = new ArrayList<>();
            for (Future<DownloadResult> future : futures) {
                results.add(future.get());
                if (results.size() % 25 == 0 || results.size() == futures.size()) {
                    System.out.printf("Processed: %d/%d%n", results.size(), futures.size());
                }
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private static DownloadResult downloadPoster(MovieRow movie, String remoteUrl, Path posterDir) {
        if (movie.doubanId == null || movie.doubanId.trim().isEmpty()) {
            return DownloadResult.failure(movie, "missing douban_id");
        }

        try {
            Path titleFile = posterDir.resolve(movie.title + ".jpg");
            if (isValidImage(titleFile)) {
                return DownloadResult.success(movie, "/api/posters/" + titleFile.getFileName(), true);
            }
        } catch (RuntimeException ignored) {
            // Some titles contain characters that cannot be used in Windows paths.
        }

        Path outputFile = posterDir.resolve(movie.doubanId + ".jpg");
        if (isValidImage(outputFile)) {
            return DownloadResult.success(movie, "/api/posters/" + outputFile.getFileName(), true);
        }
        if (remoteUrl == null || remoteUrl.isEmpty()) {
            return DownloadResult.failure(movie, "missing CSV poster URL");
        }

        Exception lastError = null;
        List<String> candidates = buildPosterCandidates(remoteUrl, movie.doubanId);
        for (String candidate : candidates) {
            for (int attempt = 1; attempt <= 2; attempt++) {
                Path tempFile = outputFile.resolveSibling(outputFile.getFileName() + ".part");
                try {
                    Files.deleteIfExists(tempFile);
                    HttpURLConnection connection = openConnection(candidate, "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
                    int status = connection.getResponseCode();
                    if (status != HttpURLConnection.HTTP_OK) {
                        connection.disconnect();
                        throw new IOException("HTTP " + status + " for " + candidate);
                    }
                    String contentType = connection.getContentType();
                    if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                        connection.disconnect();
                        throw new IOException("unexpected content type: " + contentType);
                    }

                    try (InputStream input = connection.getInputStream()) {
                        Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    } finally {
                        connection.disconnect();
                    }

                    if (!isValidImage(tempFile)) {
                        throw new IOException("downloaded file is not a valid image");
                    }
                    moveIntoPlace(tempFile, outputFile);
                    return DownloadResult.success(movie, "/api/posters/" + outputFile.getFileName(), false);
                } catch (Exception e) {
                    lastError = e;
                    try {
                        Files.deleteIfExists(tempFile);
                        String message = e.getMessage();
                        if (message == null || !message.startsWith("HTTP 404")) {
                            Thread.sleep(attempt * 1000L);
                        }
                    } catch (Exception ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return DownloadResult.failure(movie, lastError == null ? "download failed" : lastError.getMessage());
    }

    private static List<String> buildPosterCandidates(String originalUrl, String doubanId) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(originalUrl);

        Matcher matcher = Pattern.compile("/public/(p[0-9]+\\.jpg)").matcher(originalUrl);
        if (matcher.find()) {
            String fileName = matcher.group(1);
            String[] hosts = {"img1", "img2", "img3", "img9"};
            String[] sizes = {"s_ratio_poster", "l", "m", "raw"};
            for (String host : hosts) {
                for (String size : sizes) {
                    candidates.add("https://" + host + ".doubanio.com/view/photo/"
                            + size + "/public/" + fileName);
                }
            }
        }

        String currentPosterUrl = fetchCurrentPosterUrl(doubanId);
        if (currentPosterUrl != null) {
            candidates.add(currentPosterUrl);
        }
        return new ArrayList<>(candidates);
    }

    private static String fetchCurrentPosterUrl(String doubanId) {
        String[] endpoints = {
                "https://movie.douban.com/j/subject_abstract?subject_id=" + doubanId,
                "https://m.douban.com/rexxar/api/v2/movie/" + doubanId,
                "https://m.douban.com/movie/subject/" + doubanId + "/",
                "https://movie.douban.com/subject/" + doubanId + "/"
        };
        for (String endpoint : endpoints) {
            HttpURLConnection connection = null;
            try {
                connection = openConnection(endpoint, "application/json,text/html,application/xhtml+xml");
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    continue;
                }
                String body;
                try (InputStream input = connection.getInputStream()) {
                    body = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                            .replace("\\/", "/")
                            .replace("\\u0026", "&");
                }
                Matcher matcher = POSTER_URL_PATTERN.matcher(body);
                if (matcher.find()) {
                    return matcher.group();
                }
            } catch (Exception ignored) {
                // Try the next public subject endpoint.
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        return null;
    }

    private static HttpURLConnection openConnection(String url, String accept) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Referer", REFERER);
        connection.setRequestProperty("Accept", accept);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setInstanceFollowRedirects(true);
        return connection;
    }

    private static boolean isValidImage(Path path) {
        try {
            if (!Files.isRegularFile(path) || Files.size(path) < 1024) {
                return false;
            }
            BufferedImage image = ImageIO.read(path.toFile());
            return image != null && image.getWidth() > 0 && image.getHeight() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static int updateDatabase(Connection connection, List<DownloadResult> results) throws Exception {
        String sql = "UPDATE movie SET poster_url = ? "
                + "WHERE id = ? AND (poster_url IS NULL OR TRIM(poster_url) = '')";
        int updated = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (DownloadResult result : results) {
                if (!result.success) {
                    continue;
                }
                statement.setString(1, result.localUrl);
                statement.setLong(2, result.movie.id);
                statement.addBatch();
            }
            for (int count : statement.executeBatch()) {
                if (count > 0) {
                    updated += count;
                }
            }
        }
        return updated;
    }

    private static void writeFailureReport(Path report, List<DownloadResult> failures) throws IOException {
        if (failures.isEmpty()) {
            Files.deleteIfExists(report);
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(report, StandardCharsets.UTF_8)) {
            writer.write("movie_id\tdouban_id\ttitle\treason\n");
            for (DownloadResult failure : failures) {
                writer.write(failure.movie.id + "\t"
                        + clean(failure.movie.doubanId) + "\t"
                        + clean(failure.movie.title) + "\t"
                        + clean(failure.error) + "\n");
            }
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    private static final class MovieRow {
        private final long id;
        private final String doubanId;
        private final String title;

        private MovieRow(long id, String doubanId, String title) {
            this.id = id;
            this.doubanId = doubanId;
            this.title = title;
        }
    }

    private static final class DownloadResult {
        private final MovieRow movie;
        private final boolean success;
        private final boolean reused;
        private final String localUrl;
        private final String error;

        private DownloadResult(MovieRow movie, boolean success, boolean reused, String localUrl, String error) {
            this.movie = movie;
            this.success = success;
            this.reused = reused;
            this.localUrl = localUrl;
            this.error = error;
        }

        private static DownloadResult success(MovieRow movie, String localUrl, boolean reused) {
            return new DownloadResult(movie, true, reused, localUrl, null);
        }

        private static DownloadResult failure(MovieRow movie, String error) {
            return new DownloadResult(movie, false, false, null, error);
        }
    }
}
