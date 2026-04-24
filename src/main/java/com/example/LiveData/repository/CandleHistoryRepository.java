package com.example.LiveData.repository;

import com.example.LiveData.entity.CandleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandleHistoryRepository extends JpaRepository<CandleHistory, Long> {

    @Query(value = "SELECT * FROM candle_history WHERE instrument_key = :key ORDER BY timestamp DESC LIMIT :lim",
           nativeQuery = true)
    List<CandleHistory> findLatestCandles(@Param("key") String instrumentKey, @Param("lim") int limit);
}
