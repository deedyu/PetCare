package com.petcare.petcare.controller

import com.petcare.petcare.entity.Pet
import com.petcare.petcare.entity.ProfileDisplayType
import com.petcare.petcare.entity.User
import com.petcare.petcare.service.FamilyService
import com.petcare.petcare.service.PetPhotoProcessor
import com.petcare.petcare.service.PetPhotoStore
import com.petcare.petcare.service.PetProfileCommand
import com.petcare.petcare.service.PetService
import jakarta.servlet.http.HttpSession
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.concurrent.TimeUnit

@Controller
@RequestMapping("/pets")
class PetController(
    private val petService: PetService,
    private val familyService: FamilyService,
    private val photoStore: PetPhotoStore,
    private val photoProcessor: PetPhotoProcessor
) {

    @GetMapping
    fun petList(model: Model, session: HttpSession): String {
        val user = sessionUser(session) ?: return "redirect:/login"
        model.addAttribute("pets", petService.getVisiblePets(user.id!!))
        model.addAttribute("user", user)
        return "pets"
    }

    @GetMapping("/new")
    fun newPet(model: Model, session: HttpSession): String {
        val user = sessionUser(session) ?: return "redirect:/login"
        model.addAttribute("user", user)
        return "pet-register"
    }

    @GetMapping("/{petId}/edit")
    fun editPet(
        @PathVariable petId: Long,
        model: Model,
        session: HttpSession,
        redirect: RedirectAttributes
    ): String {
        val user = sessionUser(session) ?: return "redirect:/login"
        val pet = findOwned(user, petId, redirect) ?: return "redirect:/pets"
        model.addAttribute("user", user)
        model.addAttribute("pet", pet)
        return "pet-edit"
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createPet(
        @RequestParam name: String,
        @RequestParam type: String,
        @RequestParam age: Int,
        @RequestParam(required = false) displayType: String?,
        @RequestParam(required = false) profileEmoji: String?,
        @RequestParam(required = false) photo: MultipartFile?,
        session: HttpSession,
        model: Model
    ): String {
        val user = sessionUser(session) ?: return "redirect:/login"
        return try {
            petService.createPet(user, name, type, age, command(displayType, profileEmoji, photo))
            "redirect:/pets"
        } catch (e: IllegalArgumentException) {
            showForm(model, user, null, name, type, age, displayType, profileEmoji, e.message)
        }
    }

    @PostMapping("/{petId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updatePet(
        @PathVariable petId: Long,
        @RequestParam name: String,
        @RequestParam type: String,
        @RequestParam age: Int,
        @RequestParam(required = false) displayType: String?,
        @RequestParam(required = false) profileEmoji: String?,
        @RequestParam(required = false) photo: MultipartFile?,
        session: HttpSession,
        model: Model,
        redirect: RedirectAttributes
    ): String {
        val user = sessionUser(session) ?: return "redirect:/login"
        val pet = findOwned(user, petId, redirect) ?: return "redirect:/pets"
        return try {
            petService.updatePet(user.id!!, petId, name, type, age, command(displayType, profileEmoji, photo))
            redirect.addFlashAttribute("success", "반려동물 정보가 저장되었습니다.")
            "redirect:/pets"
        } catch (e: IllegalArgumentException) {
            if (denied(e)) {
                redirect.addFlashAttribute("error", "권한이 없거나 반려동물을 찾을 수 없습니다.")
                "redirect:/pets"
            } else {
                showForm(model, user, pet, name, type, age, displayType, profileEmoji, e.message)
            }
        }
    }

    @PostMapping("/{petId}/photo/delete")
    fun deletePhoto(
        @PathVariable petId: Long,
        session: HttpSession,
        redirect: RedirectAttributes
    ): String {
        val user = sessionUser(session) ?: return "redirect:/login"
        return try {
            petService.clearPhoto(user.id!!, petId)
            redirect.addFlashAttribute("success", "사진을 삭제했습니다.")
            "redirect:/pets/$petId/edit"
        } catch (_: IllegalArgumentException) {
            redirect.addFlashAttribute("error", "권한이 없거나 반려동물을 찾을 수 없습니다.")
            "redirect:/pets"
        }
    }

    @GetMapping("/{petId}/photo")
    fun photo(@PathVariable petId: Long, session: HttpSession): ResponseEntity<ByteArray> {
        val user = sessionUser(session) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (!familyService.canAccessPet(user.id!!, petId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val key = petService.getPet(petId).photoKey
        val bytes = photoStore.read(key) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate())
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Security-Policy", "default-src 'none'")
            .header("Content-Disposition", "inline; filename=\"pet.jpg\"")
            .body(bytes)
    }

    private fun command(displayType: String?, emoji: String?, photo: MultipartFile?): PetProfileCommand {
        val type = when (displayType?.trim()?.uppercase()) {
            "PHOTO" -> ProfileDisplayType.PHOTO
            "EMOJI" -> ProfileDisplayType.EMOJI
            "INITIAL", null, "" -> ProfileDisplayType.INITIAL
            else -> throw IllegalArgumentException("표시 방식을 선택하세요.")
        }
        val jpeg = if (photo == null || photo.isEmpty) null else photoProcessor.toJpeg(photo)
        return PetProfileCommand(type, emoji, jpeg)
    }

    private fun showForm(
        model: Model,
        user: User,
        pet: Pet?,
        name: String,
        type: String,
        age: Int,
        displayType: String?,
        emoji: String?,
        error: String?
    ): String {
        model.addAttribute("user", user)
        model.addAttribute("pet", pet)
        model.addAttribute("name", name)
        model.addAttribute("type", type)
        model.addAttribute("age", age)
        model.addAttribute("displayType", displayType ?: ProfileDisplayType.INITIAL.name)
        model.addAttribute("profileEmoji", emoji)
        model.addAttribute("error", error)
        return if (pet == null) "pet-register" else "pet-edit"
    }

    private fun findOwned(user: User, petId: Long, redirect: RedirectAttributes): Pet? {
        return try {
            val pet = petService.getPet(petId)
            if (pet.user?.id != user.id) {
                redirect.addFlashAttribute("error", "권한이 없거나 반려동물을 찾을 수 없습니다.")
                null
            } else {
                pet
            }
        } catch (_: IllegalArgumentException) {
            redirect.addFlashAttribute("error", "권한이 없거나 반려동물을 찾을 수 없습니다.")
            null
        }
    }

    private fun sessionUser(session: HttpSession): User? = session.getAttribute("user") as? User

    private fun denied(error: IllegalArgumentException): Boolean {
        val message = error.message.orEmpty()
        return message.startsWith("unauthorized") || message.contains("not found")
    }
}
