package com.petcare.petcare.entity

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

    @Column(name = "image_url")
    var imageUrl: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    // 비어 있으면 등록한 사람만 본다. 값이 있으면 그 가족 구성원이 일정을 함께 본다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    var family: Family? = null
)
