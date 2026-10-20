
package com.petcare.petcare.api

import com.petcare.petcare.dto.PetDto
import com.petcare.petcare.entity.ProfileDisplayType
import com.petcare.petcare.security.CurrentUser
import com.petcare.petcare.service.FamilyService
import com.petcare.petcare.service.PetProfileCommand
import com.petcare.petcare.service.PetService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/pets")
class PetApiController(
    private val petService: PetService,
    private val currentUser: CurrentUser,
    private val familyService: FamilyService
) {

    // 인증된 사용자만 자신의 반려동물을 다룬다.
    @GetMapping
    fun getAllPets(): ResponseEntity<*> {
        val user = currentUser.optional()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()

        val userId = user.id
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()

        val pets = petService.getVisiblePets(userId)
        return ResponseEntity.ok(pets.map { it.toDto() })
    }

    // 반려동물 상세 조회 (GET)
    @GetMapping("/{petId}")
    fun getPet(
        @PathVariable petId: Long
    ): ResponseEntity<*> {
        val user = currentUser.optional()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()

        val pet = petService.getPet(petId)

        if (!familyService.canAccessPet(user.id!!, petId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>()
        }

        return ResponseEntity.ok(pet.toDto())
    }

    // 반려동물 등록 (POST)
    @PostMapping
    fun createPet(
        @RequestBody request: CreatePetRequest
    ): ResponseEntity<*> {
        val user = currentUser.optional()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()

        return try {
            val type = when (request.profileDisplayType?.trim()?.uppercase()) {
                null, "", "INITIAL" -> ProfileDisplayType.INITIAL
                "EMOJI" -> ProfileDisplayType.EMOJI
                "PHOTO" -> return ResponseEntity.badRequest().body("사진은 파일로 업로드해 주세요.")
                else -> return ResponseEntity.badRequest().body("표시 방식을 선택하세요.")
            }
            val pet = petService.createPet(
                user = user,
                name = request.name,
                type = request.type,
                age = request.age,
                profile = PetProfileCommand(type, request.profileEmoji)
            )
            ResponseEntity.ok(pet.toDto())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    // 반려동물 수정 (PUT)
    @PutMapping("/{petId}")
    fun updatePet(
        @PathVariable petId: Long,
        @RequestBody request: UpdatePetRequest
    ): ResponseEntity<*> {
        val user = currentUser.optional()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()

        val userId = user.id
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()

        val pet = petService.getPet(petId)

        if (pet.user?.id != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>()
        }

        return try {
            val updatedPet = petService.updatePet(
                userId = userId,
                petId = petId,
                name = request.name,
                type = request.type,
                age = request.age
            )
            ResponseEntity.ok(updatedPet.toDto())
        } catch (e: IllegalArgumentException) {
            if (e.message?.startsWith("unauthorized") == true) {
                ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>()
            } else {
                ResponseEntity.badRequest().body(e.message)
            }
        }
    }

    // 반려동물 삭제 (DELETE)
    @DeleteMapping("/{petId}")
    fun deletePet(
        @PathVariable petId: Long
    ): ResponseEntity<*> {
        val user = currentUser.optional()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()

        val userId = user.id
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()

        val pet = petService.getPet(petId)

        if (pet.user?.id != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>()
        }

        petService.deletePet(userId, petId)
        return ResponseEntity.noContent().build<Void>()
    }

    // Pet 엔티티를 DTO로 변환
    private fun com.petcare.petcare.entity.Pet.toDto() = PetDto(
        id = id,
        name = name,
        type = type,
        age = age,
        imageUrl = if (resolvedDisplayType == ProfileDisplayType.PHOTO && !photoKey.isNullOrBlank()) {
            "/pets/$id/photo"
        } else {
            null
        },
        profileDisplayType = resolvedDisplayType.name,
        profileEmoji = profileEmoji,
        familyName = family?.name
    )

    data class CreatePetRequest(
        val name: String,
        val type: String,
        val age: Int,
        val imageUrl: String? = null,
        val profileDisplayType: String? = null,
        val profileEmoji: String? = null
    )

    data class UpdatePetRequest(
        val name: String,
        val type: String,
        val age: Int
    )
}
