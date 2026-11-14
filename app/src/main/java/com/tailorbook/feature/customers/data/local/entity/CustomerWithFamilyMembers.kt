package com.tailorbook.feature.customers.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 1-to-Many relational representation of a Customer and their linked Family Members (REQ-FAM-01).
 */
data class CustomerWithFamilyMembers(
    @Embedded
    val customer: CustomerEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val familyMembers: List<FamilyMemberEntity> = emptyList()
)
