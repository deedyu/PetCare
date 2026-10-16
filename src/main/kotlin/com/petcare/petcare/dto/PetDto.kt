package com.petcare.petcare.dto

data class PetDto(
    val id: Long?,
    val name: String,
    val type: String,
    val age: Int,
    val imageUrl: String?,
    val familyName: String? = null
)



