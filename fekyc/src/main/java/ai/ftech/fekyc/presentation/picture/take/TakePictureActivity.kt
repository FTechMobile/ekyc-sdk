package ai.ftech.fekyc.presentation.picture.take

import ai.ftech.fekyc.R
import ai.ftech.fekyc.base.common.StatusBar
import ai.ftech.fekyc.base.extension.*
import ai.ftech.fekyc.common.FEkycActivity
import ai.ftech.fekyc.common.getAppDrawable
import ai.ftech.fekyc.common.getAppString
import ai.ftech.fekyc.common.widget.overlay.OverlayView
import ai.ftech.fekyc.common.widget.toolbar.ToolbarView
import ai.ftech.fekyc.data.source.remote.event.MessageEvent
import ai.ftech.fekyc.domain.APIException
import ai.ftech.fekyc.domain.event.FinishActivityEvent
import ai.ftech.fekyc.domain.model.ekyc.*
import ai.ftech.fekyc.presentation.dialog.ConfirmDialog
import ai.ftech.fekyc.presentation.dialog.WARNING_TYPE
import ai.ftech.fekyc.presentation.dialog.WarningCaptureDialog
import ai.ftech.fekyc.presentation.picture.confirm.ConfirmPictureActivity
import ai.ftech.fekyc.presentation.picture.preview.PreviewPictureActivity
import ai.ftech.fekyc.publish.FTechEkycManager
import ai.ftech.fekyc.publish.IFTechEkycCallback
import ai.ftech.fekyc.utils.FileUtils
import ai.ftech.fekyc.utils.ShareFlowEventBus
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.CountDownTimer
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Flash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File


class TakePictureActivity : FEkycActivity(R.layout.fekyc_take_picture_activity) {

    companion object {
        const val REMAINING_TIME = 60000L
        const val RETAKE_PHOTO_TYPE = "retakePhotoType"
    }

    private lateinit var ovFrameCrop: OverlayView
    private lateinit var cvCameraView: CameraView
    private lateinit var tbvHeader: ToolbarView
    private lateinit var tvWarningText: TextView
    private lateinit var tvLivenessRemainingTime: TextView
    private lateinit var ivFlash: ImageView
    private lateinit var ivCapture: ImageView
    private lateinit var ivChangeCamera: ImageView
    private lateinit var ivProgressTop: ImageView
    private lateinit var ivProgressLeft: ImageView
    private lateinit var ivProgressRight: ImageView
    private lateinit var ivProgressDown: ImageView
    private lateinit var clProgressLiveness: ConstraintLayout
    private lateinit var tvTakePictureTransId: AppCompatTextView

    private var countDownTimer: CountDownTimer? = null
    private val viewModel by viewModels<TakePictureViewModel>()
    private var isFrontFace = false
    private var isFlash = false
    private var file: File? = null
    private var poseDatas = arrayListOf(FACE_POSE.LEFT, FACE_POSE.DOWN, FACE_POSE.RIGHT, FACE_POSE.UP, FACE_POSE.STRAIGHT)
    private var position = -1
    private var isBlockChecking = false

    override fun setupStatusBar(): StatusBar {
        return StatusBar(color = R.color.fbase_color_black, isDarkText = false)
    }

    override fun onResume() {
        super.onResume()
        FTechEkycManager.notifyActive(this)
        cvCameraView.open()
        if (warningDialog == null) {
            warningDialog = WarningCaptureDialog(getWarningType())
        }
    }

    override fun onPause() {
        super.onPause()
        cvCameraView.close()
        warningDialog = null
        FTechEkycManager.notifyInactive(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cvCameraView.close()
        warningDialog = null
    }

    @SuppressLint("SetTextI18n")
    override fun onInitView() {
        super.onInitView()
        val indexRetake = intent.getIntExtra(RETAKE_PHOTO_TYPE, -1)
        viewModel.retakePhotoType = when (indexRetake) {
            0 -> PHOTO_INFORMATION.FRONT
            1 -> PHOTO_INFORMATION.BACK
            2 -> PHOTO_INFORMATION.FACE
            else -> null
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ovFrameCrop = findViewById(R.id.ovTakePictureFrameCrop)
        tbvHeader = findViewById(R.id.tbvTakePictureHeader)
        tvWarningText = findViewById(R.id.tvTakePictureWarningText)
        cvCameraView = findViewById(R.id.cvTakePictureCameraView)
        ivFlash = findViewById(R.id.ivTakePictureFlash)
        ivCapture = findViewById(R.id.ivTakePictureCapture)
        ivChangeCamera = findViewById(R.id.ivTakePictureChangeCamera)
        clProgressLiveness = findViewById(R.id.clLiveNess)
        ivProgressDown = findViewById(R.id.ivLivenessBottom)
        ivProgressLeft = findViewById(R.id.ivLivenessLeft)
        ivProgressTop = findViewById(R.id.ivLivenessTop)
        ivProgressRight = findViewById(R.id.ivLivenessRight)
        tvLivenessRemainingTime = findViewById(R.id.tvLivenessRemainingTime)
        tvTakePictureTransId = findViewById(R.id.tvTakePictureTransId)

        tvTakePictureTransId.text = "Trans ID: ${FTechEkycManager.getTransactionID()}"

        tbvHeader.setTitle(getToolbarTitleByEkycType())

        tbvHeader.setListener(object : ToolbarView.IListener {
            override fun onLeftIconClick() {
                if (viewModel.retakePhotoType != null) {
                    finish()
                } else
                    onBackPressed()
            }

            override fun onRightIconClick() {
                warningDialog?.showDialog(
                    supportFragmentManager,
                    warningDialog!!::class.java.simpleName
                )
            }
        })

        setFacing()

        cvCameraView.apply {
            setLifecycleOwner(this@TakePictureActivity)
            useDeviceOrientation = false
            facing = if (isFrontFace) {
                Facing.FRONT
            } else {
                Facing.BACK
            }

            addCameraListener(object : CameraListener() {
                override fun onPictureTaken(result: PictureResult) {
                    cvCameraView.close()
                    uploadFile(result)
                }
            })
        }

        ivFlash.setOnSafeClick {
            if (isFlash) {
                cvCameraView.flash = Flash.OFF
                ivFlash.setImageDrawable(getAppDrawable(R.drawable.fekyc_ic_flash_off))
                isFlash = false
            } else {
                cvCameraView.flash = Flash.TORCH
                ivFlash.setImageDrawable(getAppDrawable(R.drawable.fekyc_ic_flash_on))
                isFlash = true
            }
        }

        ivCapture.setOnSafeClick {
            cvCameraView.takePictureSnapshot()
            showLoading()
            countDownTimer?.cancel()
        }

        ivChangeCamera.setOnSafeClick {
            if (isFrontFace) {
                cvCameraView.facing = Facing.BACK
                isFrontFace = false
            } else {
                cvCameraView.facing = Facing.FRONT
                isFrontFace = true
            }
        }

        ovFrameCrop.listener = object : OverlayView.ICallback {
            override fun onTakePicture(bitmap: Bitmap) {
                val file = FileUtils.bitmapToFile(bitmap, file?.absolutePath.toString())
                if (file != null) {
                    viewModel.uploadPhoto(file.absolutePath)
                }
            }

            override fun onError(exception: Exception) {
                hideLoading()
                Toast.makeText(this@TakePictureActivity, exception.message, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        lifecycleScope.launchWhenResumed {
            val flow = ShareFlowEventBus.events.filter {
                it is FinishActivityEvent
            }

            flow.collectLatest {
                if (it is FinishActivityEvent) {
                    finish()
                }
            }
        }
    }

    override fun onObserverViewModel() {
        super.onObserverViewModel()
        observer(viewModel.uploadPhoto) {
            hideLoading()
            when (it?.data) {
                UPLOAD_STATUS.FAIL -> {
                    if (it.exception is APIException) {
                        handleCaseUploadFail(it.exception as APIException)
                    } else {
                        Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
                UPLOAD_STATUS.SUCCESS -> {
                    viewModel.clearUploadPhotoValue()
                    navigateToTakePictureScreen()
                }
                UPLOAD_STATUS.COMPLETE -> {
                    finish()
                    EventBus.getDefault().post(MessageEvent())
                    if (viewModel.retakePhotoType == null)
                        navigateTo(ConfirmPictureActivity::class.java)
                }
                UPLOAD_STATUS.NONE -> {}
                else -> {}
            }
        }

        observer(viewModel.uploadFrame) {
            when (it?.data) {
                UPLOAD_STATUS.FAIL -> {
                    showLivenessError(it.exception?.message)
                }
                UPLOAD_STATUS.SUCCESS -> {
                    setProgressLiveness(true)
                    setTextGuideLiveness()
                    isBlockChecking = false
                    viewModel.clearFrame()
                }
                UPLOAD_STATUS.NONE -> {}
                else -> {}
            }
        }
    }

    private fun handleCaseUploadFail(exp: APIException) {
//        when (exp.code) {
//            APIException.TIME_OUT_ERROR,
//            APIException.NETWORK_ERROR -> showNotiNetworkDialog()
//            APIException.EXPIRE_SESSION_ERROR -> {}
//            else ->
//        }
        if (exp.code != APIException.EXPIRE_SESSION_ERROR && exp.code != APIException.LIMIT_SESSION_ERROR)
            navigateToPreviewScreen(viewModel.filePath ?: "", exp.message)
    }

    private fun uploadFile(result: PictureResult) {
        val path = viewModel.getFolderPathByEkycType(viewModel.retakePhotoType)
        val file = File(filesDir, path)

        if (file.exists()) {
            FileUtils.deleteFile(path)
        }

        result.toFile(file) {
            it?.let { file ->
                this.file = file
                ovFrameCrop.attachFile(file.absolutePath)
            }
        }
    }

    private fun navigateToTakePictureScreen() {
        finish()
        navigateTo(TakePictureActivity::class.java)
    }

    private fun navigateToPreviewScreen(path: String, message: String? = null) {
        navigateTo(PreviewPictureActivity::class.java) { intent ->
            intent.putExtra(PreviewPictureActivity.SEND_PREVIEW_IMAGE_KEY, path)
            intent.putExtra(PreviewPictureActivity.SEND_MESSAGE_KEY, message)
        }
    }

    private fun setFacing() {
        when (viewModel.retakePhotoType ?: EkycStep.getCurrentStep()) {
            PHOTO_INFORMATION.FACE -> {
                cvCameraView.facing = Facing.FRONT
                isFrontFace = true
                setEnableCameraFace(false)
                setFrameMlKit()
            }
            PHOTO_INFORMATION.BACK, PHOTO_INFORMATION.FRONT, PHOTO_INFORMATION.PAGE_NUMBER_2 -> {
                cvCameraView.facing = Facing.BACK
                isFrontFace = false
            }
        }
    }

    private fun setFrameMlKit() {
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(realTimeOpts)

        cvCameraView.addFrameProcessor {
            if (it.dataClass === ByteArray::class.java) {
                val bitmap = FTechEkycManager.convertFrameToBitmap(it.getData(), it.size.width, it.size.height)
                val cropBitmap = when (FTechEkycManager.getCropType()) {
                    CROP_TYPE.DEFAULT -> ovFrameCrop.attachBitmap(bitmap)
                    CROP_TYPE.FULL_SCREEN -> ovFrameCrop.attachBitmapFullScreen(bitmap)
                    CROP_TYPE.LARGE -> ovFrameCrop.attachBitmapLarge(bitmap)
                }
                if (isBlockChecking && viewModel.isValidUpdateFrame()) {
                    viewModel.updateFrame(cropBitmap, "$filesDir/${FileUtils.getFramePath(viewModel.files.size)}")
                } else {
                    val inputImage = InputImage.fromBitmap(cropBitmap, 0)
                    Tasks.await(detector.process(inputImage)
                        .addOnSuccessListener { faces ->
                            if (position == -1 && faces.size == 1) {
                                setTextGuideLiveness()
                                position = 0
                            }
                            if (FTechEkycManager.isEnableLiveness()) {
                                if (faces.size == 1)
                                    setFacePose(faces[0])
                            } else {
                                setEnableCameraFace(faces.size == 1)
                            }
                        }
                        .addOnFailureListener {
                            setEnableCameraFace(true)
                        })
                }
            }
        }
    }

    private fun setFacePose(face: Face) {
        if (position == 5) {
            setTextGuideLiveness()
            return
        }
        if (isBlockChecking) return

        FTechEkycManager.getFacePost(face, poseDatas[position], object : IFTechEkycCallback<Boolean> {
            override fun onSuccess(info: Boolean?) {
                setActionsFace()
                position++
                setProgressLiveness(false)
                isBlockChecking = true
//                setTextGuideLiveness()
            }
        })
    }

    private fun setActionsFace() {
        viewModel.actions = when (poseDatas[position]) {
            FACE_POSE.LEFT -> FACE_POSE_TYPE.LEFT
            FACE_POSE.DOWN -> FACE_POSE_TYPE.DOWN
            FACE_POSE.RIGHT -> FACE_POSE_TYPE.RIGHT
            FACE_POSE.UP -> FACE_POSE_TYPE.UP
            else -> FACE_POSE_TYPE.NONE
        }
    }

    private fun setTextGuideLiveness() {
        CoroutineScope(Dispatchers.Main).launch {
            when (position) {
                -1 -> tvWarningText.text = getAppString(R.string.liveness_fill_face)
                0 -> tvWarningText.text = getAppString(R.string.liveness_face_pose_left)
                1 -> tvWarningText.text = getAppString(R.string.liveness_face_pose_down)
                2 -> tvWarningText.text = getAppString(R.string.liveness_face_pose_right)
                3 -> tvWarningText.text = getAppString(R.string.liveness_face_pose_up)
                4 -> tvWarningText.text = getAppString(R.string.liveness_face_pose_straight)
                5 -> tvWarningText.text = getAppString(R.string.liveness_face_pose_straight)
            }
        }
    }

    private fun setProgressLiveness(isApiVerify: Boolean) {
        when (position) {
            1 -> ivProgressLeft.setColor(isApiVerify)
            2 -> ivProgressDown.setColor(isApiVerify)
            3 -> ivProgressRight.setColor(isApiVerify)
            4 -> ivProgressTop.setColor(isApiVerify)
            5 -> {
                ivCapture.show()
                ivCapture.setImageDrawable(getAppDrawable(R.drawable.fekyc_ic_capture_on))
            }
        }
    }

    private fun setEnableCameraFace(isEnable: Boolean) {
        if (FTechEkycManager.isEnableLiveness()) {
            ovFrameCrop.setFrameCrop(null)
            clProgressLiveness.show()
            ivCapture.hide()
            ivChangeCamera.hide()
            ivFlash.hide()
            ivCapture.isEnabled = true
            tvLivenessRemainingTime.show()
            setupTimer()
        } else {
            if (isEnable) {
                ovFrameCrop.setFrameCrop(getAppDrawable(R.drawable.fekyc_ic_photo_circle_blue_crop))
                ivCapture.setImageDrawable(getAppDrawable(R.drawable.fekyc_ic_capture_on))
                ivCapture.isEnabled = true
            } else {
                ovFrameCrop.setFrameCrop(getAppDrawable(R.drawable.fekyc_ic_photo_circle_white_crop))
                ivCapture.setImageDrawable(getAppDrawable(R.drawable.fekyc_ic_capture_off))
                ivCapture.isEnabled = false
            }
        }
    }

    private fun getWarningType(): WARNING_TYPE {
        return when (viewModel.retakePhotoType ?: EkycStep.getCurrentStep()) {
            PHOTO_INFORMATION.FRONT,
            PHOTO_INFORMATION.PAGE_NUMBER_2 -> {
                ovFrameCrop.apply {
                    setCropType(OverlayView.CROP_TYPE.REACTANGLE)
                }
                tvWarningText.text = getAppString(R.string.fekyc_take_picture_warning_take_papers)
                WARNING_TYPE.PAPERS
            }
            PHOTO_INFORMATION.BACK -> {
                tvWarningText.text = getAppString(R.string.fekyc_take_picture_warning_take_papers_back)
                WARNING_TYPE.PAPERS
            }
            PHOTO_INFORMATION.FACE -> {
                ovFrameCrop.apply {
                    setCropType(OverlayView.CROP_TYPE.CIRCLE)
                }
                tvWarningText.text = getAppString(R.string.fekyc_take_picture_warning_take_face)
                WARNING_TYPE.PORTRAIT
            }
        }
    }

    private fun getToolbarTitleByEkycType(): String {
        return when (viewModel.retakePhotoType ?: EkycStep.getCurrentStep()) {
            PHOTO_INFORMATION.FRONT -> getAppString(R.string.fekyc_take_picture_take_front)
            PHOTO_INFORMATION.BACK -> getAppString(R.string.fekyc_take_picture_take_back)
            PHOTO_INFORMATION.FACE -> getAppString(R.string.fekyc_take_picture_image_portrait)
            PHOTO_INFORMATION.PAGE_NUMBER_2 -> getAppString(R.string.fekyc_take_picture_take_passport)
        }
    }

    private fun showTimeOutLiveness() {
        val dialog = ConfirmDialog.Builder()
            .setTitle(getAppString(R.string.fekyc_notification))
            .setContent(getAppString(R.string.remaining_time_liveness_content))
            .setRightTitle(getAppString(R.string.fekyc_confirm))
            .build()
        dialog.listener = object : ConfirmDialog.IListener {
            override fun onRightClick() {
                position = -1
                ivProgressRight.resetColor()
                ivProgressTop.resetColor()
                ivProgressLeft.resetColor()
                ivProgressDown.resetColor()
                ivCapture.hide()
                viewModel.clearFrame()
                isBlockChecking = false
                setupTimer()
                dialog.dismiss()
            }
        }
        dialog.showDialog(supportFragmentManager, dialog::class.java.simpleName)
    }

    private fun showLivenessError(msg: String?) {
        showError(msg) {
            resetProgressLiveness()
            position--
            isBlockChecking = false
            viewModel.clearFrame()
        }
    }

    private fun resetProgressLiveness() {
        when (position) {
            1 -> ivProgressLeft.resetColor()
            2 -> ivProgressDown.resetColor()
            3 -> ivProgressRight.resetColor()
            4 -> ivProgressTop.resetColor()
        }
    }

    private fun setupTimer() {
        countDownTimer = object : CountDownTimer(REMAINING_TIME, 1000) {
            override fun onTick(time: Long) {
                tvLivenessRemainingTime.text = String.format(getAppString(R.string.remaining_time_liveness), time / 1000)
            }

            override fun onFinish() {
                showTimeOutLiveness()
            }
        }.start()
    }
}
