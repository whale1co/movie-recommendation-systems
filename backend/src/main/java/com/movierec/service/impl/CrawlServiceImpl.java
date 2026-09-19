package com.movierec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movierec.entity.CrawlLog;
import com.movierec.entity.Movie;
import com.movierec.entity.Rating;
import com.movierec.entity.User;
import com.movierec.mapper.CrawlLogMapper;
import com.movierec.mapper.MovieMapper;
import com.movierec.mapper.RatingMapper;
import com.movierec.mapper.UserMapper;
import com.movierec.service.CrawlService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CrawlServiceImpl implements CrawlService {

    private static final Logger log = LoggerFactory.getLogger(CrawlServiceImpl.class);

    private static final String AJAX_API = "https://movie.douban.com/j/new_searchsubjects";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36";
    private static final String REFERER = "https://movie.douban.com";
    private static final int PAGE_SIZE = 20;

    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/119.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/118.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/121.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Safari/17.2",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/117.0.0.0 Safari/537.36"
    };

    private final MovieMapper movieMapper;
    private final CrawlLogMapper crawlLogMapper;
    private final RatingMapper ratingMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    @Value("${movie.poster-dir:./posters}")
    private String posterDir;

    public CrawlServiceImpl(MovieMapper movieMapper, CrawlLogMapper crawlLogMapper,
                            RatingMapper ratingMapper, UserMapper userMapper,
                            PasswordEncoder passwordEncoder) {
        this.movieMapper = movieMapper;
        this.crawlLogMapper = crawlLogMapper;
        this.ratingMapper = ratingMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    private String randomUA() {
        return USER_AGENTS[random.nextInt(USER_AGENTS.length)];
    }

    private int randomDelay(int min, int max) {
        return (min + random.nextInt(max - min + 1)) * 1000;
    }

    @Override
    public Map<String, Integer> crawlTop250() {
        return crawlMovies(13); // 13 * 20 = 260, covers Top250
    }

    @Override
    public Map<String, Integer> crawlMovies(int pages) {
        ensurePosterDir();

        CrawlLog crawlLog = new CrawlLog();
        crawlLog.setTaskType("AJAX_API");
        crawlLog.setStatus("RUNNING");
        crawlLogMapper.insert(crawlLog);

        int inserted = 0;
        int updated = 0;
        int failed = 0;

        try {
            for (int page = 0; page < pages; page++) {
                int start = page * PAGE_SIZE;
                String apiUrl = AJAX_API + "?sort=U&range=0,10&tags=&start=" + start;

                log.info("正在爬取第{}/{}页, start={}", page + 1, pages, start);

                try {
                    String json = fetchJson(apiUrl);
                    JsonNode root = objectMapper.readTree(json);
                    JsonNode dataArr = root.get("data");

                    if (dataArr == null || dataArr.size() == 0) {
                        log.warn("第{}页无数据，可能被反爬拦截或已到末尾", page + 1);
                        Thread.sleep(randomDelay(10, 20));
                        continue;
                    }

                    for (JsonNode item : dataArr) {
                        try {
                            String doubanId = item.has("id") ? item.get("id").asText() : null;
                            String title = item.has("title") ? item.get("title").asText() : "";
                            String rate = item.has("rate") ? item.get("rate").asText() : "0";
                            String cover = item.has("cover") ? item.get("cover").asText() : "";
                            String url = item.has("url") ? item.get("url").asText() : "";

                            // 提取导演
                            String directors = "";
                            if (item.has("directors") && item.get("directors").isArray()) {
                                StringBuilder sb = new StringBuilder();
                                for (JsonNode d : item.get("directors")) {
                                    if (sb.length() > 0) sb.append(",");
                                    sb.append(d.has("name") ? d.get("name").asText() : "");
                                }
                                directors = sb.toString();
                            }

                            // 提取主演
                            String casts = "";
                            if (item.has("casts") && item.get("casts").isArray()) {
                                StringBuilder sb = new StringBuilder();
                                int count = 0;
                                for (JsonNode c : item.get("casts")) {
                                    if (count >= 3) break;
                                    if (sb.length() > 0) sb.append(",");
                                    sb.append(c.has("name") ? c.get("name").asText() : "");
                                    count++;
                                }
                                casts = sb.toString();
                            }

                            if (doubanId == null || doubanId.isEmpty()) {
                                failed++;
                                continue;
                            }

                            Movie movie = new Movie();
                            movie.setDoubanId(doubanId);
                            movie.setTitle(title);
                            movie.setDirector(directors);
                            movie.setActors(casts);
                            movie.setPosterUrl(cover);

                            if (!rate.isEmpty()) {
                                try {
                                    movie.setDoubanRating(new BigDecimal(rate));
                                } catch (NumberFormatException ignored) {
                                }
                            }

                            // 尝试获取详情页补充更多信息（genre, runtime, releaseDate, summary）
                            try {
                                Thread.sleep(randomDelay(2, 4));
                                fetchDetailInfo(movie, doubanId);
                            } catch (Exception e) {
                                log.warn("获取详情页失败, doubanId={}: {}", doubanId, e.getMessage());
                            }

                            // 保存或更新
                            LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
                            wrapper.eq(Movie::getDoubanId, doubanId);
                            Movie existing = movieMapper.selectOne(wrapper);

                            if (existing != null) {
                                existing.setTitle(movie.getTitle());
                                existing.setDirector(movie.getDirector());
                                existing.setActors(movie.getActors());
                                existing.setGenre(movie.getGenre());
                                existing.setSummary(movie.getSummary());
                                existing.setReleaseDate(movie.getReleaseDate());
                                existing.setRuntime(movie.getRuntime());
                                existing.setDoubanRating(movie.getDoubanRating());
                                if (movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty()) {
                                    existing.setPosterUrl(movie.getPosterUrl());
                                }
                                movieMapper.updateById(existing);
                                updated++;
                            } else {
                                movieMapper.insert(movie);
                                inserted++;
                            }

                        } catch (Exception e) {
                            failed++;
                            log.error("处理单部电影失败: {}", e.getMessage());
                        }
                    }

                    log.info("第{}/{}页处理完成, 本页{}部", page + 1, pages, dataArr.size());

                } catch (Exception e) {
                    failed++;
                    log.error("爬取第{}页失败: {}", page + 1, e.getMessage());
                }

                // 页间延迟
                Thread.sleep(randomDelay(3, 6));
            }

            crawlLog.setStatus("SUCCESS");
            crawlLog.setMessage("新增:" + inserted + ", 更新:" + updated + ", 失败:" + failed);
        } catch (Exception e) {
            crawlLog.setStatus("FAILED");
            crawlLog.setMessage(e.getMessage());
            log.error("爬虫任务失败: {}", e.getMessage());
        }

        crawlLog.setEndTime(java.time.LocalDateTime.now());
        crawlLogMapper.updateById(crawlLog);

        Map<String, Integer> result = new HashMap<>();
        result.put("新增", inserted);
        result.put("更新", updated);
        result.put("失败", failed);
        return result;
    }

    @Override
    public Map<String, Integer> fetchPosters() {
        ensurePosterDir();

        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Movie::getPosterUrl)
               .ne(Movie::getPosterUrl, "")
               .and(w -> w.likeRight(Movie::getPosterUrl, "http")
                          .or().likeRight(Movie::getPosterUrl, "https"));
        List<Movie> movies = movieMapper.selectList(wrapper);

        int success = 0;
        int failed = 0;

        for (Movie movie : movies) {
            try {
                String doubanId = movie.getDoubanId();
                if (doubanId == null || doubanId.isEmpty()) {
                    failed++;
                    continue;
                }

                String remoteUrl = movie.getPosterUrl();
                if (remoteUrl == null || remoteUrl.isEmpty() || !remoteUrl.startsWith("http")) {
                    failed++;
                    continue;
                }

                String localPath = downloadPoster(doubanId, remoteUrl);
                if (localPath != null) {
                    movie.setPosterUrl(localPath);
                    movieMapper.updateById(movie);
                    success++;
                    log.info("下载海报成功: {} -> {}", movie.getTitle(), localPath);
                } else {
                    failed++;
                }

                Thread.sleep(randomDelay(5, 8));
            } catch (Exception e) {
                failed++;
                log.error("下载海报失败, movieId={}: {}", movie.getId(), e.getMessage());
            }
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("成功", success);
        result.put("失败", failed);
        return result;
    }

    @Override
    public Map<String, Integer> importFromCsv() {
        int movieCount = 0;
        int ratingCount = 0;
        int userCount = 0;
        int movieFailed = 0;
        int ratingFailed = 0;
        int posterUpdated = 0;

        // 构建海报文件名集合（用于快速查找）
        File posterDirFile = new File(posterDir);
        if (!posterDirFile.isAbsolute()) {
            posterDirFile = new File(System.getProperty("user.dir"), posterDir);
        }
        Set<String> posterFiles = new HashSet<>();
        File[] files = posterDirFile.listFiles();
        if (files != null) {
            for (File f : files) {
                posterFiles.add(f.getName());
            }
        }

        // ===== 1. 导入电影数据 =====
        // douban_id -> movie 自增ID 的映射
        Map<String, Long> doubanIdToDbId = new HashMap<>();
        Map<String, Movie> existingMovieByDoubanId = new HashMap<>();

        // 先加载已有的 movie 映射
        List<Movie> existingMovies = movieMapper.selectList(null);
        for (Movie m : existingMovies) {
            if (m.getDoubanId() != null) {
                doubanIdToDbId.put(m.getDoubanId(), m.getId());
                existingMovieByDoubanId.put(m.getDoubanId(), m);
            }
        }

        String movieCsvPath = System.getProperty("user.dir") + File.separator + "douban_movies.csv";
        File movieCsvFile = new File(movieCsvPath);
        if (!movieCsvFile.exists()) {
            throw new RuntimeException("找不到 douban_movies.csv: " + movieCsvPath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(movieCsvFile), "UTF-8"))) {
            String header = reader.readLine(); // 跳过表头
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    // CSV字段: ,name,english_name,directors,writer,actors,rate,style1,style2,style3,country,language,date,duration,introduction,dataID,url,pic
                    String[] fields = parseCsvLine(line);
                    if (fields.length < 18) {
                        movieFailed++;
                        continue;
                    }

                    String name = fields[1].trim();
                    String directors = fields[3].trim();
                    String actors = fields[5].trim();
                    String rate = fields[6].trim();
                    String style1 = fields[7].trim();
                    String style2 = fields[8].trim();
                    String style3 = fields[9].trim();
                    String dateStr = fields[12].trim();
                    String durationStr = fields[13].trim();
                    String introduction = fields[14].trim();
                    String dataID = fields[15].trim();
                    String remotePosterUrl = fields[17].trim();

                    if (name.isEmpty() || dataID.isEmpty()) {
                        movieFailed++;
                        continue;
                    }

                    // 已有电影也要补写空海报，避免重新导入时永久跳过缺失数据
                    Movie existingMovie = existingMovieByDoubanId.get(dataID);
                    if (existingMovie != null) {
                        if (existingMovie.getPosterUrl() == null || existingMovie.getPosterUrl().trim().isEmpty()) {
                            String posterUrl = resolveImportedPosterUrl(name, dataID, remotePosterUrl, posterFiles);
                            if (posterUrl != null) {
                                existingMovie.setPosterUrl(posterUrl);
                                movieMapper.updateById(existingMovie);
                                posterUpdated++;
                            }
                        }
                        continue;
                    }

                    Movie movie = new Movie();
                    movie.setDoubanId(dataID);
                    movie.setTitle(name);
                    movie.setDirector(directors);
                    movie.setActors(actors);

                    // 拼接类型
                    StringBuilder genreSb = new StringBuilder();
                    if (!style1.isEmpty()) genreSb.append(style1);
                    if (!style2.isEmpty()) {
                        if (genreSb.length() > 0) genreSb.append(",");
                        genreSb.append(style2);
                    }
                    if (!style3.isEmpty()) {
                        if (genreSb.length() > 0) genreSb.append(",");
                        genreSb.append(style3);
                    }
                    movie.setGenre(genreSb.toString());

                    // 评分
                    if (!rate.isEmpty()) {
                        try {
                            BigDecimal rating = new BigDecimal(rate);
                            movie.setDoubanRating(rating);
                            movie.setAvgRating(rating); // 用豆瓣评分初始化站内均分
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    // 上映日期
                    if (!dateStr.isEmpty()) {
                        try {
                            // 尝试解析年份
                            String year = dateStr.replaceAll("[^0-9]", "").trim();
                            if (year.length() >= 4) {
                                movie.setReleaseDate(LocalDate.of(Integer.parseInt(year.substring(0, 4)), 1, 1));
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    // 片长
                    if (!durationStr.isEmpty()) {
                        try {
                            String numStr = durationStr.replaceAll("[^0-9]", "").trim();
                            if (!numStr.isEmpty()) {
                                movie.setRuntime(Integer.parseInt(numStr));
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    // 简介
                    movie.setSummary(introduction);

                    movie.setPosterUrl(resolveImportedPosterUrl(name, dataID, remotePosterUrl, posterFiles));

                    movie.setRatingCount(0);

                    movieMapper.insert(movie);
                    doubanIdToDbId.put(dataID, movie.getId());
                    movieCount++;

                } catch (Exception e) {
                    movieFailed++;
                    log.error("导入电影失败: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取 douban_movies.csv 失败: " + e.getMessage());
        }

        log.info("电影导入完成: 成功{}, 失败{}", movieCount, movieFailed);

        // ===== 2. 导入用户评分数据 =====
        // 豆瓣用户ID -> 数据库用户ID 的映射
        Map<String, Long> doubanUserIdToDbId = new HashMap<>();

        // 先加载已有的用户映射
        List<User> existingUsers = userMapper.selectList(null);
        for (User u : existingUsers) {
            doubanUserIdToDbId.put(u.getUsername(), u.getId());
        }

        String userCsvPath = System.getProperty("user.dir") + File.separator + "douban_users.csv";
        File userCsvFile = new File(userCsvPath);
        if (!userCsvFile.exists()) {
            log.warn("找不到 douban_users.csv: {}, 跳过用户评分导入", userCsvPath);
        } else {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(userCsvFile), "UTF-8"))) {
                String header = reader.readLine(); // 跳过表头
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        String[] fields = parseCsvLine(line);
                        if (fields.length < 4) {
                            ratingFailed++;
                            continue;
                        }

                        String doubanUserId = fields[1].trim();
                        String doubanMovieId = fields[2].trim();
                        String ratingStr = fields[3].trim();

                        if (doubanUserId.isEmpty() || doubanMovieId.isEmpty() || ratingStr.isEmpty()) {
                            ratingFailed++;
                            continue;
                        }

                        // 查找 movie 数据库ID
                        Long movieDbId = doubanIdToDbId.get(doubanMovieId);
                        if (movieDbId == null) {
                            ratingFailed++;
                            continue; // 电影不存在，跳过
                        }

                        // 查找或创建用户
                        String username = "douban_" + doubanUserId;
                        Long userDbId = doubanUserIdToDbId.get(username);
                        if (userDbId == null) {
                            User newUser = new User();
                            newUser.setUsername(username);
                            newUser.setPassword(passwordEncoder.encode("douban123"));
                            newUser.setPreferences("");
                            userMapper.insert(newUser);
                            userDbId = newUser.getId();
                            doubanUserIdToDbId.put(username, userDbId);
                            userCount++;
                        }

                        // 插入评分
                        double score;
                        try {
                            score = Double.parseDouble(ratingStr);
                        } catch (NumberFormatException e) {
                            ratingFailed++;
                            continue;
                        }

                        // 检查是否已存在评分
                        LambdaQueryWrapper<Rating> ratingWrapper = new LambdaQueryWrapper<>();
                        ratingWrapper.eq(Rating::getUserId, userDbId).eq(Rating::getMovieId, movieDbId);
                        if (ratingMapper.selectCount(ratingWrapper) > 0) {
                            continue; // 跳过重复
                        }

                        Rating rating = new Rating();
                        rating.setUserId(userDbId);
                        rating.setMovieId(movieDbId);
                        rating.setScore(score);
                        ratingMapper.insert(rating);
                        ratingCount++;

                    } catch (Exception e) {
                        ratingFailed++;
                        log.error("导入评分失败: {}", e.getMessage());
                    }
                }
            } catch (IOException e) {
                log.error("读取 douban_users.csv 失败: {}", e.getMessage());
            }
        }

        log.info("评分导入完成: 成功{}, 失败{}, 新增用户{}", ratingCount, ratingFailed, userCount);

        // ===== 3. 更新电影的评分统计 =====
        List<Movie> allMovies = movieMapper.selectList(null);
        for (Movie m : allMovies) {
            LambdaQueryWrapper<Rating> rw = new LambdaQueryWrapper<>();
            rw.eq(Rating::getMovieId, m.getId());
            Long cnt = ratingMapper.selectCount(rw);
            if (cnt != null && cnt > 0) {
                m.setRatingCount(cnt.intValue());
                // 计算平均分
                List<Rating> ratings = ratingMapper.selectList(rw);
                double avg = ratings.stream().mapToDouble(Rating::getScore).average().orElse(0.0);
                m.setAvgRating(BigDecimal.valueOf(avg).setScale(1, BigDecimal.ROUND_HALF_UP));
                movieMapper.updateById(m);
            }
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("电影导入成功", movieCount);
        result.put("电影导入失败", movieFailed);
        result.put("海报地址补写", posterUpdated);
        result.put("评分导入成功", ratingCount);
        result.put("评分导入失败", ratingFailed);
        result.put("新增虚拟用户", userCount);
        return result;
    }

    /**
     * 解析CSV行，处理引号内的逗号
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private String resolveImportedPosterUrl(String title, String doubanId, String remotePosterUrl,
                                            Set<String> posterFiles) {
        String titleFileName = title + ".jpg";
        if (posterFiles.contains(titleFileName)) {
            return "/api/posters/" + titleFileName;
        }

        String idFileName = doubanId + ".jpg";
        if (posterFiles.contains(idFileName)) {
            return "/api/posters/" + idFileName;
        }

        return remotePosterUrl == null || remotePosterUrl.trim().isEmpty()
                ? null
                : remotePosterUrl.trim();
    }

    /**
     * 通过HTTP请求获取AJAX API的JSON响应
     */
    private String fetchJson(String apiUrl) throws Exception {
        int maxRetries = 3;
        Exception lastException = null;

        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", randomUA());
                conn.setRequestProperty("Referer", REFERER);
                conn.setRequestProperty("Accept", "application/json, text/plain, */*");
                conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    try (InputStream in = conn.getInputStream()) {
                        byte[] bytes = in.readAllBytes();
                        String body = new String(bytes, "UTF-8");

                        // 检测反爬拦截
                        if (body.contains("检测到有异常请求") || body.contains("forbidden")) {
                            log.warn("被反爬拦截，等待60秒后重试...");
                            Thread.sleep(60000);
                            continue;
                        }
                        return body;
                    }
                } else if (code == 403 || code == 429) {
                    log.warn("请求被限制(code={}), 等待60秒后重试...", code);
                    Thread.sleep(60000);
                    continue;
                } else {
                    throw new RuntimeException("HTTP " + code);
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("请求失败(重试{}/{}): {}", retry + 1, maxRetries, e.getMessage());
                if (retry < maxRetries - 1) {
                    Thread.sleep((retry + 1) * 10000L);
                }
            }
        }
        throw lastException != null ? lastException : new RuntimeException("请求失败");
    }

    /**
     * 获取电影详情页补充信息（genre, runtime, releaseDate, summary）
     */
    private void fetchDetailInfo(Movie movie, String doubanId) {
        try {
            String detailUrl = "https://movie.douban.com/subject/" + doubanId + "/";
            Document doc = Jsoup.connect(detailUrl)
                    .userAgent(randomUA())
                    .referrer(REFERER)
                    .timeout(10000)
                    .get();

            // 类型
            StringBuilder genres = new StringBuilder();
            for (Element el : doc.select("span[property=v:genre]")) {
                if (genres.length() > 0) genres.append(",");
                genres.append(el.text());
            }
            movie.setGenre(genres.toString());

            // 片长
            Element runtimeEl = doc.select("span[property=v:runtime]").first();
            if (runtimeEl != null) {
                String text = runtimeEl.text();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)分钟").matcher(text);
                if (m.find()) {
                    movie.setRuntime(Integer.parseInt(m.group(1)));
                }
            }

            // 上映日期
            Element dateEl = doc.select("span[property=v:initialReleaseDate]").first();
            if (dateEl != null) {
                String text = dateEl.text();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(text);
                if (m.find()) {
                    movie.setReleaseDate(LocalDate.parse(m.group(1), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                }
            }

            // 简介
            Element summaryEl = doc.select("span[property=v:summary]").first();
            if (summaryEl != null) {
                movie.setSummary(summaryEl.text().trim());
            }

        } catch (Exception e) {
            log.warn("获取详情页信息失败, doubanId={}: {}", doubanId, e.getMessage());
        }
    }

    private void ensurePosterDir() {
        File dir = new File(posterDir);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), posterDir);
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private String downloadPoster(String doubanId, String remoteUrl) {
        try {
            File dir = new File(posterDir);
            if (!dir.isAbsolute()) {
                dir = new File(System.getProperty("user.dir"), posterDir);
            }

            String fileName = doubanId + ".jpg";
            File outFile = new File(dir, fileName);

            if (outFile.exists() && outFile.length() > 0) {
                return "/api/posters/" + fileName;
            }

            int maxRetries = 3;
            for (int retry = 0; retry < maxRetries; retry++) {
                try {
                    URL url = new URL(remoteUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", randomUA());
                    conn.setRequestProperty("Referer", REFERER);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    try (InputStream in = conn.getInputStream();
                         FileOutputStream out = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }

                    return "/api/posters/" + fileName;
                } catch (Exception e) {
                    if (retry < maxRetries - 1) {
                        Thread.sleep((retry + 1) * 5000L);
                    } else {
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            log.error("下载海报失败, doubanId={}, url={}: {}", doubanId, remoteUrl, e.getMessage());
        }
        return null;
    }
}
