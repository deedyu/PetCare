package com.petcare.petcare

import com.petcare.petcare.entity.User
import com.petcare.petcare.service.PetService
import com.petcare.petcare.service.UserService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class MyPageController(
    private val userService: UserService,
    private val petService: PetService
) {
    @GetMapping("/mypage")
    fun myPage(model: Model, session: HttpSession): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }

        val currentUser = userService.getUserById(user.id!!)
        val pets = petService.getPetsByUser(user.id!!)

        model.addAttribute("user", currentUser)
        model.addAttribute("pets", pets)

        return "mypage"
    }

    @PostMapping("/mypage/update")
    fun updateUser(
        @RequestParam username: String,
        @RequestParam(required = false, defaultValue = "") password: String,
        @RequestParam name: String,
        @RequestParam(required = false) email: String?,
        session: HttpSession,
        model: Model
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }

        try {
            val updatedUser = userService.updateUser(user.id!!, username, password, name, email)
            session.setAttribute("user", updatedUser)
        } catch (e: IllegalArgumentException) {
            model.addAttribute("error", e.message)
            return myPage(model, session)
        }

        model.addAttribute("success", "정보가 수정되었습니다.")
        return myPage(model, session)
    }

    @PostMapping("/mypage/pets/{petId}/update")
    fun updatePet(
        @PathVariable petId: Long,
        @RequestParam name: String,
        @RequestParam type: String,
        @RequestParam age: Int,
        session: HttpSession,
        model: Model
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }

        try {
            petService.updatePet(user.id!!, petId, name, type, age)
            model.addAttribute("success", "반려동물 정보가 수정되었습니다.")
        } catch (e: IllegalArgumentException) {
            val denied = e.message?.startsWith("unauthorized") == true || e.message?.contains("not found") == true
            model.addAttribute(
                "error",
                if (denied) "권한이 없거나 반려동물을 찾을 수 없습니다." else e.message
            )
        }
        return myPage(model, session)
    }

    @PostMapping("/mypage/pets/{petId}/delete")
    fun deletePet(
        @PathVariable petId: Long,
        session: HttpSession,
        model: Model
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }

        try {
            petService.deletePet(user.id!!, petId)
            model.addAttribute("success", "반려동물이 삭제되었습니다.")
        } catch (e: IllegalArgumentException) {
            model.addAttribute("error", "권한이 없거나 반려동물을 찾을 수 없습니다.")
        }
        return myPage(model, session)
    }

    @PostMapping("/mypage/delete")
    fun deleteUser(
        @RequestParam confirmText: String,
        session: HttpSession,
        model: Model
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }

        if (confirmText != "DELETE") {
            model.addAttribute("deleteError", "DELETE를 정확히 입력해주세요.")
            return myPage(model, session)
        }

        try {
            userService.deleteUser(user.id!!)
        } catch (e: IllegalArgumentException) {
            model.addAttribute("deleteError", e.message)
            return myPage(model, session)
        }
        session.invalidate()

        return "redirect:/login"
    }
}

