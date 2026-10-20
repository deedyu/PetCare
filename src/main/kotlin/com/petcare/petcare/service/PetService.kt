package com.petcare.petcare.service

import com.petcare.petcare.entity.Pet
import com.petcare.petcare.entity.ProfileDisplayType
import com.petcare.petcare.entity.User
import com.petcare.petcare.repository.HealthRecordRepository
import com.petcare.petcare.repository.PetRepository
import com.petcare.petcare.repository.TaskOccurrenceRepository
import com.petcare.petcare.repository.TaskRepository
import com.petcare.petcare.text.EmojiRules
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PetService(
    private val petRepository: PetRepository,
    private val taskRepository: TaskRepository,
    private val occurrenceRepository: TaskOccurrenceRepository,
    private val healthRecordRepository: HealthRecordRepository,
    private val photoStore: PetPhotoStore
) {

    // ✅ 사용자별 펫 목록 조회
    fun getPetsByUser(userId: Long): List<Pet> {
        return petRepository.findByUserIdOrderByIdDesc(userId)
    }

    // ✅ 사용자별 최신 펫 목록 조회 (최대 3개)
    fun getLatestPetsByUser(userId: Long, limit: Int = 3): List<Pet> {
        return petRepository.findTop3ByUserIdOrderByIdDesc(userId)
    }

    fun getVisiblePets(userId: Long): List<Pet> {
        return petRepository.findVisibleToUser(userId)
    }

    // ✅ 전체 펫 목록 조회 (관리자용)
    fun getAllPets(): List<Pet> {
        return petRepository.findAll()
    }

    // ✅ 단일 펫 조회 (Task 쪽에서 사용)
    fun getPet(petId: Long): Pet {
        return petRepository.findById(petId)
            .orElseThrow { IllegalArgumentException("pet not found: $petId") }
    }

    fun createPet(user: User, name: String, type: String, age: Int): Pet {
        return createPet(user, name, type, age, PetProfileCommand(ProfileDisplayType.INITIAL))
    }

    @Transactional
    fun createPet(user: User, name: String, type: String, age: Int, profile: PetProfileCommand): Pet {
        val checked = requirePet(name, type, age)
        val pet = Pet(
            name = checked.first,
            type = checked.second,
            age = age,
            profileDisplayType = ProfileDisplayType.INITIAL,
            user = user
        )
        val staged = applyProfile(pet, profile)
        return saveProfile(pet, staged)
    }

    fun updatePet(userId: Long, petId: Long, name: String, type: String, age: Int): Pet {
        val pet = ownedPet(userId, petId)
        val checked = requirePet(name, type, age)
        pet.name = checked.first
        pet.type = checked.second
        pet.age = age
        return petRepository.save(pet)
    }

    @Transactional
    fun updatePet(userId: Long, petId: Long, name: String, type: String, age: Int, profile: PetProfileCommand): Pet {
        val pet = ownedPet(userId, petId)
        val checked = requirePet(name, type, age)
        pet.name = checked.first
        pet.type = checked.second
        pet.age = age
        val staged = applyProfile(pet, profile)
        return saveProfile(pet, staged)
    }

    @Transactional
    fun clearPhoto(userId: Long, petId: Long) {
        val pet = ownedPet(userId, petId)
        val key = pet.photoKey
        pet.photoKey = null
        if (pet.resolvedDisplayType == ProfileDisplayType.PHOTO) {
            pet.profileDisplayType = ProfileDisplayType.INITIAL
            pet.profileEmoji = null
        }
        petRepository.save(pet)
        photoStore.delete(key)
    }

    private fun requirePet(name: String, type: String, age: Int): Pair<String, String> {
        val trimmedName = name.trim()
        val trimmedType = type.trim()
        if (trimmedName.isEmpty() || trimmedType.isEmpty()) {
            throw IllegalArgumentException("이름과 종류를 입력하세요.")
        }
        if (age < 0) {
            throw IllegalArgumentException("나이는 0 이상이어야 합니다.")
        }
        return trimmedName to trimmedType
    }

    // 반려동물과 연결된 일정, 수행 기록, 건강 기록을 먼저 지운다.
    @Transactional
    fun deletePet(userId: Long, petId: Long) {
        val pet = ownedPet(userId, petId)
        val key = pet.photoKey
        deletePetGraph(petId)
        photoStore.delete(key)
    }

    @Transactional
    fun deletePet(petId: Long) {
        val pet = petRepository.findById(petId)
            .orElseThrow { IllegalArgumentException("pet not found: $petId") }
        val key = pet.photoKey
        deletePetGraph(petId)
        photoStore.delete(key)
    }

    private fun ownedPet(userId: Long, petId: Long): Pet {
        val pet = petRepository.findById(petId)
            .orElseThrow { IllegalArgumentException("pet not found: $petId") }
        if (pet.user?.id != userId) {
            throw IllegalArgumentException("unauthorized: pet does not belong to user")
        }
        return pet
    }

    private fun applyProfile(pet: Pet, profile: PetProfileCommand): StagedPhoto {
        val previous = pet.photoKey
        when (profile.displayType) {
            ProfileDisplayType.INITIAL -> {
                pet.profileDisplayType = ProfileDisplayType.INITIAL
                pet.profileEmoji = null
                pet.photoKey = null
                return StagedPhoto(created = null, removeAfterSave = previous)
            }
            ProfileDisplayType.EMOJI -> {
                pet.profileEmoji = EmojiRules.requireSingle(profile.emoji)
                pet.profileDisplayType = ProfileDisplayType.EMOJI
                pet.photoKey = null
                return StagedPhoto(created = null, removeAfterSave = previous)
            }
            ProfileDisplayType.PHOTO -> {
                val jpeg = profile.photoJpeg
                if (jpeg == null) {
                    if (pet.photoKey.isNullOrBlank()) {
                        throw IllegalArgumentException("사진을 선택하세요.")
                    }
                    pet.profileDisplayType = ProfileDisplayType.PHOTO
                    pet.profileEmoji = null
                    return StagedPhoto(created = null, removeAfterSave = null)
                }
                val key = photoStore.save(jpeg)
                pet.photoKey = key
                pet.profileDisplayType = ProfileDisplayType.PHOTO
                pet.profileEmoji = null
                return StagedPhoto(created = key, removeAfterSave = previous)
            }
        }
    }

    private fun saveProfile(pet: Pet, staged: StagedPhoto): Pet {
        try {
            val saved = petRepository.save(pet)
            val obsolete = staged.removeAfterSave
            if (obsolete != null && obsolete != saved.photoKey) {
                photoStore.delete(obsolete)
            }
            return saved
        } catch (e: RuntimeException) {
            staged.created?.let { photoStore.delete(it) }
            throw e
        }
    }

    private data class StagedPhoto(val created: String?, val removeAfterSave: String?)

    private fun deletePetGraph(petId: Long) {
        healthRecordRepository.findByPetIdOrderByRecordDateDesc(petId)
            .forEach { healthRecordRepository.delete(it) }
        taskRepository.findByPetIdOrderByIdDesc(petId).forEach { task ->
            task.id?.let { occurrenceRepository.deleteByTaskId(it) }
            taskRepository.delete(task)
        }
        petRepository.deleteById(petId)
    }
}
