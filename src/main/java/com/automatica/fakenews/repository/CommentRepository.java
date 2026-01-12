package com.automatica.fakenews.repository;

import com.automatica.fakenews.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByReportIdOrderByCreatedAtDesc(Long reportId);
}
