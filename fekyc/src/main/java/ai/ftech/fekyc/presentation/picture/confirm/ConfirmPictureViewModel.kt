package ai.ftech.fekyc.presentation.picture.confirm

import ai.ftech.fekyc.R
import ai.ftech.fekyc.base.common.BaseAction
import ai.ftech.fekyc.base.common.BaseViewModel
import ai.ftech.fekyc.base.extension.postSelf
import ai.ftech.fekyc.common.action.FEkycActionResult
import ai.ftech.fekyc.common.getAppString
import ai.ftech.fekyc.common.onException
import ai.ftech.fekyc.domain.APIException
import ai.ftech.fekyc.domain.action.GetConfirmPhotoListAction
import ai.ftech.fekyc.domain.model.ekyc.EkycFormInfo
import ai.ftech.fekyc.domain.model.ekyc.EkycInfo
import ai.ftech.fekyc.domain.model.ekyc.PhotoConfirmDetailInfo
import ai.ftech.fekyc.domain.model.ekyc.PhotoInfo
import ai.ftech.fekyc.domain.model.facematching.FaceMatchingData
import ai.ftech.fekyc.publish.FTechEkycManager
import ai.ftech.fekyc.publish.IFTechEkycCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.json.JSONObject

class ConfirmPictureViewModel : BaseViewModel() {
    var photoConfirmDetailInfoList = MutableLiveData(mutableListOf<PhotoConfirmDetailInfo>())
        private set

    var photoInfoList = MutableLiveData<List<PhotoInfo>>()
        private set

    var ekycInfo = MutableLiveData(FEkycActionResult<EkycInfo>())
        private set

    var selectedPosition = -1

    fun getConfirmPhotoList() {
        viewModelScope.launch {
            GetConfirmPhotoListAction().invoke(BaseAction.VoidRequest()).onException {

            }.collect {
                photoConfirmDetailInfoList.value = it.toMutableList()
                photoInfoList.value = getPhotoInfoList(it)
            }
        }
    }

    var currentSessionId: String? = ""

    fun setSelectedIndex(item: PhotoInfo) {
        selectedPosition = photoInfoList.value?.indexOf(item) ?: -1
    }

    fun setSelectedIndex(pos: Int) {
        selectedPosition = pos
    }

    fun getSelectedIndex(): Int {
        return selectedPosition
    }

    fun getItemSelected(): PhotoInfo? {
        return photoInfoList.value?.get(getSelectedIndex())
    }

    fun clearSelected() {
        selectedPosition = -1
    }

    private fun getPhotoInfoList(list: List<PhotoConfirmDetailInfo>): List<PhotoInfo> {
        val result = mutableListOf<PhotoInfo>()
        list.forEach {
            result.addAll(it.photoList)
        }
        return result
    }

    fun getFaceMatchingData() {
        FTechEkycManager.faceMatching(object : IFTechEkycCallback<FaceMatchingData> {
            override fun onSuccess(info: FaceMatchingData) {
                super.onSuccess(info)
                currentSessionId = info.sessionId.orEmpty()
                ekycInfo.value?.data = convertMatchingDataToEkycInfo(info.cardInfo)
                ekycInfo.postSelf()
            }

            override fun onFail(error: APIException?) {
                super.onFail(error)
                ekycInfo.value?.exception = error
                ekycInfo.postSelf()
            }
        })
    }

    private fun convertMatchingDataToEkycInfo(info: FaceMatchingData.CardInfo?): EkycInfo {
        if (info == null)
            return EkycInfo()

        val newForm = getFormInfo(info)
        val listInfo = arrayListOf<EkycFormInfo>()
        listInfo.add(newForm.find { it.fieldName.equals("id") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("birthDay") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("birthPlace") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("cardType") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("gender") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("issueDate") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("issuePlace") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("name") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("nationality") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("originLocation") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("passportNo") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("recentLocation") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("validDate") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("feature") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("nation") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("religion") } ?: EkycFormInfo())
        listInfo.add(newForm.find { it.fieldName.equals("mrz") } ?: EkycFormInfo())


        return EkycInfo().apply {
            this.identityType = getAppString(R.string.mock_title_card_type)
            this.identityName = info.cardType
            this.form = listInfo
        }
    }

    private fun getFormInfo(info: FaceMatchingData.CardInfo): List<EkycFormInfo> {
        val listInfo = arrayListOf<EkycFormInfo>()

        val infoJson = Gson().toJson(info)
        val infoJsonObject = JSONObject(infoJson)
        val fieldInfo = infoJsonObject.keys()
        fieldInfo.forEach {
            val value = infoJsonObject[it]
            listInfo.add(EkycFormInfo().apply {
                this.fieldName = it
                this.fieldValue = value.toString()
                this.fieldType = EkycFormInfo.FIELD_TYPE.STRING
                this.isEditable = true
            })
        }
        return listInfo
    }
}
