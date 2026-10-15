package com.banduairlines.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banduairlines.model.Extra;

public interface ExtraRepository extends JpaRepository<Extra, Long>{

    List<Extra> findByActivoTrueOrderByIdAsc();
}
