package com.petcare.petcare.dto

data class PetDto(
    val id: Long?,
    val name: String,
    val type: String,
    val age: Int,
    val imageUrl: String?,
    val profileDisplayType: String = "INITIAL",
    val profileEmoji: String? = null,
    val familyName: String? = null
)



