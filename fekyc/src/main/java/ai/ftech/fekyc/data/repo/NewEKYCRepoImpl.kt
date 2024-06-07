package ai.ftech.fekyc.data.repo

import ai.ftech.fekyc.base.repo.BaseRepo
import ai.ftech.fekyc.data.repo.converter.CapturePhotoResponseConvertToData
import ai.ftech.fekyc.data.repo.converter.FaceMatchingResponseConvertData
import ai.ftech.fekyc.data.repo.converter.TransactionProcessResponseConvertToData
import ai.ftech.fekyc.data.source.remote.base.invokeApi
import ai.ftech.fekyc.data.source.remote.base.invokeInitSDKFEkycService
import ai.ftech.fekyc.data.source.remote.base.invokeNewFEkycService
import ai.ftech.fekyc.data.source.remote.model.ekyc.facematching.FaceMatchingRequest
import ai.ftech.fekyc.data.source.remote.model.ekyc.init.sdk.InitSDKRequest
import ai.ftech.fekyc.data.source.remote.model.ekyc.init.sdk.RegisterEkycData
import ai.ftech.fekyc.data.source.remote.model.ekyc.submit.NewSubmitInfoRequest
import ai.ftech.fekyc.data.source.remote.model.ekyc.transaction.TransactionData
import ai.ftech.fekyc.data.source.remote.model.ekyc.transaction.TransactionRequest
import ai.ftech.fekyc.data.source.remote.service.InitSDKService
import ai.ftech.fekyc.data.source.remote.service.NewEkycService
import ai.ftech.fekyc.domain.model.capture.CaptureData
import ai.ftech.fekyc.domain.model.facematching.FaceMatchingData
import ai.ftech.fekyc.domain.model.transaction.TransactionProcessData
import ai.ftech.fekyc.domain.repo.INewEKYCRepo
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class NewEKYCRepoImpl : BaseRepo(), INewEKYCRepo {

    companion object {
        private const val PART_IMAGE = "image"
        private const val PART_FILES = "files"
        private const val PART_ORIENTATION = "card_orientation"
        private const val PART_TRANSACTION_ID = "transaction_id"
    }

    override fun registerEkyc(appId: String, licenseKey: String, deviceType: String): RegisterEkycData {
        val service = invokeInitSDKFEkycService(InitSDKService::class.java)

        val request = InitSDKRequest().apply {
            this.appId = appId
            this.secretKey = licenseKey
        }

        return service.registerEkyc(request, deviceType).invokeApi { _, body -> body.data!! }
    }

    override fun createTransaction(extraData: String, deviceType: String): TransactionData {
        val service = invokeNewFEkycService(NewEkycService::class.java)

        val request = TransactionRequest().apply {
            this.extraData = extraData
        }
        return service.createTransaction(request, deviceType).invokeApi { _, body ->
            body.data!!
        }
    }

    override fun getProcessTransaction(transactionId: String, deviceType: String): TransactionProcessData {
        val service = invokeNewFEkycService(NewEkycService::class.java)
        return service.getProcessTransaction(transactionId, deviceType).invokeApi { _, body ->
            TransactionProcessResponseConvertToData().convert(body.data)
        }
    }

    override fun submitInfo(request: NewSubmitInfoRequest, deviceType: String): Boolean {
        val service = invokeNewFEkycService(NewEkycService::class.java)

        return service.submitInfo(request, deviceType).invokeApi { _, _ ->
            true
        }
    }

    override fun capturePhoto(
        transactionId: String,
        orientation: String,
        imagePath: String,
        deviceType: String
    ): CaptureData {
        val service = invokeNewFEkycService(NewEkycService::class.java)

        val partImage = convertImageToMultipart(imagePath)
        val partTransactionId =
            hashMapOf(PART_TRANSACTION_ID to convertToRequestBody(transactionId))
        val partOrientation = hashMapOf(PART_ORIENTATION to convertToRequestBody(orientation))

        return service.capturePhoto(partImage, partTransactionId, partOrientation, deviceType)
            .invokeApi { _, data ->
                CapturePhotoResponseConvertToData().convert(data)
            }
    }

    override fun captureFace(transactionId: String, imagePath: String, deviceType: String): CaptureData {
        val service = invokeNewFEkycService(NewEkycService::class.java)

        val partImage = convertImageToMultipart(imagePath)
        val partTransactionId =
            hashMapOf(PART_TRANSACTION_ID to convertToRequestBody(transactionId))

        return service.captureFace(partImage, partTransactionId, deviceType)
            .invokeApi { _, data -> CapturePhotoResponseConvertToData().convert(data) }
    }

    override fun liveness(files: ArrayList<File>, action: String, transId: String, deviceType: String): Boolean {
        val service = invokeNewFEkycService(NewEkycService::class.java)
        val imagesParts = ArrayList<MultipartBody.Part>()
        files.forEach {
            imagesParts.add(MultipartBody.Part.createFormData(PART_FILES, it.name, it.asRequestBody()))
        }

        return service.liveness(action, transId, deviceType, imagesParts).invokeApi { _, _ -> true }
    }

    override fun faceMatching(
        idTransaction: String,
        idSessionFront: String,
        idSessionBack: String,
        idSessionFace: String,
        deviceType: String
    ): FaceMatchingData {
        val service = invokeNewFEkycService(NewEkycService::class.java)
        val bodyMatching = FaceMatchingRequest().apply {
            transactionId = idTransaction
            sessionIdBack = idSessionBack
            sessionIdFront = idSessionFront
            sessionIdFace = idSessionFace
        }
        return service.faceMatching(body = bodyMatching, deviceType).invokeApi { _, body ->
            FaceMatchingResponseConvertData().convert(body.data)
        }
    }


    private fun convertImageToMultipart(absolutePath: String): MultipartBody.Part {
        val file = File(absolutePath)
        return MultipartBody.Part.createFormData(PART_IMAGE, file.name, file.asRequestBody())
    }

    private fun convertToRequestBody(field: String): RequestBody {
        return field.toRequestBody("text/plain".toMediaTypeOrNull())
    }

}
