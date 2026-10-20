package com.petcare.petcare.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID

/**
 * 사진 파일은 애플리케이션 디렉터리 밖에 둔다.
 * 로컬 기본값은 사용자 홈의 petcare-data 이고, Docker나 Kubernetes에서는 PETCARE_PHOTO_DIR 볼륨을 붙인다.
 * 데이터베이스에는 UUID 파일 이름만 저장한다.
 */
@Component
class PetPhotoStore(
    @Value("\${petcare.photos.directory}") directory: String
) {
    private val root: Path = Paths.get(directory).toAbsolutePath().normalize()

    init {
        Files.createDirectories(root)
    }

    fun save(jpeg: ByteArray): String {
        val key = UUID.randomUUID().toString() + ".jpg"
        val path = resolve(key)
        Files.write(path, jpeg, StandardOpenOption.CREATE_NEW)
        removeExecuteBit(path)
        return key
    }

    fun read(key: String?): ByteArray? {
        val path = try {
            resolve(key)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (!Files.isRegularFile(path)) return null
        return Files.readAllBytes(path)
    }

    fun delete(key: String?) {
        val path = try {
            resolve(key)
        } catch (_: IllegalArgumentException) {
            return
        }
        Files.deleteIfExists(path)
    }

    private fun resolve(key: String?): Path {
        if (key.isNullOrBlank() || !KEY.matches(key)) {
            throw IllegalArgumentException("잘못된 사진 식별자입니다.")
        }
        val path = root.resolve(key).normalize()
        if (!path.startsWith(root)) {
            throw IllegalArgumentException("잘못된 사진 식별자입니다.")
        }
        return path
    }

    private fun removeExecuteBit(path: Path) {
        val views = path.fileSystem.supportedFileAttributeViews()
        if (!views.contains("posix")) return
        val permissions = Files.getPosixFilePermissions(path).toMutableSet()
        permissions.remove(PosixFilePermission.OWNER_EXECUTE)
        permissions.remove(PosixFilePermission.GROUP_EXECUTE)
        permissions.remove(PosixFilePermission.OTHERS_EXECUTE)
        permissions.remove(PosixFilePermission.GROUP_READ)
        permissions.remove(PosixFilePermission.GROUP_WRITE)
        permissions.remove(PosixFilePermission.OTHERS_READ)
        permissions.remove(PosixFilePermission.OTHERS_WRITE)
        Files.setPosixFilePermissions(path, permissions)
    }

    companion object {
        private val KEY = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.jpg")
    }
}
