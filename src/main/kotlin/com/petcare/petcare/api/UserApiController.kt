package com.petcare.petcare.api

import com.petcare.petcare.dto.UserDto
import com.petcare.petcare.entity.Role
import com.petcare.petcare.entity.User
import com.petcare.petcare.security.CurrentUser
import com.petcare.petcare.security.PasswordRules
import com.petcare.petcare.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserApiController(
    private val userService: UserService,
    private val currentUser: CurrentUser
) {

    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: Long): ResponseEntity<*> {
        if (!canRead(userId)) return forbiddenOrUnauthorized()
        return ResponseEntity.ok(userService.getUserById(userId).toDto())
    }

    @GetMapping("/username/{username}")
    fun getUserByUsername(@PathVariable username: String): ResponseEntity<*> {
        val user = userService.findByUsername(username) ?: return ResponseEntity.notFound().build<Void>()
        val id = user.id ?: return ResponseEntity.notFound().build<Void>()
        if (!canRead(id)) return forbiddenOrUnauthorized()
        return ResponseEntity.ok(user.toDto())
    }

    @PostMapping
    fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<*> {
        val me = currentUser.optional() ?: return unauthorized()
        if (me.effectiveRole() != Role.ADMIN) return forbidden()
        return try {
            PasswordRules.check(request.password)
            val user = userService.createUser(
                username = request.username,
                password = request.password,
                name = request.name,
                email = request.email
            )
            ResponseEntity.ok(user.toDto())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @PutMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: Long,
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<*> {
        if (!canRead(userId)) return forbiddenOrUnauthorized()
        return try {
            val user = userService.updateUser(
                userId = userId,
                username = request.username,
                password = request.password,
                name = request.name,
                email = request.email
            )
            ResponseEntity.ok(user.toDto())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: Long): ResponseEntity<*> {
        if (!canRead(userId)) return forbiddenOrUnauthorized()
        return try {
            userService.deleteUser(userId)
            ResponseEntity.noContent().build<Void>()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    private fun canRead(userId: Long): Boolean {
        val me = currentUser.optional() ?: return false
        return me.id == userId || me.effectiveRole() == Role.ADMIN
    }

    private fun forbiddenOrUnauthorized(): ResponseEntity<Void> {
        if (currentUser.optional() == null) return unauthorized()
        return forbidden()
    }

    private fun unauthorized() = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()

    private fun forbidden() = ResponseEntity.status(HttpStatus.FORBIDDEN).build<Void>()

    private fun User.toDto() = UserDto(id = id, username = username, name = name)

    data class CreateUserRequest(
        val username: String,
        val password: String,
        val name: String,
        val email: String? = null
    )

    data class UpdateUserRequest(
        val username: String,
        val password: String = "",
        val name: String,
        val email: String? = null
    )
}
