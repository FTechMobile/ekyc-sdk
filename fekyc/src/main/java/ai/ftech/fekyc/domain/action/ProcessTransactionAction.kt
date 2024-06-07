package ai.ftech.fekyc.domain.action

import ai.ftech.fekyc.base.common.BaseAction
import ai.ftech.fekyc.di.RepositoryFactory
import ai.ftech.fekyc.domain.model.ekyc.DEVICE_TYPE
import ai.ftech.fekyc.domain.model.transaction.TransactionProcessData

class ProcessTransactionAction :
    BaseAction<ProcessTransactionAction.ProcessTransactionRV, TransactionProcessData>() {
    class ProcessTransactionRV(var transactionId: String) : RequestValue

    override suspend fun execute(rv: ProcessTransactionRV): TransactionProcessData {
        val repo = RepositoryFactory.getNewEKYCRepo()
        return repo.getProcessTransaction(rv.transactionId, DEVICE_TYPE.ANDROID.value)
    }
}