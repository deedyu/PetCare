package com.petcare.petcare

import com.petcare.petcare.entity.ProfileDisplayType
import com.petcare.petcare.repository.PetRepository
import com.petcare.petcare.repository.UserRepository
import com.petcare.petcare.service.PetPhotoProcessor
import com.petcare.petcare.service.PetPhotoStore
import com.petcare.petcare.service.PetProfileCommand
import com.petcare.petcare.service.PetService
import com.petcare.petcare.service.UserService
import com.petcare.petcare.text.EmojiRules
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
class PetProfileFlowTest {
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var userService: UserService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var petRepository: PetRepository
    @Autowired lateinit var petService: PetService
    @Autowired lateinit var photoStore: PetPhotoStore
    @Autowired lateinit var photoProcessor: PetPhotoProcessor

    @Test
    fun registerPageOffersThreeModesAndFreeEmojiField() {
        val session = login("profile-form")
        mockMvc.perform(get("/pets/new").session(session))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("사진")))
            .andExpect(content().string(containsString("이모지")))
            .andExpect(content().string(containsString("이름 첫 글자")))
            .andExpect(content().string(containsString("placeholder=\"🐶\"")))
            .andExpect(content().string(containsString("value=\"INITIAL\"")))
            .andExpect(content().string(containsString("checked=\"checked\"")))
            .andExpect(content().string(not(containsString("value=\"강\""))))
            .andExpect(content().string(not(containsString("사진 파일 업로드는 없습니다"))))
        mockMvc.perform(get("/css/app.css"))
            .andExpect(content().string(containsString("object-fit: cover")))
    }

    @Test
    fun initialIsTheDefaultAndLegacyMarkIsNotShown() {
        val session = login("profile-initial")
        mockMvc.perform(
            multipart("/pets").session(session).with(csrf())
                .param("name", "나비")
                .param("type", "고양이")
                .param("age", "3")
                .param("displayType", "INITIAL")
        ).andExpect(redirectedUrl("/pets"))
        val pet = onlyPet("profile-initial")
        assertEquals(ProfileDisplayType.INITIAL, pet.profileDisplayType)
        assertNull(pet.profileEmoji)
        assertNull(pet.photoKey)

        pet.profileDisplayType = null
        pet.imageUrl = "ZXMARK"
        petRepository.save(pet)
        mockMvc.perform(get("/pets").session(session))
            .andExpect(content().string(containsString("나비")))
            .andExpect(content().string(not(containsString("ZXMARK"))))
    }

    @Test
    fun emojiRoundTripKeepsCompositeCharacters() {
        assertFailsWith<IllegalArgumentException> { EmojiRules.requireSingle("🐶🐶") }
        assertFailsWith<IllegalArgumentException> { EmojiRules.requireSingle("<script>alert(1)</script>") }
        assertFailsWith<IllegalArgumentException> { EmojiRules.requireSingle("가") }
        assertFailsWith<IllegalArgumentException> { EmojiRules.requireSingle("1") }
        assertEquals("👍🏽", EmojiRules.requireSingle(" 👍🏽 "))
        assertEquals("👨‍👩‍👧", EmojiRules.requireSingle("👨‍👩‍👧"))
        assertEquals("🇰🇷", EmojiRules.requireSingle("🇰🇷"))
        assertEquals("1️⃣", EmojiRules.requireSingle("1️⃣"))

        val session = login("profile-emoji")
        mockMvc.perform(
            multipart("/pets").session(session).with(csrf())
                .param("name", "이모")
                .param("type", "고양이")
                .param("age", "1")
                .param("displayType", "EMOJI")
                .param("profileEmoji", "👍🏽")
        ).andExpect(redirectedUrl("/pets"))
        mockMvc.perform(get("/pets").session(session))
            .andExpect(content().string(containsString("👍🏽")))
        mockMvc.perform(get("/").session(session))
            .andExpect(content().string(containsString("👍🏽")))
        assertEquals("👍🏽", onlyPet("profile-emoji").profileEmoji)

        val family = petService.createPet(
            userRepository.findByUsername("profile-emoji")!!,
            "가족",
            "개",
            1,
            PetProfileCommand(ProfileDisplayType.EMOJI, "👨‍👩‍👧")
        )
        assertEquals("👨‍👩‍👧", petRepository.findById(family.id!!).get().profileEmoji)
    }

    @Test
    fun invalidEmojiIsRejectedAndEscaped() {
        val session = login("profile-xss")
        mockMvc.perform(
            multipart("/pets").session(session).with(csrf())
                .param("name", "위험")
                .param("type", "개")
                .param("age", "1")
                .param("displayType", "EMOJI")
                .param("profileEmoji", "<img src=x onerror=alert(1)>")
        ).andExpect(status().isOk)
            .andExpect(content().string(containsString("이모지")))
            .andExpect(content().string(not(containsString("<img src=x"))))
        assertTrue(petRepository.findByUserIdOrderByIdDesc(userRepository.findByUsername("profile-xss")!!.id!!).isEmpty())
    }

    @Test
    fun photoUploadRejectsBadBytesAndServesReencodedJpeg() {
        assertFailsWith<IllegalArgumentException> {
            photoProcessor.reencode("not an image".toByteArray())
        }
        assertFailsWith<IllegalArgumentException> {
            photoProcessor.reencode(image("gif"))
        }
        assertFailsWith<IllegalArgumentException> {
            photoProcessor.reencode(ByteArray(PetPhotoProcessor.MAX_BYTES + 1))
        }
        val png = photoProcessor.reencode(image("png"))
        assertTrue(png[0] == 0xFF.toByte() && png[1] == 0xD8.toByte())
        val webp = photoProcessor.reencode(TINY_WEBP)
        assertTrue(webp[0] == 0xFF.toByte() && webp[1] == 0xD8.toByte())
        assertNull(photoStore.read("../etc/passwd"))
        assertNull(photoStore.read("not-a-key.jpg"))

        val session = login("profile-photo")
        mockMvc.perform(
            multipart("/pets").session(session).with(csrf())
                .file(MockMultipartFile("photo", "note.jpg", "image/jpeg", "hello".toByteArray()))
                .param("name", "가짜")
                .param("type", "개")
                .param("age", "1")
                .param("displayType", "PHOTO")
        ).andExpect(status().isOk)
            .andExpect(content().string(containsString("JPG, PNG, WebP")))
        assertTrue(petsOf("profile-photo").isEmpty())

        val oversized = mockMvc.perform(
            multipart("/pets").session(session).with(csrf())
                .file(MockMultipartFile("photo", "big.jpg", "image/jpeg", ByteArray(PetPhotoProcessor.MAX_BYTES + 1)))
                .param("name", "큰사진")
                .param("type", "개")
                .param("age", "1")
                .param("displayType", "PHOTO")
        ).andReturn()
        val sawLimit = oversized.response.contentAsString.contains(PetPhotoProcessor.TOO_LARGE) ||
            oversized.response.redirectedUrl == "/pets/new"
        assertTrue(sawLimit, "status=${oversized.response.status}")

        mockMvc.perform(
            multipart("/pets").session(session).with(csrf())
                .file(MockMultipartFile("photo", "pet.png", "image/png", image("png")))
                .param("name", "사진이")
                .param("type", "강아지")
                .param("age", "2")
                .param("displayType", "PHOTO")
        ).andExpect(redirectedUrl("/pets"))
        val created = onlyPet("profile-photo")
        val firstKey = created.photoKey!!
        val first = mockMvc.perform(get("/pets/${created.id}/photo").session(session))
            .andExpect(status().isOk)
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andReturn().response.contentAsByteArray
        assertTrue(first[0] == 0xFF.toByte() && first[1] == 0xD8.toByte())
        mockMvc.perform(get("/pets").session(session))
            .andExpect(content().string(containsString("/pets/${created.id}/photo")))

        mockMvc.perform(
            multipart("/pets/${created.id}").session(session).with(csrf())
                .file(MockMultipartFile("photo", "next.jpg", "image/jpeg", image("jpeg", Color.BLUE)))
                .param("name", "사진이")
                .param("type", "강아지")
                .param("age", "2")
                .param("displayType", "PHOTO")
        ).andExpect(redirectedUrl("/pets"))
        val changed = petRepository.findById(created.id!!).get()
        assertNotEquals(firstKey, changed.photoKey)
        assertNull(photoStore.read(firstKey))
        val second = mockMvc.perform(get("/pets/${created.id}/photo").session(session))
            .andReturn().response.contentAsByteArray
        assertTrue(!first.contentEquals(second))

        mockMvc.perform(post("/pets/${created.id}/photo/delete").session(session).with(csrf()))
            .andExpect(redirectedUrl("/pets/${created.id}/edit"))
        val cleared = petRepository.findById(created.id!!).get()
        assertEquals(ProfileDisplayType.INITIAL, cleared.profileDisplayType)
        assertNull(cleared.photoKey)
        mockMvc.perform(get("/pets/${created.id}/photo").session(session))
            .andExpect(status().isNotFound)
        assertNull(photoStore.read(changed.photoKey))
    }

    @Test
    fun otherUserCannotReadOrChangePhotoAndNameUpdateKeepsEmoji() {
        val owner = login("profile-owner")
        mockMvc.perform(
            multipart("/pets").session(owner).with(csrf())
                .file(MockMultipartFile("photo", "a.jpg", "image/jpeg", image("jpeg")))
                .param("name", "지키")
                .param("type", "개")
                .param("age", "4")
                .param("displayType", "PHOTO")
        ).andExpect(redirectedUrl("/pets"))
        val pet = onlyPet("profile-owner")
        val other = login("profile-other")
        mockMvc.perform(get("/pets/${pet.id}/photo").session(other))
            .andExpect(status().isForbidden)
        mockMvc.perform(get("/pets/${pet.id}/photo"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/login"))
        mockMvc.perform(
            multipart("/pets/${pet.id}").session(other).with(csrf())
                .param("name", "훔침")
                .param("type", "개")
                .param("age", "4")
                .param("displayType", "EMOJI")
                .param("profileEmoji", "🐱")
        ).andExpect(redirectedUrl("/pets"))
        mockMvc.perform(post("/pets/${pet.id}/photo/delete").session(other).with(csrf()))
            .andExpect(redirectedUrl("/pets"))
        val unchanged = petRepository.findById(pet.id!!).get()
        assertEquals("지키", unchanged.name)
        assertEquals(ProfileDisplayType.PHOTO, unchanged.profileDisplayType)
        assertEquals(pet.photoKey, unchanged.photoKey)
        mockMvc.perform(get("/pets/${pet.id}/photo").session(owner)).andExpect(status().isOk)

        val emojiUser = userRepository.findByUsername("profile-owner")!!
        val emojiPet = petService.createPet(
            emojiUser, "유지", "고양이", 1, PetProfileCommand(ProfileDisplayType.EMOJI, "🐶")
        )
        mockMvc.perform(
            post("/mypage/pets/${emojiPet.id}/update").session(owner).with(csrf())
                .param("name", "유지됨")
                .param("type", "고양이")
                .param("age", "2")
        )
        val kept = petRepository.findById(emojiPet.id!!).get()
        assertEquals("유지됨", kept.name)
        assertEquals("🐶", kept.profileEmoji)
        mockMvc.perform(get("/pets/${emojiPet.id}/edit").session(owner))
            .andExpect(content().string(containsString("🐶")))
    }

    @Test
    fun apiAcceptsEmojiAndRejectsPhotoWithoutAFile() {
        val session = login("profile-api")
        mockMvc.perform(
            post("/api/pets").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"제이","type":"개","age":1,"profileDisplayType":"EMOJI","profileEmoji":"🐶"}""")
        ).andExpect(status().isOk)
            .andExpect(content().string(containsString("EMOJI")))
            .andExpect(content().string(containsString("🐶")))
        mockMvc.perform(
            post("/api/pets").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"피","type":"개","age":1,"profileDisplayType":"PHOTO"}""")
        ).andExpect(status().isBadRequest)
        mockMvc.perform(
            multipart("/pets").session(session)
                .param("name", "토큰없음")
                .param("type", "개")
                .param("age", "1")
                .param("displayType", "INITIAL")
        ).andExpect(status().isForbidden)
    }

    private fun login(username: String): MockHttpSession {
        userService.createUser(username, "abcd1234", "프로필")
        val login = mockMvc.perform(
            post("/login").with(csrf())
                .param("username", username)
                .param("password", "abcd1234")
        ).andExpect(redirectedUrl("/")).andReturn()
        return login.request.getSession(false) as MockHttpSession
    }

    private fun petsOf(username: String) =
        petRepository.findByUserIdOrderByIdDesc(userRepository.findByUsername(username)!!.id!!)

    private fun onlyPet(username: String) = petsOf(username).single()

    private fun image(format: String, color: Color = Color.RED): ByteArray {
        val picture = BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB)
        val graphics = picture.createGraphics()
        graphics.color = color
        graphics.fillRect(0, 0, 8, 8)
        graphics.dispose()
        val output = ByteArrayOutputStream()
        ImageIO.write(picture, format, output)
        return output.toByteArray()
    }

    companion object {
        private val TINY_WEBP: ByteArray = Base64.getDecoder().decode(
            "UklGRiQAAABXRUJQVlA4IBgAAAAwAQCdASoBAAEAAwA0JaQAA3AA/vuUAAA="
        )
    }
}
