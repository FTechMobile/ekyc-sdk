package ai.ftech.fekyc.domain.repo

import ai.ftech.fekyc.data.source.remote.model.ekyc.init.sdk.RegisterEkycData
import ai.ftech.fekyc.data.source.remote.model.ekyc.submit.NewSubmitInfoRequest
import ai.ftech.fekyc.data.source.remote.model.ekyc.transaction.TransactionData
import ai.ftech.fekyc.domain.model.capture.CaptureData
import ai.ftech.fekyc.domain.model.facematching.FaceMatchingData
import ai.ftech.fekyc.domain.model.transaction.TransactionProcessData
import java.io.File

interface INewEKYCRepo {
    fun registerEkyc(appId: String, licenseKey: String, deviceType: String): RegisterEkycData

    fun createTransaction(extraData: String, deviceType: String): TransactionData

    fun getProcessTransaction(transactionId: String, deviceType: String): TransactionProcessData

    fun submitInfo(request: NewSubmitInfoRequest, deviceType: String): Boolean

    fun capturePhoto(transactionId: String, orientation: String, imagePath: String, deviceType: String):CaptureData

    fun captureFace(transactionId: String, imagePath: String, deviceType: String): CaptureData

    fun liveness(files: ArrayList<File>, action: String, transId: String, deviceType: String): Boolean

    fun faceMatching(
        idTransaction: String,
        idSessionFront: String,
        idSessionBack: String,
        idSessionFace: String,
        deviceType: String
    ): FaceMatchingData
}
