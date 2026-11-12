package com.example.aispring.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aispring.entity.ConsultationSession;
import com.example.aispring.entity.EmotionDiary;
import com.example.aispring.mapper.AnalyticsMapper;
import com.example.aispring.mapper.ConsultationSessionMapper;
import com.example.aispring.mapper.EmotionDiaryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板聚合。
 * 前端 dashboard.vue 会直接对 systemOverview / emotionTrend / consultationStats.dailyTrend / userActivity
 * 取属性并 map，所以这几个 key 必须全部返回，缺一个就是整页 JS 报错。
 */
@Service
public class AnalyticsService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    @Autowired
    private AnalyticsMapper analyticsMapper;
    @Autowired
    private EmotionDiaryMapper diaryMapper;
    @Autowired
    private ConsultationSessionMapper sessionMapper;

    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("systemOverview", systemOverview());
        result.put("emotionTrend", emotionTrend());
        result.put("consultationStats", consultationStats());
        result.put("userActivity", userActivity());
        return result;
    }

    private Map<String, Object> systemOverview() {
        LocalDate today = LocalDate.now();

        QueryWrapper<EmotionDiary> diaryToday = new QueryWrapper<>();
        diaryToday.eq("diary_date", today);
        long todayNewDiaries = diaryMapper.selectCount(diaryToday);

        QueryWrapper<ConsultationSession> sessionToday = new QueryWrapper<>();
        sessionToday.ge("started_at", today.atStartOfDay());
        long todayNewSessions = sessionMapper.selectCount(sessionToday);

        long totalSessions = sessionMapper.selectCount(null);
        long totalDiaries = diaryMapper.selectCount(null);
        Double avgMoodScore = analyticsMapper.avgMoodScore();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalUsers", analyticsMapper.countUsers());
        overview.put("activeUsers", activeUsersInLast7Days());
        overview.put("totalDiaries", totalDiaries);
        overview.put("todayNewDiaries", todayNewDiaries);
        overview.put("totalSessions", totalSessions);
        overview.put("todayNewSessions", todayNewSessions);
        overview.put("avgMoodScore", avgMoodScore == null ? 0 : avgMoodScore);
        return overview;
    }

    /**
     * 情绪趋势：近 7 天，缺失日期补 0，保证 x 轴连续
     */
    private List<Map<String, Object>> emotionTrend() {
        Map<String, Map<String, Object>> byDate = indexByDate(analyticsMapper.emotionTrend());
        List<Map<String, Object>> trend = new ArrayList<>();
        for (String day : last7Days()) {
            Map<String, Object> row = byDate.get(day);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day);
            item.put("avgMoodScore", row == null || row.get("avgMoodScore") == null ? 0 : row.get("avgMoodScore"));
            item.put("recordCount", row == null || row.get("recordCount") == null ? 0 : row.get("recordCount"));
            trend.add(item);
        }
        return trend;
    }

    private Map<String, Object> consultationStats() {
        Map<String, Map<String, Object>> byDate = indexByDate(analyticsMapper.consultationDailyTrend());
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (String day : last7Days()) {
            Map<String, Object> row = byDate.get(day);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day);
            item.put("sessionCount", row == null || row.get("sessionCount") == null ? 0 : row.get("sessionCount"));
            item.put("userCount", row == null || row.get("userCount") == null ? 0 : row.get("userCount"));
            dailyTrend.add(item);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSessions", sessionMapper.selectCount(null));
        stats.put("avgDurationMinutes", defaultIfNull(analyticsMapper.avgDurationMinutes()));
        stats.put("dailyTrend", dailyTrend);
        return stats;
    }

    /**
     * 用户活跃度：新增用户、日记用户、咨询用户、当天活跃用户（写日记或发起会话）
     */
    private List<Map<String, Object>> userActivity() {
        Map<String, Map<String, Object>> newUsers = indexByDate(analyticsMapper.newUserTrend());
        Map<String, Map<String, Object>> diaryUsers = indexByDate(analyticsMapper.diaryUserTrend());
        Map<String, Map<String, Object>> sessionUsers = indexByDate(analyticsMapper.consultationDailyTrend());
        Map<String, Map<String, Object>> activeUsers = indexByDate(analyticsMapper.activeUserTrend());

        List<Map<String, Object>> activity = new ArrayList<>();
        for (String day : last7Days()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day);
            item.put("activeUsers", valueOf(activeUsers, day, "activeUsers"));
            item.put("newUsers", valueOf(newUsers, day, "newUsers"));
            item.put("diaryUsers", valueOf(diaryUsers, day, "diaryUsers"));
            item.put("consultationUsers", valueOf(sessionUsers, day, "userCount"));
            activity.add(item);
        }
        return activity;
    }

    private long activeUsersInLast7Days() {
        Object value = analyticsMapper.countUsers();
        long total = value instanceof Number number ? number.longValue() : 0L;
        // 近 7 天有写日记或发起会话的用户数，取两个来源的并集由 SQL 里 UNION 完成
        long active = 0L;
        for (Map<String, Object> row : analyticsMapper.activeUserTrend()) {
            Object count = row.get("activeUsers");
            if (count instanceof Number number) {
                active += number.longValue();
            }
        }
        return Math.min(active, total);
    }

    private Object valueOf(Map<String, Map<String, Object>> indexed, String day, String key) {
        Map<String, Object> row = indexed.get(day);
        if (row == null || row.get(key) == null) {
            return 0;
        }
        return row.get(key);
    }

    private Map<String, Map<String, Object>> indexByDate(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object date = row.get("date");
            if (date != null) {
                map.put(date.toString(), row);
            }
        }
        return map;
    }

    private List<String> last7Days() {
        List<String> days = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            days.add(today.minusDays(i).format(DAY_FORMAT));
        }
        return days;
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
