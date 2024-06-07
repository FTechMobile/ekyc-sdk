package ai.ftech.fekyc.domain.action

import ai.ftech.fekyc.base.common.BaseAction
import ai.ftech.fekyc.di.RepositoryFactory
import ai.ftech.fekyc.domain.model.ekyc.FACE_POSE_TYPE
import java.io.File

class LivenessEkycAction : BaseAction<LivenessEkycAction.LivenessEkycRV, Boolean>() {
    override suspend fun execute(rv: LivenessEkycRV): Boolean {
        val repo = RepositoryFactory.getNewEKYCRepo()
        return repo.liveness(rv.files, rv.actions.value, rv.transId, rv.deviceType)
    }

    class LivenessEkycRV(
        var files: ArrayList<File>,
        var actions: FACE_POSE_TYPE,
        var transId: String,
        var deviceType: String,
    ) : RequestValue
}
