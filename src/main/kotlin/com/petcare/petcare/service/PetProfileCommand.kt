package com.petcare.petcare.service

import com.petcare.petcare.entity.ProfileDisplayType

data class PetProfileCommand(
    val displayType: ProfileDisplayType,
    val emoji: String? = null,
    val photoJpeg: ByteArray? = null
)
