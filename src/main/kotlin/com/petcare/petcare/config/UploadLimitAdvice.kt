package com.petcare.petcare.config

import com.petcare.petcare.service.PetPhotoProcessor
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@ControllerAdvice
class UploadLimitAdvice {
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun tooLarge(request: HttpServletRequest, redirect: RedirectAttributes): Any {
        val message = PetPhotoProcessor.TOO_LARGE
        val path = request.requestURI.orEmpty()
        if (path.startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message)
        }
        redirect.addFlashAttribute("error", message)
        val edit = Regex("^/pets/(\\d+)$").matchEntire(path)
        return if (edit != null) "redirect:/pets/${edit.groupValues[1]}/edit" else "redirect:/pets/new"
    }
}
