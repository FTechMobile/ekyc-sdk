package ai.ftech.fekyc.presentation.picture.take

import ai.ftech.fekyc.base.common.BaseViewModel
import ai.ftech.fekyc.common.action.FEkycActionResult
import ai.ftech.fekyc.domain.APIException
import ai.ftech.fekyc.domain.model.capture.CaptureData
import ai.ftech.fekyc.domain.model.ekyc.*
import ai.ftech.fekyc.publish.FTechEkycManager
import ai.ftech.fekyc.publish.IFTechEkycCallback
import ai.ftech.fekyc.utils.FileUtils
import android.graphics.Bitmap
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

class TakePictureViewModel : BaseViewModel() {
    val BATCH_SIZE = 2
    var currentPhotoType: PHOTO_TYPE? = null
    var files = ArrayList<File>()
    var actions: FACE_POSE_TYPE = FACE_POSE_TYPE.NONE

    var uploadPhoto = MutableLiveData(FEkycActionResult<UPLOAD_STATUS>())
        private set

    var uploadFrame = MutableLiveData(FEkycActionResult<UPLOAD_STATUS>())
        private set

    var filePath: String? = null
        private set

    var retakePhotoType: PHOTO_INFORMATION? = null

    var countDownTimer: CountDownTimer? = null

    var remainingTime: Long = 0L

    init {
        currentPhotoType = EkycStep.getType()
    }

    fun clearUploadPhotoValue() {
        uploadPhoto.value = FEkycActionResult<UPLOAD_STATUS>().apply {
            this.data = UPLOAD_STATUS.NONE
        }
    }

    fun uploadPhoto(absolutePath: String) {
        val orientation = when (retakePhotoType ?: EkycStep.getCurrentStep()) {
            PHOTO_INFORMATION.FRONT -> CAPTURE_TYPE.FRONT
            PHOTO_INFORMATION.BACK -> CAPTURE_TYPE.BACK
            PHOTO_INFORMATION.FACE -> CAPTURE_TYPE.FACE
            else -> CAPTURE_TYPE.FRONT
        }
        viewModelScope.launch {
            FTechEkycManager.uploadPhoto(absolutePath, orientation = orientation, object : IFTechEkycCallback<CaptureData> {
                override fun onSuccess(info: CaptureData?) {
                    super.onSuccess(info)
                    EkycStep.add(PHOTO_TYPE.SSN, absolutePath, retakePhotoType)
                    uploadPhoto.value = if (EkycStep.isDoneStep()) {
                        FEkycActionResult<UPLOAD_STATUS>().apply {
                            this.data = UPLOAD_STATUS.COMPLETE
                        }

                    } else {
                        FEkycActionResult<UPLOAD_STATUS>().apply {
                            this.data = UPLOAD_STATUS.SUCCESS
                        }
                    }
                }

                override fun onFail(error: APIException?) {
                    super.onFail(error)
                    if (error is APIException) {
                        error.printStackTrace()
                    }
                    filePath = absolutePath
                    uploadPhoto.value = FEkycActionResult<UPLOAD_STATUS>().apply {
                        this.exception = error
                        this.data = UPLOAD_STATUS.FAIL
                    }
                }
            })
        }
    }

    fun getFolderPathByEkycType(retakePhotoInformation: PHOTO_INFORMATION?): String {
        return when (retakePhotoInformation ?: EkycStep.getCurrentStep()) {
            PHOTO_INFORMATION.FRONT -> FileUtils.getIdentityFrontPath()
            PHOTO_INFORMATION.BACK -> FileUtils.getIdentityBackPath()
            PHOTO_INFORMATION.FACE -> FileUtils.getFacePath()
            PHOTO_INFORMATION.PAGE_NUMBER_2 -> FileUtils.getPassportPath()
        }
    }

    fun uploadFrame() {
        if (actions == FACE_POSE_TYPE.NONE) return
        viewModelScope.launch {
            FTechEkycManager.checkLivenessFrame(files, actions, object : IFTechEkycCallback<Boolean> {
                override fun onSuccess(info: Boolean?) {
                    uploadFrame.value = FEkycActionResult<UPLOAD_STATUS>().apply {
                        this.data = UPLOAD_STATUS.SUCCESS
                    }
                }

                override fun onFail(error: APIException?) {
                    if (error is APIException) {
                        error.printStackTrace()
                    }
                    uploadFrame.value = FEkycActionResult<UPLOAD_STATUS>().apply {
                        this.exception = error
                        this.data = UPLOAD_STATUS.FAIL
                    }
                }
            })
        }
    }

    fun updateFrame(bitmap: Bitmap, filePath: String) {
        val file = FileUtils.bitmapToFile(bitmap, filePath)
        if (file != null && isValidUpdateFrame())
            files.add(file)

        if (files.size == BATCH_SIZE) {
            Log.e("uploadframe", "uploadframe")
            uploadFrame()
        }
    }

    fun clearFrame() {
        files.clear()
    }

    fun isValidUpdateFrame(): Boolean {

        return files.size < BATCH_SIZE
    }
}
