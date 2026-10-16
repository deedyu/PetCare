package com.petcare.petcare.controller

import com.petcare.petcare.entity.User
import com.petcare.petcare.service.PetService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/pets")
class PetController(
    private val petService: PetService
) {

    @GetMapping
    fun petList(model: Model, session: HttpSession): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        model.addAttribute("pets", petService.getVisiblePets(user.id!!))
        model.addAttribute("user", user)
        return "pets"
    }

    @GetMapping("/new")
    fun newPet(model: Model, session: HttpSession): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }
        
        model.addAttribute("user", user)
        return "pet-register"
    }

    @PostMapping
    fun createPet(
        @RequestParam name: String,
        @RequestParam type: String,
        @RequestParam age: Int,
        @RequestParam(required = false) imageUrl: String?,
        session: HttpSession,
        model: Model
    ): String {
        val user = session.getAttribute("user") as? User
        if (user == null) {
            return "redirect:/login"
        }

        return try {
            petService.createPet(user, name, type, age, imageUrl)
            "redirect:/"
        } catch (e: IllegalArgumentException) {
            model.addAttribute("user", user)
            model.addAttribute("error", e.message)
            "pet-register"
        }
    }
}
