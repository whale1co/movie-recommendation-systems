package com.movierec.service;

import java.util.Map;
import java.nio.file.Path;

public interface CrawlService {

    Map<String, Integer> crawlTop250();

    Map<String, Integer> crawlMovies(int pages);

    /** Executes an inclusive page range. Pages are zero-based and the page size is bounded by the service. */
    Map<String, Integer> crawlMovies(int startPage, int endPage, int pageSize, boolean overwrite);

    Map<String, Integer> fetchPosters();

    Map<String, Integer> importFromCsv();

    /** Imports a supplied movie CSV. The optional sibling douban_users.csv is imported when present. */
    Map<String, Integer> importFromCsv(Path movieCsvPath);
}
