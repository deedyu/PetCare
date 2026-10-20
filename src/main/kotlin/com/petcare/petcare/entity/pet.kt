package com.petcare.petcare.entity

import com.petcare.petcare.text.Graphemes
import jakarta.persistence.*

@Entity
@Table(name = "pet")
class Pet(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Version
    @Column(nullable = false)
    var version: Long = 0,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false)
    var type: String = "",

    @Column(nullable = false)
    var age: Int = 0,

    // 예전 표시 글자. 새 프로필은 아래 세 칸을 쓰고, 이 값은 지우지 않는다.
    @Column(name = "image_url")
    var imageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_display_type", length = 16)
    var profileDisplayType: ProfileDisplayType? = ProfileDisplayType.INITIAL,

    @Column(name = "profile_emoji", length = 64)
    var profileEmoji: String? = null,

    // 저장 폴더 안의 UUID 파일 이름만 둔다. 사용자 경로를 넣지 않는다.
    @Column(name = "photo_key", length = 80)
    var photoKey: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    // 비어 있으면 등록한 사람만 본다. 값이 있으면 그 가족 구성원이 일정을 함께 본다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    var family: Family? = null
) {
    @get:Transient
    val resolvedDisplayType: ProfileDisplayType
        get() = profileDisplayType ?: ProfileDisplayType.INITIAL

    @get:Transient
    val initial: String
        get() = Graphemes.first(name)
}
