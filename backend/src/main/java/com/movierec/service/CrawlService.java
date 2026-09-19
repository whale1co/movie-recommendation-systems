package com.movierec.service;

import java.util.Map;

public interface CrawlService {

    Map<String, Integer> crawlTop250();

    Map<String, Integer> crawlMovies(int pages);

    Map<String, Integer> fetchPosters();

    Map<String, Integer> importFromCsv();
}
