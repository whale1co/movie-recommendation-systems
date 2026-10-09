package com.movierec.ai.service;

import com.movierec.ai.model.MovieIntent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LocalIntentParser {
    private static final List<String> GENRES = List.of(
            "剧情", "喜剧", "爱情", "动作", "科幻", "动画", "悬疑", "犯罪", "冒险", "奇幻",
            "家庭", "历史", "战争", "音乐", "纪录片", "恐怖", "惊悚", "儿童", "古装", "传记");
    private static final Pattern RUNTIME_MINUTES = Pattern.compile("(\\d{2,3})\\s*(?:分钟|分|min)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RUNTIME_HOURS = Pattern.compile("(\\d(?:\\.\\d+)?)\\s*(?:小时|h)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RATING = Pattern.compile("(?:评分|豆瓣)[^0-9]{0,5}(\\d(?:\\.\\d+)?)\\s*(?:分)?(?:以上|起)?");
    private static final Pattern YEAR = Pattern.compile("(?<!\\d)((?:19|20)\\d{2})(?!\\d)");

    public MovieIntent parse(String question) {
        Set<String> included = new LinkedHashSet<>();
        Set<String> excluded = new LinkedHashSet<>();
        for (String genre : GENRES) {
            int index = question.indexOf(genre);
            if (index < 0) continue;
            String prefix = question.substring(Math.max(0, index - 6), index);
            if (prefix.matches(".*(?:不要|不看|排除|拒绝|避开|非|别).*")) excluded.add(genre);
            else included.add(genre);
        }
        included.removeAll(excluded);

        Integer maxRuntime = parseRuntime(question);
        Double minRating = firstDouble(RATING, question);
        List<Integer> years = matches(YEAR, question).stream().map(Integer::valueOf).toList();
        Integer minYear = years.size() >= 2 ? years.get(0) : null;
        Integer maxYear = years.size() >= 2 ? years.get(1) : null;
        if (years.size() == 1) {
            if (question.contains("以后") || question.contains("之后")) minYear = years.get(0);
            if (question.contains("以前") || question.contains("之前")) maxYear = years.get(0);
        }

        Set<String> keywords = new LinkedHashSet<>();
        List.of("轻松", "温馨", "治愈", "搞笑", "烧脑", "感人", "励志", "经典", "节奏快", "适合全家")
                .stream().filter(question::contains).forEach(keywords::add);
        return new MovieIntent(new ArrayList<>(included), new ArrayList<>(excluded), maxRuntime,
                minRating, minYear, maxYear, new ArrayList<>(keywords)).normalized();
    }

    public MovieIntent merge(MovieIntent local, MovieIntent model) {
        if (model == null) return local;
        MovieIntent normalized = model.normalized();
        return new MovieIntent(union(local.genres(), normalized.genres()),
                union(local.excludedGenres(), normalized.excludedGenres()),
                local.maxRuntime() != null ? local.maxRuntime() : normalized.maxRuntime(),
                local.minRating() != null ? local.minRating() : normalized.minRating(),
                local.minYear() != null ? local.minYear() : normalized.minYear(),
                local.maxYear() != null ? local.maxYear() : normalized.maxYear(),
                union(local.keywords(), normalized.keywords())).normalized();
    }

    private List<String> union(List<String> first, List<String> second) {
        Set<String> values = new LinkedHashSet<>(first);
        values.addAll(second);
        return new ArrayList<>(values);
    }

    private Integer firstInt(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private Integer parseRuntime(String question) {
        Integer minutes = firstInt(RUNTIME_MINUTES, question);
        if (minutes != null) return minutes;
        Double hours = firstDouble(RUNTIME_HOURS, question);
        if (hours != null) return (int) Math.round(hours * 60);
        if (question.contains("一个半小时") || question.contains("一小时半")) return 90;
        if (question.contains("两个半小时") || question.contains("两小时半")) return 150;
        if (question.contains("半小时")) return 30;
        if (question.contains("一个小时") || question.contains("一小时")) return 60;
        if (question.contains("两个小时") || question.contains("两小时")) return 120;
        if (question.contains("三个小时") || question.contains("三小时")) return 180;
        return null;
    }

    private Double firstDouble(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? Double.valueOf(matcher.group(1)) : null;
    }

    private List<String> matches(Pattern pattern, String input) {
        List<String> values = new ArrayList<>();
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) values.add(matcher.group(1));
        return values;
    }
}
