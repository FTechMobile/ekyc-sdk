package ai.ftech.fekyc.presentation.picture.confirm


import ai.ftech.fekyc.base.extension.observer
import ai.ftech.fekyc.base.extension.setOnSafeClick
import ai.ftech.fekyc.R
import ai.ftech.fekyc.base.adapter.group.GroupAdapter
import ai.ftech.fekyc.common.FEkycActivity
import ai.ftech.fekyc.common.action.FEkycActionResult
import ai.ftech.fekyc.common.getAppString
import ai.ftech.fekyc.common.widget.toolbar.ToolbarView
import ai.ftech.fekyc.data.source.remote.event.MessageEvent
import ai.ftech.fekyc.domain.APIException
import ai.ftech.fekyc.domain.event.EkycEvent
import ai.ftech.fekyc.domain.model.ekyc.PhotoConfirmDetailInfo
import ai.ftech.fekyc.domain.model.ekyc.PhotoInfo
import ai.ftech.fekyc.presentation.dialog.ConfirmDialog
import ai.ftech.fekyc.presentation.info.EkycInfoActivity
import ai.ftech.fekyc.publish.FTechEkycManager
import ai.ftech.fekyc.utils.ShareFlowEventBus
import android.annotation.SuppressLint
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ConfirmPictureActivity : FEkycActivity(R.layout.fekyc_confirm_picture_activity) {
    private lateinit var tbvHeader: ToolbarView
    private lateinit var rvPhoto: RecyclerView
    private lateinit var btnContinue: Button
    private lateinit var tvConfirmPictureTransId: AppCompatTextView
    private val viewModel by viewModels<ConfirmPictureViewModel>()
    private val adapter = GroupAdapter()

    @SuppressLint("SetTextI18n")
    override fun onInitView() {
        super.onInitView()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tbvHeader = findViewById(R.id.tbvConfirmPictureHeader)
        rvPhoto = findViewById(R.id.rvConfirmPicturePhotoList)
        btnContinue = findViewById(R.id.btnConfirmPictureContinues)
        tvConfirmPictureTransId = findViewById(R.id.tvConfirmPictureTransId)

        tvConfirmPictureTransId.text = "Trans ID: ${FTechEkycManager.getTransactionID()}"

        rvPhoto.layoutManager = LinearLayoutManager(this)
        rvPhoto.adapter = adapter
        viewModel.getConfirmPhotoList()

        tbvHeader.setListener(object : ToolbarView.IListener {
            override fun onLeftIconClick() {
                onBackPressed()
            }
        })

        btnContinue.setOnSafeClick {
//            showLoading()
            viewModel.getFaceMatchingData()
        }
    }

    override fun onObserverViewModel() {
        super.onObserverViewModel()
        observer(viewModel.photoConfirmDetailInfoList) {
            createConfirmPictureGroup(it)
        }
        observer(viewModel.ekycInfo) {
            when (it?.resultStatus) {
                FEkycActionResult.RESULT_STATUS.SUCCESS -> {
                    hideLoading()
                    finish()
                    navigateTo(EkycInfoActivity::class.java, bundle = bundleOf(EkycInfoActivity.EKYC_INFO to it.data, EkycInfoActivity.SESSION_ID to viewModel.currentSessionId))
                }
                FEkycActionResult.RESULT_STATUS.ERROR -> {
                    hideLoading()
                    val dialog = ConfirmDialog.Builder()
                        .setTitle(getAppString(R.string.fekyc_notification))
                        .setContent(it.exception?.message
                            ?: getAppString(R.string.fekyc_unknown_error))
                        .setRightTitle(getAppString(R.string.fekyc_retry))
                        .build()
                    dialog.listener = object : ConfirmDialog.IListener {
                        override fun onRightClick() {
                            dialog.dismiss()
                        }
                    }
                    dialog.showDialog(supportFragmentManager, dialog::class.java.simpleName)
                }
                else -> {

                }
            }
        }
    }

    override fun getContainerId() = R.id.flconfirmPictureFrame

    private fun createConfirmPictureGroup(list: MutableList<PhotoConfirmDetailInfo>?) {
        adapter.clear()
        list?.forEach { photoConfirmDetailInfo ->
            val groupData = ConfirmPictureGroup(photoConfirmDetailInfo).apply {
                this.listener = object : ConfirmPictureGroup.IListener {
                    override fun onClickItem(item: PhotoInfo) {
                        viewModel.setSelectedIndex(item)
                        replaceFragment(ConfirmPictureFragment())
                    }
                }
            }
            adapter.addGroupData(groupData)
        }
        adapter.notifyAllGroupChanged()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        Log.e("MessageEvent", "MessageEvent")
        adapter.clear()
        viewModel.getConfirmPhotoList()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onResume() {
        super.onResume()
        FTechEkycManager.notifyActive(this)
    }

    override fun onPause() {
        super.onPause()
        FTechEkycManager.notifyInactive(this)
    }
}
