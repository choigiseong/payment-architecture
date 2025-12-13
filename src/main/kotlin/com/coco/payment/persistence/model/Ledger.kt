package com.coco.payment.persistence.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

//todo 컬럼 생각
@Entity
@Table(name = "ledger")
class Ledger(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var customerSeq: Long,
    @Column(nullable = false)
    var beforeBalance: Long,
    @Column(nullable = false)
    var balance: Long,
    @Column(nullable = false)
    var amount: Long
)
