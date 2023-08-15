package com.example.postgresqlinsertion.batchinsertion.impl.saver

import com.example.postgresqlinsertion.batchinsertion.api.processor.BatchInsertionByPropertyProcessor
import com.example.postgresqlinsertion.batchinsertion.api.saver.BatchInsertionByPropertySaver
import com.example.postgresqlinsertion.batchinsertion.exception.BatchInsertionException
import com.example.postgresqlinsertion.logic.entity.BaseEntity
import javax.sql.DataSource
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

open class UpdateByPropertySaver<E: BaseEntity>(
    private val processor: BatchInsertionByPropertyProcessor,
    private val entityClass: KClass<E>,
    dataSource: DataSource,
) : AbstractBatchInsertionSaver(dataSource), BatchInsertionByPropertySaver<E> {

    private val nullValue = "NULL"
    private val dataForUpdate = mutableListOf<String>()

    override fun addDataForSave(data: Map<out KProperty1<E, *>, String?>) {
        val id = entityClass
            .memberProperties
            .find { it.name == "id" }
            ?.let { data[it] }
            ?.toLongOrNull()
            ?: throw BatchInsertionException("Id should be not null for update")
        dataForUpdate.add(processor.getStringForUpdate(data, id, nullValue))
    }

    override fun saveData(columns: Set<KProperty1<E, *>>) {
        processor.updateDataToDataBase(entityClass, columns, dataForUpdate, conn)
    }
}