package com.petcare.petcare

import com.petcare.petcare.entity.FamilyRole
import com.petcare.petcare.entity.User
import com.petcare.petcare.service.FamilyService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
class FamilyController(
    private val familyService: FamilyService
) {
    @GetMapping("/family")
    fun list(model: Model, session: HttpSession): String {
        val user = member(session) ?: return "redirect:/login"
        model.addAttribute("user", user)
        model.addAttribute("families", familyService.myFamilies(user.id!!))
        return "family"
    }

    @PostMapping("/family")
    fun create(
        @RequestParam name: String,
        @RequestParam(required = false) description: String?,
        session: HttpSession,
        redirect: RedirectAttributes
    ): String {
        val user = member(session) ?: return "redirect:/login"
        return try {
            val family = familyService.create(user.id!!, name, description)
            "redirect:/family/${family.id}"
        } catch (e: IllegalArgumentException) {
            redirect.addFlashAttribute("error", e.message)
            "redirect:/family"
        }
    }

    @PostMapping("/family/join")
    fun join(
        @RequestParam code: String,
        session: HttpSession,
        redirect: RedirectAttributes
    ): String {
        val user = member(session) ?: return "redirect:/login"
        return try {
            val family = familyService.join(user.id!!, code)
            "redirect:/family/${family.id}"
        } catch (e: IllegalArgumentException) {
            redirect.addFlashAttribute("error", e.message)
            "redirect:/family"
        }
    }

    @GetMapping("/family/{id}")
    fun detail(@PathVariable id: Long, model: Model, session: HttpSession, redirect: RedirectAttributes): String {
        val user = member(session) ?: return "redirect:/login"
        return try {
            val membership = familyService.requireMember(user.id!!, id)
            val owner = membership.role == FamilyRole.OWNER
            model.addAttribute("user", user)
            model.addAttribute("family", membership.family)
            model.addAttribute("owner", owner)
            model.addAttribute("members", familyService.membersOf(id))
            model.addAttribute("sharedPets", familyService.sharedPets(id))
            model.addAttribute("shareablePets", familyService.shareablePets(user.id!!))
            model.addAttribute("invites", if (owner) familyService.openInvites(id) else emptyList())
            model.addAttribute("activities", familyService.recentActivity(id))
            "family-detail"
        } catch (e: IllegalArgumentException) {
            redirect.addFlashAttribute("error", e.message)
            "redirect:/family"
        }
    }

    @PostMapping("/family/{id}/invite")
    fun invite(@PathVariable id: Long, session: HttpSession, redirect: RedirectAttributes): String {
        val user = member(session) ?: return "redirect:/login"
        return act(id, redirect) { familyService.invite(user.id!!, id) }
    }

    @PostMapping("/family/{id}/leave")
    fun leave(@PathVariable id: Long, session: HttpSession, redirect: RedirectAttributes): String {
        val user = member(session) ?: return "redirect:/login"
        return try {
            familyService.leave(user.id!!, id)
            "redirect:/family"
        } catch (e: IllegalArgumentException) {
            redirect.addFlashAttribute("error", e.message)
            "redirect:/family/$id"
        }
    }

    @PostMapping("/family/{id}/delete")
    fun delete(@PathVariable id: Long, session: HttpSession, redirect: RedirectAttributes): String {
        val user = member(session) ?: return "redirect:/login"
        return try {
            familyService.deleteFamily(user.id!!, id)
            "redirect:/family"
        } catch (e: IllegalArgumentException) {
            redirect.addFlashAttribute("error", e.message)
            "redirect:/family/$id"
        }
    }

    @PostMapping("/family/{id}/members/{memberId}/remove")
    fun remove(
        @PathVariable id: Long,
        @PathVariable memberId: Long,
        session: HttpSession,
        redirect: RedirectAttributes
    ): String {
        val user = member(session) ?: return "redirect:/login"
        return act(id, redirect) { familyService.removeMember(user.id!!, id, memberId) }
    }

    @PostMapping("/family/{id}/share")
    fun share(
        @PathVariable id: Long,
        @RequestParam petId: Long,
        session: HttpSession,
        redirect: RedirectAttributes
    ): String {
        val user = member(session) ?: return "redirect:/login"
        return act(id, redirect) { familyService.sharePet(user.id!!, id, petId) }
    }

    @PostMapping("/family/{id}/unshare")
    fun unshare(
        @PathVariable id: Long,
        @RequestParam petId: Long,
        session: HttpSession,
        redirect: RedirectAttributes
    ): String {
        val user = member(session) ?: return "redirect:/login"
        return act(id, redirect) { familyService.unsharePet(user.id!!, id, petId) }
    }

    private fun member(session: HttpSession): User? = session.getAttribute("user") as? User

    private fun act(familyId: Long, redirect: RedirectAttributes, action: () -> Any?): String {
        return try {
            action()
            "redirect:/family/$familyId"
        } catch (e: IllegalArgumentException) {
            redirect.addFlashAttribute("error", e.message)
            "redirect:/family/$familyId"
        }
    }
}
