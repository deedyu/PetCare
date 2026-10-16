package com.petcare.petcare.repository

import com.petcare.petcare.entity.Family
import org.springframework.data.jpa.repository.JpaRepository

interface FamilyRepository : JpaRepository<Family, Long>
