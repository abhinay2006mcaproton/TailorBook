package com.tailorbook.feature.customers.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Family Member entities.
 */
@Dao
interface FamilyMemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMembers(members: List<FamilyMemberEntity>)

    @Update
    suspend fun updateFamilyMember(member: FamilyMemberEntity)

    @Delete
    suspend fun deleteFamilyMember(member: FamilyMemberEntity)

    @Query("DELETE FROM family_members WHERE id = :memberId")
    suspend fun deleteById(memberId: String)

    @Query("SELECT * FROM family_members WHERE customerId = :customerId ORDER BY name COLLATE NOCASE ASC")
    fun getFamilyMembersForCustomer(customerId: String): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE id = :memberId")
    suspend fun getFamilyMemberById(memberId: String): FamilyMemberEntity?
}
