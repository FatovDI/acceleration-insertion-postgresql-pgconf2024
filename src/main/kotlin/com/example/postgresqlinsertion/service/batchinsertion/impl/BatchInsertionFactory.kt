package com.example.postgresqlinsertion.service.batchinsertion.impl

import com.example.postgresqlinsertion.entity.BaseEntity
import com.example.postgresqlinsertion.service.batchinsertion.api.BatchInsertionByEntityProcessor
import com.example.postgresqlinsertion.service.batchinsertion.api.IBatchInsertionFactory
import com.example.postgresqlinsertion.service.batchinsertion.api.BatchInsertionSaver
import com.example.postgresqlinsertion.service.batchinsertion.api.SaverType
import javax.sql.DataSource
import kotlin.reflect.KClass

abstract class BatchInsertionFactory<E: BaseEntity>(
    private val entityClass: KClass<E>,
    override val processor: BatchInsertionByEntityProcessor,
    private val dataSource: DataSource,
) : IBatchInsertionFactory<E> {

    override fun getSaver(type: SaverType): BatchInsertionSaver<E> {

        return when (type) {
            SaverType.COPY -> CopyModelSaver(processor, entityClass, dataSource)
            SaverType.COPY_VIA_FILE -> CopyModelViaFileSaver(processor, entityClass, dataSource)
            SaverType.INSERT -> InsertModelSaver(processor, entityClass, dataSource)
            SaverType.UPDATE -> UpdateModelSaver(processor, entityClass, dataSource)
        }

    }
}
