package com.petcare.petcare.service

import com.petcare.petcare.entity.*
import com.petcare.petcare.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class FamilyService(
    private val familyRepository: FamilyRepository,
    private val memberRepository: FamilyMemberRepository,
    private val invitationRepository: FamilyInvitationRepository,
    private val activityRepository: ActivityLogRepository,
    private val petRepository: PetRepository,
    private val taskRepository: TaskRepository,
    private val occurrenceRepository: TaskOccurrenceRepository,
    private val healthRecordRepository: HealthRecordRepository,
    private val userRepository: UserRepository
) {
    @Transactional(readOnly = true)
    fun myFamilies(userId: Long): List<Family> {
        return memberRepository.findByUserId(userId).mapNotNull { it.family }
    }

    @Transactional(readOnly = true)
    fun requireMember(userId: Long, familyId: Long): FamilyMember {
        return memberRepository.findByFamilyIdAndUserId(familyId, userId)
            ?: throw IllegalArgumentException("가족 구성원만 볼 수 있습니다.")
    }

    @Transactional
    fun create(userId: Long, name: String, description: String?): Family {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("가족 이름을 입력해 주세요.")
        }
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        val family = familyRepository.save(
            Family(
                name = trimmed.take(80),
                description = description?.trim()?.ifBlank { null }?.take(500),
                owner = user
            )
        )
        memberRepository.save(FamilyMember(family = family, user = user, role = FamilyRole.OWNER))
        addActivity(family, user, "CREATE", "${user.name}님이 가족 ${family.name}을 만들었습니다.")
        return family
    }

    @Transactional
    fun invite(userId: Long, familyId: Long): FamilyInvitation {
        val owner = requireOwner(userId, familyId)
        val family = owner.family ?: throw IllegalArgumentException("가족을 찾을 수 없습니다.")
        val code = uniqueCode()
        val invitation = invitationRepository.save(
            FamilyInvitation(
                family = family,
                code = code,
                createdBy = owner.user,
                expiresAt = LocalDateTime.now().plusDays(7)
            )
        )
        addActivity(family, owner.user, "INVITE", "초대 코드가 발급되었습니다.")
        return invitation
    }

    @Transactional(readOnly = true)
    fun openInvites(familyId: Long): List<FamilyInvitation> {
        val now = LocalDateTime.now()
        return invitationRepository.findByFamilyIdAndStatus(familyId, InviteStatus.OPEN)
            .filter { it.expiresAt.isAfter(now) }
    }

    @Transactional
    fun join(userId: Long, rawCode: String): Family {
        val code = rawCode.trim().uppercase()
        if (code.isEmpty()) {
            throw IllegalArgumentException("초대 코드를 입력해 주세요.")
        }
        val invitation = invitationRepository.findByCode(code)
            ?: throw IllegalArgumentException("초대 코드를 찾을 수 없습니다.")
        if (invitation.status != InviteStatus.OPEN || !invitation.expiresAt.isAfter(LocalDateTime.now())) {
            throw IllegalArgumentException("만료되었거나 이미 사용된 코드입니다.")
        }
        val family = invitation.family ?: throw IllegalArgumentException("가족을 찾을 수 없습니다.")
        val familyId = family.id ?: throw IllegalArgumentException("가족을 찾을 수 없습니다.")
        if (memberRepository.existsByFamilyIdAndUserId(familyId, userId)) {
            throw IllegalArgumentException("이미 이 가족에 속해 있습니다.")
        }
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다.") }
        memberRepository.save(FamilyMember(family = family, user = user, role = FamilyRole.MEMBER))
        invitation.status = InviteStatus.USED
        invitation.acceptedBy = user
        invitationRepository.save(invitation)
        addActivity(family, user, "JOIN", "${user.name}님이 가족에 참여했습니다.")
        return family
    }

    @Transactional
    fun leave(userId: Long, familyId: Long) {
        val member = requireMember(userId, familyId)
        if (member.role == FamilyRole.OWNER) {
            throw IllegalArgumentException("소유자는 가족을 삭제해야 합니다.")
        }
        val family = member.family
        val user = member.user
        memberRepository.delete(member)
        taskRepository.clearAssigneeInFamily(familyId, userId)
        if (family != null && user != null) {
            addActivity(family, user, "LEAVE", "${user.name}님이 가족에서 나갔습니다.")
        }
    }

    @Transactional
    fun removeMember(actorId: Long, familyId: Long, memberUserId: Long) {
        requireOwner(actorId, familyId)
        if (actorId == memberUserId) {
            throw IllegalArgumentException("소유자는 구성원 목록에서 뺄 수 없습니다.")
        }
        val member = memberRepository.findByFamilyIdAndUserId(familyId, memberUserId)
            ?: throw IllegalArgumentException("구성원을 찾을 수 없습니다.")
        val family = member.family
        val actor = userRepository.findById(actorId).orElse(null)
        val name = member.user?.name ?: "구성원"
        memberRepository.delete(member)
        taskRepository.clearAssigneeInFamily(familyId, memberUserId)
        if (family != null) {
            addActivity(family, actor, "REMOVE", "${name}님을 가족에서 내보냈습니다.")
        }
    }

    @Transactional
    fun deleteFamily(userId: Long, familyId: Long) {
        requireOwner(userId, familyId)
        deleteFamilyInternal(familyId)
    }

    @Transactional
    fun sharePet(userId: Long, familyId: Long, petId: Long) {
        requireMember(userId, familyId)
        val pet = petRepository.findById(petId)
            .orElseThrow { IllegalArgumentException("반려동물을 찾을 수 없습니다.") }
        if (pet.user?.id != userId) {
            throw IllegalArgumentException("자신이 등록한 반려동물만 공유할 수 있습니다.")
        }
        val family = familyRepository.findById(familyId)
            .orElseThrow { IllegalArgumentException("가족을 찾을 수 없습니다.") }
        pet.family = family
        petRepository.save(pet)
        val user = userRepository.findById(userId).orElse(null)
        addActivity(family, user, "SHARE", "${pet.name} 반려동물을 가족에 공유했습니다.")
    }

    @Transactional
    fun unsharePet(userId: Long, familyId: Long, petId: Long) {
        val member = requireMember(userId, familyId)
        val pet = petRepository.findById(petId)
            .orElseThrow { IllegalArgumentException("반려동물을 찾을 수 없습니다.") }
        if (pet.family?.id != familyId) {
            throw IllegalArgumentException("이 가족에 공유된 반려동물이 아닙니다.")
        }
        val isOwner = member.role == FamilyRole.OWNER
        if (pet.user?.id != userId && !isOwner) {
            throw IllegalArgumentException("반려동물 소유자 또는 가족 소유자만 공유를 해제할 수 있습니다.")
        }
        pet.id?.let { taskRepository.clearAssigneesOfPet(it) }
        pet.family = null
        petRepository.save(pet)
        val family = member.family
        val user = member.user
        if (family != null) {
            addActivity(family, user, "UNSHARE", "${pet.name} 공유를 해제했습니다.")
        }
    }

    @Transactional(readOnly = true)
    fun sharedPets(familyId: Long) = petRepository.findByFamilyIdOrderByIdDesc(familyId)

    @Transactional(readOnly = true)
    fun shareablePets(userId: Long) = petRepository.findByUserIdAndFamilyIsNullOrderByIdDesc(userId)

    @Transactional(readOnly = true)
    fun membersOf(familyId: Long): List<FamilyMemberView> {
        return memberRepository.findByFamilyId(familyId).map {
            FamilyMemberView(
                userId = it.user?.id ?: 0,
                name = it.user?.name ?: "",
                username = it.user?.username ?: "",
                role = it.role
            )
        }
    }

    @Transactional(readOnly = true)
    fun membersForPet(petId: Long): List<FamilyMemberView> {
        val pet = petRepository.findById(petId).orElse(null) ?: return emptyList()
        val familyId = pet.family?.id ?: return emptyList()
        return membersOf(familyId)
    }

    @Transactional(readOnly = true)
    fun recentActivity(familyId: Long) = activityRepository.findByFamilyIdOrderByIdDesc(familyId).take(30)

    fun canAccessPet(userId: Long, petId: Long): Boolean {
        return petRepository.countVisible(petId, userId) > 0
    }

    fun isMember(familyId: Long, userId: Long): Boolean {
        return memberRepository.existsByFamilyIdAndUserId(familyId, userId)
    }

    @Transactional
    fun addActivity(family: Family, actor: User?, action: String, summary: String) {
        activityRepository.save(
            ActivityLog(
                family = family,
                actor = actor,
                action = action,
                summary = summary.take(200)
            )
        )
    }

    /**
     * 회원 탈퇴 전에 가족 소속과 담당자 참조를 정리한다.
     * 다른 구성원이 남은 가족의 소유자는 탈퇴할 수 없다.
     */
    @Transactional
    fun releaseUser(userId: Long) {
        val owned = memberRepository.findByUserIdAndRole(userId, FamilyRole.OWNER)
        for (membership in owned) {
            val familyId = membership.family?.id ?: continue
            if (memberRepository.countByFamilyId(familyId) > 1) {
                throw IllegalArgumentException("다른 구성원이 있는 가족은 삭제한 뒤 탈퇴할 수 있습니다.")
            }
            deleteFamilyInternal(familyId)
        }
        memberRepository.deleteByUserId(userId)
        taskRepository.clearAssignee(userId)
        occurrenceRepository.clearCompletedBy(userId)
        invitationRepository.clearAcceptedBy(userId)
        invitationRepository.clearCreatedBy(userId)
        activityRepository.clearActor(userId)
        healthRecordRepository.clearAuthor(userId)
    }

    private fun requireOwner(userId: Long, familyId: Long): FamilyMember {
        val member = requireMember(userId, familyId)
        if (member.role != FamilyRole.OWNER) {
            throw IllegalArgumentException("가족 소유자만 할 수 있습니다.")
        }
        return member
    }

    private fun deleteFamilyInternal(familyId: Long) {
        taskRepository.clearAssigneesOfFamily(familyId)
        petRepository.clearFamily(familyId)
        invitationRepository.deleteByFamilyId(familyId)
        activityRepository.deleteByFamilyId(familyId)
        memberRepository.deleteByFamilyId(familyId)
        familyRepository.deleteById(familyId)
    }

    private fun uniqueCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        repeat(5) {
            val code = (1..8).map { alphabet.random() }.joinToString("")
            if (invitationRepository.findByCode(code) == null) {
                return code
            }
        }
        throw IllegalStateException("초대 코드를 만들지 못했습니다.")
    }
}
