package com.coco.payment.persistence.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "ledger")
class Ledger(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var customerSeq: String = "",
    @Column(nullable = false)
    var beforeBalance: Long = 0,
    @Column(nullable = false)
    var balance: Long = 0,
    @Column(nullable = false)
    var amount: Long = 0
)
