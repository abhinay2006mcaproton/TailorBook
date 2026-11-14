package com.tailorbook.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tailorbook.core.sync.local.SyncQueueDao
import com.tailorbook.core.sync.local.SyncQueueEntity
import com.tailorbook.feature.customers.data.local.dao.CustomerDao
import com.tailorbook.feature.customers.data.local.dao.FamilyMemberDao
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity
import com.tailorbook.feature.gallery.data.local.dao.DesignReferenceDao
import com.tailorbook.feature.gallery.data.local.entity.DesignReferenceEntity
import com.tailorbook.feature.measurements.data.local.dao.MeasurementDao
import com.tailorbook.feature.measurements.data.local.dao.MeasurementTemplateDao
import com.tailorbook.feature.measurements.data.local.entity.MeasurementTemplateEntity
import com.tailorbook.feature.measurements.data.local.entity.MeasurementVersionEntity
import com.tailorbook.feature.orders.data.local.dao.OrderDao
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.data.local.entity.OrderItemEntity
import com.tailorbook.feature.orders.data.local.entity.OrderMeasurementSnapshotEntity
import com.tailorbook.feature.orders.data.local.entity.OrderStatusHistoryEntity
import com.tailorbook.feature.payments.data.local.dao.PaymentDao
import com.tailorbook.feature.payments.data.local.entity.PaymentEntity
import com.tailorbook.feature.payments.data.local.entity.PaymentRefundEntity
import com.tailorbook.feature.staff.data.local.dao.StaffDao
import com.tailorbook.feature.staff.data.local.entity.StaffEntity

/**
 * Main Room Database for TailorBook.
 * Contains local offline storage for customers, family members, measurements, orders, payments, and staff.
 */
@Database(
    entities = [
        CustomerEntity::class,
        FamilyMemberEntity::class,
        MeasurementVersionEntity::class,
        MeasurementTemplateEntity::class,
        DesignReferenceEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        OrderMeasurementSnapshotEntity::class,
        OrderStatusHistoryEntity::class,
        PaymentEntity::class,
        PaymentRefundEntity::class,
        StaffEntity::class,
        SyncQueueEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class TailorBookDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun measurementTemplateDao(): MeasurementTemplateDao
    abstract fun designReferenceDao(): DesignReferenceDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao
    abstract fun staffDao(): StaffDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: TailorBookDatabase? = null

        fun getInstance(context: Context): TailorBookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TailorBookDatabase::class.java,
                    "tailorbook_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
