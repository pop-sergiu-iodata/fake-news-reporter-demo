package com.automatica.fakenews.repository;

import com.automatica.fakenews.model.FactCheckHistory;
import com.automatica.fakenews.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactCheckHistoryRepository extends JpaRepository<FactCheckHistory, Long> {
    List<FactCheckHistory> findByUserOrderByTimestampDesc(User user);
}
