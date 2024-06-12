package ai.ftech.fekyc.presentation.picture.preview

import ai.ftech.fekyc.base.common.BaseViewModel
import ai.ftech.fekyc.domain.model.ekyc.PHOTO_INFORMATION

class PreviewPictureViewModel : BaseViewModel() {
    var imagePreviewPath: String? = null
    var message: String? = null
    var retakePhotoType: PHOTO_INFORMATION? = null
}
