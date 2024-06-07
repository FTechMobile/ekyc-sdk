package ai.ftech.fekyc.presentation.info

import ai.ftech.fekyc.R
import ai.ftech.fekyc.base.common.BaseViewModel
import ai.ftech.fekyc.base.extension.postSelf
import ai.ftech.fekyc.common.action.FEkycActionResult
import ai.ftech.fekyc.common.getAppString
import ai.ftech.fekyc.data.repo.converter.FaceMatchingDataConvertToSubmitRequest
import ai.ftech.fekyc.data.source.remote.model.ekyc.submit.NewSubmitInfoRequest
import ai.ftech.fekyc.domain.APIException
import ai.ftech.fekyc.domain.model.address.City
import ai.ftech.fekyc.domain.model.address.Nation
import ai.ftech.fekyc.domain.model.ekyc.EkycFormInfo
import ai.ftech.fekyc.domain.model.ekyc.EkycInfo
import ai.ftech.fekyc.domain.model.facematching.FaceMatchingData
import ai.ftech.fekyc.publish.FTechEkycManager
import ai.ftech.fekyc.publish.IFTechEkycCallback
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import org.json.JSONObject

class EkycInfoViewModel : BaseViewModel() {
    var submitInfo = MutableLiveData(FEkycActionResult<Boolean>())
        private set

    var cityList: List<City> = emptyList()
        private set

    var nationList: List<Nation> = emptyList()
        private set

    var currentSessionId : String? = ""

    var ekycInfo: EkycInfo? = EkycInfo()

    fun submitInfo(list: List<EkycFormInfo>) {
        if (currentSessionId.isNullOrEmpty()) return
        val newSubmitData: NewSubmitInfoRequest = getNewSubmitInfo(list)
        FTechEkycManager.submitInfo(newSubmitData, object : IFTechEkycCallback<Boolean> {
            override fun onSuccess(info: Boolean) {
                super.onSuccess(info)
                submitInfo.value?.data = info
                submitInfo.postSelf()
            }

            override fun onFail(error: APIException?) {
                super.onFail(error)
                submitInfo.value?.exception = error
                submitInfo.postSelf()
            }
        })
    }

    private fun getNewSubmitInfo(list: List<EkycFormInfo>): NewSubmitInfoRequest {
        val infoJsonObject = JSONObject()
        list.forEach { info ->
            infoJsonObject.put(info.fieldName.orEmpty(), info.fieldValue)
        }
        val cardInfoData = Gson().fromJson(infoJsonObject.toString(), FaceMatchingData.CardInfo::class.java)
        val faceMatchingData = FaceMatchingData().apply {
            this.sessionId = currentSessionId
            this.cardInfo = cardInfoData
        }
        return FaceMatchingDataConvertToSubmitRequest().convert(faceMatchingData)
    }
}
