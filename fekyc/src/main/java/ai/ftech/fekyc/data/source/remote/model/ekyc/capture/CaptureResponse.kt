package ai.ftech.fekyc.data.source.remote.model.ekyc.capture

import ai.ftech.fekyc.data.source.remote.base.BaseApiResponse
import ai.ftech.fekyc.data.source.remote.base.IApiResponse
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


class CaptureResponse : BaseApiResponse() {
    @SerializedName("data")
    @Expose
    var data: DataCapture? = null

    class DataCapture {
        @SerializedName("session_id")
        @Expose
        var sessionId: String? = null
    }

}


