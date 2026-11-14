package com.tailorbook.feature.customers.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Customer entities.
 */
@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET isArchived = 1, updatedAt = :updatedAt WHERE id = :customerId")
    suspend fun softDeleteCustomer(customerId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomerById(customerId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE (:shopId = '' OR shopId = :shopId OR shopId = '' OR :shopId = 'default_shop' OR :shopId = 'shop-001' OR shopId = 'default_shop' OR shopId = 'shop-001' OR (SELECT COUNT(*) FROM customers WHERE shopId = :shopId AND isArchived = 0) = 0) AND isArchived = 0 ORDER BY name COLLATE NOCASE ASC")
    fun getActiveCustomers(shopId: String = "default_shop"): Flow<List<CustomerEntity>>

    @Query("""
        SELECT * FROM customers 
        WHERE (:shopId = '' OR shopId = :shopId OR shopId = '' OR :shopId = 'default_shop' OR :shopId = 'shop-001' OR shopId = 'default_shop' OR shopId = 'shop-001' OR (SELECT COUNT(*) FROM customers WHERE shopId = :shopId AND isArchived = 0) = 0) AND isArchived = 0 
        AND (name LIKE '%' || :query || '%' OR primaryPhone LIKE '%' || :query || '%')
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun searchCustomers(query: String, shopId: String = "default_shop"): Flow<List<CustomerEntity>>

    @Transaction
    @Query("SELECT * FROM customers WHERE id = :customerId")
    fun getCustomerWithFamilyMembers(customerId: String): Flow<CustomerWithFamilyMembers?>

    @Transaction
    @Query("SELECT * FROM customers WHERE (:shopId = '' OR shopId = :shopId OR shopId = '' OR :shopId = 'default_shop' OR :shopId = 'shop-001' OR shopId = 'default_shop' OR shopId = 'shop-001' OR (SELECT COUNT(*) FROM customers WHERE shopId = :shopId AND isArchived = 0) = 0) AND isArchived = 0 ORDER BY createdAt DESC")
    fun getAllCustomersWithFamilyMembers(shopId: String = ""): Flow<List<CustomerWithFamilyMembers>>

    @Query("SELECT COUNT(*) FROM customers WHERE (:shopId = '' OR shopId = :shopId OR shopId = '' OR :shopId = 'default_shop' OR :shopId = 'shop-001' OR shopId = 'default_shop' OR shopId = 'shop-001' OR (SELECT COUNT(*) FROM customers WHERE shopId = :shopId AND isArchived = 0) = 0) AND isArchived = 0")
    fun getActiveCustomerCount(shopId: String = "default_shop"): Flow<Int>
}
