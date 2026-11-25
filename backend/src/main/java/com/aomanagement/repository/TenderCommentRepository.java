package com.aomanagement.repository;

import com.aomanagement.entity.TenderComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenderCommentRepository extends JpaRepository<TenderComment, Long> {
    List<TenderComment> findByTenderIdOrderByCreatedAtAsc(Long tenderId);
}
