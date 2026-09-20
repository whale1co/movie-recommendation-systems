package com.movierec.ai;

import com.movierec.ai.model.MovieIntent;
import com.movierec.ai.service.LocalIntentParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalIntentParserTest {
    private final LocalIntentParser parser = new LocalIntentParser();

    @Test
    void parsesRuntimeRatingGenreAndExclusion() {
        MovieIntent intent = parser.parse("想看两个小时以内、评分8分以上的轻松喜剧，不要恐怖片");

        assertEquals(120, intent.maxRuntime());
        assertEquals(8.0, intent.minRating());
        assertTrue(intent.genres().contains("喜剧"));
        assertTrue(intent.excludedGenres().contains("恐怖"));
        assertTrue(intent.keywords().contains("轻松"));
    }
}
