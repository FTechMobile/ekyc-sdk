package ai.ftech.fekyc.publish

import ai.ftech.fekyc.AppConfig
import ai.ftech.fekyc.R
import ai.ftech.fekyc.base.common.BaseAction
import ai.ftech.fekyc.base.extension.setApplication
import ai.ftech.fekyc.common.getAppString
import ai.ftech.fekyc.common.onException
import ai.ftech.fekyc.data.source.remote.model.ekyc.init.sdk.RegisterEkycData
import ai.ftech.fekyc.data.source.remote.model.ekyc.submit.NewSubmitInfoRequest
import ai.ftech.fekyc.data.source.remote.model.ekyc.transaction.TransactionData
import ai.ftech.fekyc.domain.APIException
import ai.ftech.fekyc.domain.action.*
import ai.ftech.fekyc.domain.model.capture.CaptureData
import ai.ftech.fekyc.domain.model.ekyc.*
import ai.ftech.fekyc.domain.model.facematching.FaceMatchingData
import ai.ftech.fekyc.domain.model.transaction.TransactionProcessData
import ai.ftech.fekyc.infras.EncodeRSA
import ai.ftech.fekyc.presentation.AppPreferences
import ai.ftech.fekyc.presentation.home.HomeActivity
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.renderscript.*
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.launch
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object FTechEkycManager {
    private var applicationContext: Context? = null

    private val launcherMap = ConcurrentHashMap<Int, ActivityResultLauncher<Unit>?>()
    private var resultLauncher: ActivityResultLauncher<Unit>? = null
    private var pendingCallback: (() -> Unit)? = null
    private var callback: IFTechEkycCallback<FTechEkycInfo>? = null
    lateinit var detector: FaceDetector
    private var isActive = true
    private var coolDownTime: Long = -1
    private var isEnableLiveness: Boolean = false
    private var cropType: CROP_TYPE = CROP_TYPE.DEFAULT

    @JvmStatic
    fun init(context: Context) {
        applicationContext = context
        setApplication(getApplicationContext())
        AppPreferences.init(context)
        initMLKit()
    }

    @JvmStatic
    fun getApplicationContext(): Application {
        return applicationContext as? Application
            ?: throw RuntimeException("applicationContext must not null")
    }

    @JvmStatic
    fun register(context: Context) {
        if (context is FragmentActivity) {
            resultLauncher = if (launcherMap.contains(context.hashCode())) {
                launcherMap[context.hashCode()]
            } else {
                context.registerForActivityResult(object :
                    ActivityResultContract<Unit, FTechEkycResult<FTechEkycInfo>>() {
                    override fun createIntent(context: Context, input: Unit?): Intent {
                        return Intent(context, HomeActivity::class.java)
                    }

                    override fun parseResult(
                        resultCode: Int,
                        intent: Intent?
                    ): FTechEkycResult<FTechEkycInfo> {
                        return parseDataFormActivityForResult(resultCode, intent)
                    }
                }) {
                    invokeCallback(callback, it)
                }
            }

            launcherMap[context.hashCode()] = resultLauncher
        }
    }

    @JvmStatic
    fun notifyActive(context: Context) {
        pendingCallback?.let {
            it.invoke()
            pendingCallback = null
        }

        if (context is FragmentActivity) {
            resultLauncher = launcherMap[context.hashCode()]
        }

        isActive = true
    }

    @JvmStatic
    fun notifyInactive(context: Context) {
        isActive = false
    }

    @JvmStatic
    fun unregister(context: Context) {
        callback = null

        if (context is FragmentActivity) {
            launcherMap.remove(context.hashCode())
        }
    }

    @JvmStatic
    fun setEnableLiveness(isEnableLiveness: Boolean) {
        this.isEnableLiveness = isEnableLiveness
    }

    @JvmStatic
    internal fun isEnableLiveness() = isEnableLiveness

    @JvmStatic
    fun setCropType(type: CROP_TYPE) {
        this.cropType = type
    }

    internal fun getCropType() = cropType

    @JvmStatic
    fun startEkyc(
        licenseKey: String,
        appId: String,
        callBack: IFTechEkycCallback<FTechEkycInfo>
    ) {
        AppPreferences.ftechKey = EncodeRSA.encryptData(licenseKey, applicationContext?.packageName)
        AppPreferences.appId = appId
        checkCoolDownAction {
            if (licenseKey.isEmpty()) {
                throw RuntimeException(getAppString(R.string.empty_license_key))
            }

            if (appId.isEmpty()) {
                throw RuntimeException(getAppString(R.string.empty_license_key))
            }

            if (AppPreferences.transactionId.isNullOrEmpty()) {
                throw RuntimeException(getAppString(R.string.empty_license_key))
            }

            callback = callBack
            resultLauncher?.launch()
        }
    }

    private fun parseDataFormActivityForResult(
        resultCode: Int,
        intent: Intent?
    ): FTechEkycResult<FTechEkycInfo> {
        val info =
            intent?.getSerializableExtra(HomeActivity.SEND_RESULT_FTECH_EKYC_INFO) as? FTechEkycInfo

        when (resultCode) {
            Activity.RESULT_OK -> {
                return FTechEkycResult<FTechEkycInfo>().apply {
                    this.type = if (info != null) {
                        FTECH_EKYC_RESULT_TYPE.SUCCESS
                    } else {
                        FTECH_EKYC_RESULT_TYPE.ERROR
                    }
                    this.data = info
                }
            }

            Activity.RESULT_CANCELED -> {
                return FTechEkycResult<FTechEkycInfo>().apply {
                    this.type = FTECH_EKYC_RESULT_TYPE.CANCEL
                }
            }

            else -> {
                return FTechEkycResult<FTechEkycInfo>().apply {
                    this.type = FTECH_EKYC_RESULT_TYPE.ERROR
                }
            }
        }
    }

    private fun <T> invokeCallback(callback: IFTechEkycCallback<T>?, result: FTechEkycResult<T>) {
        when (result.type) {
            FTECH_EKYC_RESULT_TYPE.SUCCESS -> {
                if (isActive) {
                    callback?.onSuccess(result.data!!)
                } else {
                    pendingCallback = {
                        callback?.onSuccess(result.data!!)
                    }
                }
            }

            FTECH_EKYC_RESULT_TYPE.ERROR -> {
                if (isActive) {
                    callback?.onFail(result.error)
                } else {
                    pendingCallback = {
                        callback?.onFail(result.error)
                    }
                }
            }

            FTECH_EKYC_RESULT_TYPE.CANCEL -> {
                if (isActive) {
                    callback?.onCancel()
                } else {
                    pendingCallback = {
                        callback?.onCancel()
                    }
                }
            }
        }
    }

    private fun checkCoolDownAction(action: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (coolDownTime == -1L || (currentTime - coolDownTime > AppConfig.ACTION_DELAY)) {
            coolDownTime = currentTime
            action.invoke()
        }
    }

    private fun <I : BaseAction.RequestValue, O> runActionInCoroutine(
        action: BaseAction<I, O>,
        request: I,
        callback: IFTechEkycCallback<O>?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            action.invoke(request)
                .onException {
                    CoroutineScope(Dispatchers.Main).launch {
                        invokeCallback(callback, FTechEkycResult<O>().apply {
                            this.type = FTECH_EKYC_RESULT_TYPE.ERROR
                            this.error = if (it is APIException) it else APIException(
                                APIException.UNKNOWN_ERROR,
                                it.message
                            )
                        })
                    }
                }.collect {
                    CoroutineScope(Dispatchers.Main).launch {
                        invokeCallback(callback, FTechEkycResult<O>().apply {
                            this.type = FTECH_EKYC_RESULT_TYPE.SUCCESS
                            this.data = it
                        })
                    }
                }
        }
    }

    // start ekyc
    @JvmStatic
    fun registerEkyc(appId: String, licenseKey: String, callback: IFTechEkycCallback<Boolean>) {
        if (appId.isEmpty()) {
            callback.onFail(
                APIException(
                    code = APIException.UNKNOWN_ERROR,
                    message = getAppString(R.string.empty_app_id)
                )
            )
            return
        }
        if (licenseKey.isEmpty()) {
            callback.onFail(
                APIException(
                    code = APIException.UNKNOWN_ERROR,
                    message = getAppString(R.string.empty_license_key)
                )
            )
            return
        }
        runActionInCoroutine(
            RegisterEkycAction(),
            RegisterEkycAction.RegisterEkycRV(appId, licenseKey),
            callback = object : IFTechEkycCallback<RegisterEkycData> {
                override fun onSuccess(info: RegisterEkycData) {
                    AppPreferences.token = info.token
                    callback.onSuccess(true)
                }

                override fun onCancel() {
                    callback.onCancel()
                }

                override fun onFail(error: APIException?) {
                    callback.onFail(error)
                }
            }
        )
    }


    @JvmStatic
    fun createTransaction(callback: IFTechEkycCallback<TransactionData>) {
        if (!hasTokenRegister()) {
            callback.onFail(
                APIException(
                    code = APIException.UNKNOWN_ERROR,
                    message = getAppString(R.string.null_token_register)
                )
            )
            return
        }
        clearData()
        runActionInCoroutine(
            TransactionAction(),
            BaseAction.VoidRequest(),
            object : IFTechEkycCallback<TransactionData> {
                override fun onSuccess(info: TransactionData) {
                    if (info.transactionId.isNullOrEmpty()) {
                        callback.onFail(
                            APIException(
                                code = APIException.UNKNOWN_ERROR,
                                message = getAppString(R.string.null_or_empty_transaction_id)
                            )
                        )
                    } else {
                        AppPreferences.transactionId = info.transactionId.toString()
                        callback.onSuccess(info)
                    }
                }

                override fun onCancel() {
                    callback.onCancel()
                }

                override fun onFail(error: APIException?) {
                    callback.onFail(error)
                }
            }
        )
    }

    @JvmStatic
    fun getProcessTransaction(transactionId: String, callback: IFTechEkycCallback<TransactionProcessData>) {
        runActionInCoroutine(
            action = ProcessTransactionAction(),
            request = ProcessTransactionAction.ProcessTransactionRV(transactionId = transactionId),
            callback = object : IFTechEkycCallback<TransactionProcessData> {
                override fun onSuccess(info: TransactionProcessData) {
                    handleProcessTransaction(info)
                    callback.onSuccess(info)
                }

                override fun onCancel() {
                    callback.onCancel()
                }

                override fun onFail(error: APIException?) {
                    callback.onFail(error)
                }
            })
    }

    @JvmStatic
    fun detectFacePose(bitmap: Bitmap, facePost: FACE_POSE, callback: IFTechEkycCallback<Boolean>) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        Tasks.await(detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.size == 1) {
                    getFacePost(faces[0], facePost, object : IFTechEkycCallback<Boolean> {
                        override fun onSuccess(info: Boolean?) {
                            callback.onSuccess(info)
                        }

                        override fun onFail(error: APIException?) {

                        }

                        override fun onCancel() {

                        }
                    })
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
            })
    }

    @JvmStatic
    fun getFacePost(face: Face, facePost: FACE_POSE, callback: IFTechEkycCallback<Boolean>) {
        Log.e("face", "x: ${face.headEulerAngleX} / y: ${face.headEulerAngleY}")
        when {
            //quay trái
            facePost == FACE_POSE.LEFT && face.headEulerAngleY > FACE_POSE.LEFT.value -> callback.onSuccess(true)

            //quay phải
            facePost == FACE_POSE.RIGHT && face.headEulerAngleY < FACE_POSE.RIGHT.value -> callback.onSuccess(true)

            //ngửa lên
            facePost == FACE_POSE.UP && face.headEulerAngleX > FACE_POSE.UP.value -> callback.onSuccess(true)

            //cúi xuống
            facePost == FACE_POSE.DOWN && face.headEulerAngleX < FACE_POSE.DOWN.value -> callback.onSuccess(true)

            //thẳng mặt
            facePost == FACE_POSE.STRAIGHT && face.headEulerAngleX in -FACE_POSE.STRAIGHT.value..FACE_POSE.STRAIGHT.value
                    && face.headEulerAngleY in -FACE_POSE.STRAIGHT.value..FACE_POSE.STRAIGHT.value -> callback.onSuccess(true)

        }
    }

    private fun handleProcessTransaction(info: TransactionProcessData) {
        if (info.processId.isNullOrEmpty()) {
            AppPreferences.sessionIdFront = info.sessionIdFront
            AppPreferences.sessionIdBack = info.sessionIdBack
            AppPreferences.sessionIdFace = info.sessionIdFace
        }
    }

    @JvmStatic
    fun submitInfo(submitInfoRequest: NewSubmitInfoRequest, callback: IFTechEkycCallback<Boolean>) {
        runActionInCoroutine(
            action = NewSubmitInfoAction(),
            request = NewSubmitInfoAction.SubmitRV(request = submitInfoRequest),
            callback = object : IFTechEkycCallback<Boolean> {
                override fun onSuccess(info: Boolean?) {
                    clearData()
                    callback.onSuccess(info)
                }

                override fun onCancel() {
                    callback.onCancel()
                }

                override fun onFail(error: APIException?) {
                    callback.onFail(error)
                }
            }
        )
    }

    @JvmStatic
    fun uploadPhoto(
        pathImage: String,
        orientation: CAPTURE_TYPE,
        callback: IFTechEkycCallback<CaptureData>
    ) {
        if (!hasTransactionId()) {
            callback.onFail(
                APIException(
                    APIException.UNKNOWN_ERROR,
                    getAppString(R.string.null_or_empty_transaction_id)
                )
            )
            return
        }
        runActionInCoroutine(
            action = NewUploadPhotoAction(),
            request = NewUploadPhotoAction.UploadRV(
                absolutePath = pathImage,
                orientation = orientation,
                transactionId = AppPreferences.transactionId ?: ""
            ),
            callback = object : IFTechEkycCallback<CaptureData> {
                override fun onSuccess(info: CaptureData) {
                    if (info.data?.sessionId.isNullOrEmpty()) {
                        callback.onFail(getErrorSessionId(orientation))
                    } else {
                        handleSuccessUploadPhoto(orientation, info)
                        callback.onSuccess(info)
                    }
                }

                override fun onCancel() {
                    callback.onCancel()
                }

                override fun onFail(error: APIException?) {
                    callback.onFail(error)
                }
            }
        )
    }

    @JvmStatic
    fun checkLivenessFrame(
        files: ArrayList<File>,
        actions: FACE_POSE_TYPE,
        callback: IFTechEkycCallback<Boolean>
    ) {
        if (!hasTransactionId()) {
            callback.onFail(
                APIException(
                    APIException.UNKNOWN_ERROR,
                    getAppString(R.string.null_or_empty_transaction_id)
                )
            )
            return
        }
        runActionInCoroutine(
            action = LivenessEkycAction(),
            request = LivenessEkycAction.LivenessEkycRV(
                files = files,
                actions = actions,
                transId = AppPreferences.transactionId ?: "",
                deviceType = DEVICE_TYPE.ANDROID.value
            ),
            callback = object : IFTechEkycCallback<Boolean> {
                override fun onSuccess(info: Boolean) {
                    callback.onSuccess(info)
                }

                override fun onCancel() {
                    callback.onCancel()
                }

                override fun onFail(error: APIException?) {
                    callback.onFail(error)
                }
            }
        )
    }

    private fun handleSuccessUploadPhoto(orientation: CAPTURE_TYPE?, info: CaptureData) {
        when (orientation) {
            CAPTURE_TYPE.BACK -> {
                AppPreferences.sessionIdBack = info.data?.sessionId.toString()
            }
            CAPTURE_TYPE.FRONT -> {
                AppPreferences.sessionIdFront = info.data?.sessionId.toString()
            }
            CAPTURE_TYPE.FACE -> {
                AppPreferences.sessionIdFace = info.data?.sessionId.toString()
            }
            else -> {}
        }
    }

    private fun getErrorSessionId(orientation: CAPTURE_TYPE?): APIException {
        return when (orientation) {
            CAPTURE_TYPE.BACK -> {
                APIException(
                    code = APIException.UNKNOWN_ERROR,
                    message = getAppString(R.string.null_or_empty_session_id_back)
                )
            }

            CAPTURE_TYPE.FRONT -> {
                APIException(
                    code = APIException.UNKNOWN_ERROR,
                    message = getAppString(R.string.null_or_empty_session_id_front)
                )
            }

            CAPTURE_TYPE.FACE -> {
                APIException(
                    code = APIException.UNKNOWN_ERROR,
                    message = getAppString(R.string.null_or_empty_session_id_face)
                )
            }

            else -> {
                APIException(
                    code = APIException.UNKNOWN_ERROR,
                    message = getAppString(R.string.null_or_empty_session_id_unknown)
                )
            }
        }
    }

    @JvmStatic
    fun faceMatching(
        callback: IFTechEkycCallback<FaceMatchingData>
    ) {
        if (!hasTransactionAndSessionCaptureId()) {
            callback.onFail(
                APIException(
                    APIException.UNKNOWN_ERROR,
                    getAppString(R.string.empty_transaction_id_and_session_capture)
                )
            )
            return
        }
        runActionInCoroutine(
            action = FaceMatchingAction(), request = FaceMatchingAction.FaceMatchingRV(
                AppPreferences.transactionId ?: "", AppPreferences.sessionIdFront
                    ?: "", AppPreferences.sessionIdBack ?: "", AppPreferences.sessionIdFace ?: ""
            ), callback = callback
        )
    }

    private fun hasTransactionAndSessionCaptureId(): Boolean {
        return !AppPreferences.transactionId.isNullOrEmpty() && !AppPreferences.sessionIdFront.isNullOrEmpty() &&
                !AppPreferences.sessionIdBack.isNullOrEmpty() && !AppPreferences.sessionIdFace.isNullOrEmpty()
    }

    private fun hasTransactionId(): Boolean = !AppPreferences.transactionId.isNullOrEmpty()

    private fun clearData() {
        AppPreferences.clearData()
    }

    private fun hasTokenRegister() = !AppPreferences.token.isNullOrEmpty()

    @Suppress("DEPRECATION")
    fun convertFrameToBitmap(byteArray: ByteArray, width: Int, height: Int): Bitmap {
        val rs = RenderScript.create(applicationContext)
        val yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        val yuvType = Type.Builder(rs, Element.U8(rs)).setX(byteArray.size)
        val inData = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)
        val rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height)
        val outData = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)
        inData.copyFrom(byteArray)
        yuvToRgbIntrinsic.setInput(inData)
        yuvToRgbIntrinsic.forEach(outData)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outData.copyTo(bitmap)
        return bitmap
    }

    private fun initMLKit() {
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(realTimeOpts)
    }

    internal fun getFtechKey(): String = AppPreferences.ftechKey ?: ""
    internal fun getAppID(): String = AppPreferences.appId ?: ""
    internal fun getTransactionID(): String = AppPreferences.transactionId ?: ""

}
