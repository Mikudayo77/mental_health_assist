package com.example.aispring.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 看板统计用的聚合查询（MySQL）
 */
@Mapper
public interface AnalyticsMapper {

    @Select("SELECT COUNT(*) FROM `user`")
    long countUsers();

    @Select("SELECT ROUND(AVG(mood_score), 1) FROM emotion_diary WHERE mood_score IS NOT NULL")
    Double avgMoodScore();

    @Select("SELECT ROUND(AVG(TIMESTAMPDIFF(MINUTE, s.started_at, t.last_at)), 0) "
            + "FROM consultation_session s "
            + "JOIN (SELECT session_id, MAX(created_at) AS last_at FROM consultation_message GROUP BY session_id) t "
            + "ON t.session_id = s.id "
            + "WHERE s.started_at IS NOT NULL")
    Integer avgDurationMinutes();

    @Select("SELECT DATE_FORMAT(diary_date, '%m-%d') AS date, "
            + "ROUND(AVG(mood_score), 1) AS avgMoodScore, COUNT(*) AS recordCount "
            + "FROM emotion_diary WHERE diary_date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) "
            + "GROUP BY diary_date ORDER BY diary_date")
    List<Map<String, Object>> emotionTrend();

    @Select("SELECT DATE_FORMAT(started_at, '%m-%d') AS date, COUNT(*) AS sessionCount, "
            + "COUNT(DISTINCT user_id) AS userCount "
            + "FROM consultation_session WHERE started_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) "
            + "GROUP BY DATE_FORMAT(started_at, '%m-%d') ORDER BY MIN(started_at)")
    List<Map<String, Object>> consultationDailyTrend();

    @Select("SELECT DATE_FORMAT(created_at, '%m-%d') AS date, COUNT(*) AS newUsers "
            + "FROM `user` WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) "
            + "GROUP BY DATE_FORMAT(created_at, '%m-%d') ORDER BY MIN(created_at)")
    List<Map<String, Object>> newUserTrend();

    @Select("SELECT DATE_FORMAT(diary_date, '%m-%d') AS date, COUNT(DISTINCT user_id) AS diaryUsers "
            + "FROM emotion_diary WHERE diary_date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) "
            + "GROUP BY diary_date ORDER BY diary_date")
    List<Map<String, Object>> diaryUserTrend();

    @Select("SELECT DATE_FORMAT(t.date, '%m-%d') AS date, COUNT(DISTINCT t.user_id) AS activeUsers FROM ("
            + "SELECT DATE(diary_date) AS date, user_id FROM emotion_diary "
            + "WHERE diary_date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) "
            + "UNION ALL "
            + "SELECT DATE(started_at) AS date, user_id FROM consultation_session "
            + "WHERE started_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)"
            + ") t GROUP BY t.date ORDER BY t.date")
    List<Map<String, Object>> activeUserTrend();
}
