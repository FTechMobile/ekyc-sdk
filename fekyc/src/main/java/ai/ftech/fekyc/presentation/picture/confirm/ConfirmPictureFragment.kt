package ai.ftech.fekyc.presentation.picture.confirm

import ai.ftech.fekyc.R
import ai.ftech.fekyc.base.extension.observer
import ai.ftech.fekyc.base.extension.setOnSafeClick
import ai.ftech.fekyc.common.FEkycFragment
import ai.ftech.fekyc.common.getAppString
import ai.ftech.fekyc.common.widget.toolbar.ToolbarView
import ai.ftech.fekyc.data.source.remote.event.MessageEvent
import ai.ftech.fekyc.domain.model.ekyc.PHOTO_INFORMATION
import ai.ftech.fekyc.presentation.picture.take.TakePictureActivity
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.viewpager.widget.ViewPager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class ConfirmPictureFragment : FEkycFragment(R.layout.fekyc_confirm_picture_fragment) {
    private lateinit var tbvHeader: ToolbarView
    private lateinit var vpPhoto: ViewPager
    private lateinit var ivBack: ImageView
    private lateinit var ivNext: ImageView
    private lateinit var btnConfirmPictureTakeAgain: Button

    override fun isHandleBackPressByFragment(): Boolean = true

    private val activityViewModel by activityViewModels<ConfirmPictureViewModel>()
    private val adapter = PreviewPhotoAdapter()

    override fun onDestroy() {
        super.onDestroy()
        activityViewModel.clearSelected()
    }

    override fun onInitView() {
        super.onInitView()
        tbvHeader = viewRoot.findViewById(R.id.tbvConfirmPictureHeader)
        vpPhoto = viewRoot.findViewById(R.id.vpConfirmPicture)
        ivBack = viewRoot.findViewById(R.id.ivConfirmPictureBack)
        ivNext = viewRoot.findViewById(R.id.ivConfirmPictureNext)
        btnConfirmPictureTakeAgain = viewRoot.findViewById(R.id.btnConfirmPictureTakeAgain)

        tbvHeader.setListener(object : ToolbarView.IListener {
            override fun onLeftIconClick() {
                backFragment()
            }
        })

        btnConfirmPictureTakeAgain.setOnSafeClick {
            navigateTo(TakePictureActivity::class.java, bundle = bundleOf(TakePictureActivity.RETAKE_PHOTO_TYPE to activityViewModel.selectedPosition))
        }

        vpPhoto.apply {
            offscreenPageLimit = 100
            post {
                currentItem = activityViewModel.getSelectedIndex()
            }
            adapter = this@ConfirmPictureFragment.adapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {

                }

                override fun onPageSelected(position: Int) {
                    activityViewModel.setSelectedIndex(position % 3)
                    setTitle()
                }

                override fun onPageScrollStateChanged(state: Int) {

                }

            })
        }


        ivBack.setOnSafeClick {
            var index = vpPhoto.currentItem
            if (index >= 0) {
                vpPhoto.currentItem = --index
            }
        }

        ivNext.setOnSafeClick {
            var index = vpPhoto.currentItem
            if (index <= adapter.count) {
                vpPhoto.currentItem = ++index
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        backFragment()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onObserverViewModel() {
        super.onObserverViewModel()
        observer(activityViewModel.photoInfoList) {
            adapter.dataList = it
        }
    }

    private fun setTitle() {
        tbvHeader.setTitle(
            when (activityViewModel.getItemSelected()?.photoInformation) {
                PHOTO_INFORMATION.FRONT -> getAppString(R.string.fekyc_take_picture_image_front)
                PHOTO_INFORMATION.BACK -> getAppString(R.string.fekyc_take_picture_image_back)
                PHOTO_INFORMATION.FACE -> getAppString(R.string.fekyc_take_picture_image_portrait)
                PHOTO_INFORMATION.PAGE_NUMBER_2 -> getAppString(R.string.fekyc_take_picture_image_passport)
                else -> ""
            }
        )
    }

    override fun onBackPressedFragment(tag: String?) {
        backFragment()
    }
}
