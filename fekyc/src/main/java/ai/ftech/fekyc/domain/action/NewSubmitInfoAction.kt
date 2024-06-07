package ai.ftech.fekyc.domain.action

import ai.ftech.fekyc.base.common.BaseAction
import ai.ftech.fekyc.data.source.remote.model.ekyc.submit.NewSubmitInfoRequest
import ai.ftech.fekyc.di.RepositoryFactory
import ai.ftech.fekyc.domain.model.ekyc.DEVICE_TYPE

class NewSubmitInfoAction : BaseAction<NewSubmitInfoAction.SubmitRV,Boolean>() {
    override suspend fun execute(rv: SubmitRV): Boolean {
        val repo = RepositoryFactory.getNewEKYCRepo()
        return repo.submitInfo(rv.request, DEVICE_TYPE.ANDROID.value)
    }

    class SubmitRV(val request: NewSubmitInfoRequest) : RequestValue
}

