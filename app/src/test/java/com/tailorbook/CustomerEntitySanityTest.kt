package com.tailorbook

import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers
import com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for TASK-05-01: Customer & Family Member Room entities and relations.
 */
class CustomerEntitySanityTest {

    @Test
    fun testCustomerEntity_defaultValuesAndIntegrity() {
        val customer = CustomerEntity(
            name = "Rukmini Devi",
            primaryPhone = "9876543210"
        )

        assertNotNull(customer.id)
        assertTrue(customer.id.isNotEmpty())
        assertEquals("default_shop", customer.shopId)
        assertEquals("Rukmini Devi", customer.name)
        assertEquals("9876543210", customer.primaryPhone)
        assertEquals("FEMALE", customer.gender)
        assertFalse(customer.isArchived)
        assertTrue(customer.createdAt > 0)
        assertTrue(customer.updatedAt >= customer.createdAt)
    }

    @Test
    fun testFamilyMemberEntity_relationshipAndParentLink() {
        val parentCustomerId = UUID.randomUUID().toString()
        val member = FamilyMemberEntity(
            customerId = parentCustomerId,
            name = "Ananya",
            relationship = "Daughter",
            gender = "FEMALE"
        )

        assertNotNull(member.id)
        assertEquals(parentCustomerId, member.customerId)
        assertEquals("Ananya", member.name)
        assertEquals("Daughter", member.relationship)
        assertEquals("FEMALE", member.gender)
    }

    @Test
    fun testCustomerWithFamilyMembers_embedding() {
        val customer = CustomerEntity(name = "Kavitha Sharma", primaryPhone = "9848012345")
        val daughter = FamilyMemberEntity(customerId = customer.id, name = "Pooja", relationship = "Daughter")
        val son = FamilyMemberEntity(customerId = customer.id, name = "Rahul", relationship = "Son")

        val relation = CustomerWithFamilyMembers(
            customer = customer,
            familyMembers = listOf(daughter, son)
        )

        assertEquals("Kavitha Sharma", relation.customer.name)
        assertEquals(2, relation.familyMembers.size)
        assertEquals("Pooja", relation.familyMembers[0].name)
        assertEquals("Rahul", relation.familyMembers[1].name)
    }

    @Test
    fun testCustomerPhoneValidation_tenDigits() {
        val phoneRegex = Regex("^[6-9]\\d{9}$")
        assertTrue(phoneRegex.matches("9876543210"))
        assertTrue(phoneRegex.matches("7890123456"))
        assertFalse(phoneRegex.matches("1234567890"))
        assertFalse(phoneRegex.matches("98765"))
    }
}
